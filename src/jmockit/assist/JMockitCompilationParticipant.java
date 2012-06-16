/*
 * Copyright (c) 2012 Andrejs Jermakovics.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrejs Jermakovics - initial implementation
 */
package jmockit.assist;

import static jmockit.assist.ASTUtil.isAnnotationPresent;
import static jmockit.assist.ASTUtil.isMockUpType;
import static org.eclipse.jdt.core.dom.Modifier.isPrivate;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.corext.dom.Bindings;

@SuppressWarnings("restriction")
/**
 * Compilation participant to check mock classes and methods for problems
 */
public final class JMockitCompilationParticipant extends CompilationParticipant
{
	public JMockitCompilationParticipant()
	{
	}

	@Override
	public void buildStarting(final BuildContext[] files, final boolean isBatch)
	{
		for (BuildContext f : files)
		{
			IFile file = f.getFile();

			ICompilationUnit cunit = JavaCore.createCompilationUnitFrom(file);
			if( cunit != null )
			{
				CompilationUnit cu = ASTUtil.getAstOrParse(cunit, null);

				MockASTVisitor visitor = new MockASTVisitor(cunit);
				cu.accept(visitor);
				f.recordNewProblems(visitor.getProblems());
			}
		}
	}

	@Override
	public boolean isActive(final IJavaProject project)
	{
		IType mockitType = null;
		try
		{
			mockitType = project.findType("mockit.Mockit");
		}
		catch (JavaModelException e)
		{
			Activator.log(e);
		}

		return mockitType != null;
	}

	@Override
	public void reconcile(final ReconcileContext context)
	{
		try
		{
			MockASTVisitor visitor = new MockASTVisitor( context.getWorkingCopy() );
			context.getAST3().accept(visitor );

			CategorizedProblem[] probs = visitor.getProblems();

			if( probs.length != 0 )
			{
				context.putProblems(probs[0].getMarkerType(), probs);
			}
		}
		catch (JavaModelException e)
		{
			Activator.log(e);
		}
	}

	private static class MockASTVisitor extends ASTVisitor
	{
		private ICompilationUnit icunit;
		private CompilationUnit cu;

		private List<CategorizedProblem> probs = new ArrayList<CategorizedProblem>();

		public MockASTVisitor(final ICompilationUnit cunitPar)
		{
			this.icunit = cunitPar;
		}

		@Override
		public boolean visit(final CompilationUnit node)
		{
			this.cu = node;
			return true;
		}

		@Override
		public boolean visit(final AnonymousClassDeclaration node)
		{
			ITypeBinding binding = node.resolveBinding();

			if( ASTUtil.isMockUpType(binding.getSuperclass()) ) // new MockUp< type >
			{
				ITypeBinding typePar = ASTUtil.getFirstTypeParameter(node);
				ASTNode parent = node.getParent();

				if( parent instanceof ClassInstanceCreation && typePar.isInterface() ) // creating interface mock
				{
					ASTNode gparent = parent.getParent();
					ClassInstanceCreation creation = (ClassInstanceCreation) parent;

					if( gparent instanceof MethodInvocation ) // method invocation follows
					{
						MethodInvocation inv = (MethodInvocation) gparent;
						IMethodBinding meth = inv.resolveMethodBinding();

						if( "getMockInstance".equals( meth.getName() ) )
						{
							if( gparent.getParent() instanceof ExpressionStatement )
							{
								addMarker(inv.getName(),
										"Returned mock instance is not being used.", false);
							}
						}
						else
						{
							addMarker(creation.getType(),
									"Missing call to getMockInstance() on MockUp of interface", false);
						}
					}

				}
			}

			return true;
		}

		@Override
		public boolean visit(final MethodInvocation node)
		{
			IMethodBinding meth = node.resolveMethodBinding();

			if( meth != null
					&& ASTUtil.isMockUpType(meth.getDeclaringClass()) && "getMockInstance".equals(meth.getName()) )
			{
				ITypeBinding returnType = node.resolveTypeBinding();

				if( !returnType.isInterface() )
				{
					String msg = "getMockInstance() used on mock of class " + returnType.getName()
							+ ". Use on interfaces";
					addMarker(node.getName(), msg , false);
				}
			}

			return true;
		}

		@Override
		public boolean visit(final MethodDeclaration node)
		{
			IMethodBinding meth = node.resolveBinding();
			ITypeBinding typePar, declaringClass = meth.getDeclaringClass();

			boolean isMockClass = ASTUtil.hasMockClass(declaringClass);
			boolean isMockUpType = isMockUpType(declaringClass.getSuperclass());

			if( isMockUpType || isMockClass )
			{
				if( isMockUpType )
				{
					typePar = ASTUtil.getFirstTypeParameter(node.getParent());
				}
				else
				{
					typePar = ASTUtil.findRealClassType(declaringClass);
				}

				boolean hasMockAnn = isAnnotationPresent(meth.getAnnotations(), "mockit.Mock");
				IMethodBinding origMethod = null;

				if( typePar != null )
				{
					String name = meth.getName();
					
					if( "$init".equals(name) ) // constructor
					{
						name = typePar.getName(); 
					}
					
					if( typePar.isInterface() )
					{
						origMethod = Bindings.findMethodInHierarchy(typePar, name, meth.getParameterTypes());
					}
					else
					{
						origMethod = Bindings.findMethodInType(typePar, name, meth.getParameterTypes());
					}
				}

				if( !hasMockAnn && origMethod != null )
				{
					addMarker(node.getName(), "Mocked method missing @Mock annotation", false);
				}

				if( hasMockAnn && origMethod == null )
				{
					addMarker(node.getName(), "Mocked real method not found in type " , true);
				}

				if( hasMockAnn && origMethod != null && isPrivate(meth.getModifiers()) )
				{
					addMarker(node.getName(), "Mocked method should not be private", true);
				}
			}

			return true;
		}

		private void addMarker(final ASTNode node, final String msg, final boolean isError)
		{
			try
			{
				int start = node.getStartPosition();
				int endChar = start + node.getLength();
				int line = cu.getLineNumber(start);
				int col = cu.getColumnNumber(start);
				int id = IProblem.MethodRelated;

				int severity = ProblemSeverities.Warning;
				if( isError )
				{
					severity = ProblemSeverities.Error;
				}

				CategorizedProblem problem
				= new DefaultProblem(icunit.getPath().toOSString().toCharArray(), msg, id,
						new String[]{}, severity, start, endChar, line, col);

				probs.add(problem);
			}
			catch (Exception e)
			{
				Activator.log(e);
			}
		}

		public CategorizedProblem[] getProblems()
		{
			return probs.toArray(new CategorizedProblem[]{});
		}

	}


}
