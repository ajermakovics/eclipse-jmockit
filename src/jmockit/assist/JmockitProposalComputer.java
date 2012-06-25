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

import static jmockit.assist.MockUtil.findMockedTypeFromNode;
import static org.eclipse.jface.viewers.StyledString.QUALIFIER_STYLER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings("restriction")
/**
 * Computes proposals for mock methods in mock objects
 */
public class JmockitProposalComputer implements IJavaCompletionProposalComputer, IQuickAssistProcessor
{
	private String fErrorMessage;

	public JmockitProposalComputer()
	{//
	}

	@Override
	public final List<ICompletionProposal> computeCompletionProposals(final ContentAssistInvocationContext context,
			final IProgressMonitor mon)
	{
		ITypeBinding mockType = null, paramType = null;
		ICompilationUnit cunit = getCompilationUnit(context);
		CompilationUnit astRoot = null;

		if (cunit != null)
		{
			astRoot = ASTUtil.getAstOrParse(cunit, mon);

			if( astRoot != null )
			{
				ASTNode node = NodeFinder.perform(astRoot, context.getInvocationOffset(), 1);

				mockType = getMockType(node);
				paramType = findMockedTypeFromNode(node);
			}
		}

		if ( paramType != null && mockType != null )
		{
			try
			{
				return getProposals(context, paramType, cunit, mockType, astRoot.getAST());
			}
			catch (Exception e)
			{
				fErrorMessage = e.getMessage();
				Activator.log(e);
			}
		}

		return Collections.emptyList();
	}

	private static ITypeBinding getMockType(final ASTNode node)
	{
		ITypeBinding mockType = null;

		if (node instanceof AnonymousClassDeclaration)
		{
			mockType = ((AnonymousClassDeclaration) node).resolveBinding();
		}
		else if( node instanceof TypeDeclaration )
		{
			mockType = ((TypeDeclaration) node).resolveBinding();
		}
		return mockType;
	}

	public final ICompilationUnit getCompilationUnit(final ContentAssistInvocationContext context)
	{
		ICompilationUnit cunit = null;
		if (context instanceof JavaContentAssistInvocationContext)
		{
			JavaContentAssistInvocationContext jcontext = (JavaContentAssistInvocationContext) context;

			if (jcontext.getCoreContext() != null)
			{
				cunit = jcontext.getCompilationUnit();
			}
		}
		return cunit;
	}

	private List<ICompletionProposal> getProposals(final ContentAssistInvocationContext context,
			final ITypeBinding paramType,
			final ICompilationUnit cunit, final ITypeBinding mockType, final AST ast)
					throws JavaModelException, BadLocationException
	{
		Collection<IJavaCompletionProposal> list = new ArrayList<IJavaCompletionProposal>();
		String prefix = context.computeIdentifierPrefix().toString();
		Set<String> existingMethods = ASTUtil.getMethodSignatures(mockType);

		addItFieldProposal(context, paramType, mockType, list, prefix);

		for (final IMethodBinding meth : ASTUtil.getAllMethods(paramType, ast) )
		{
			String methodName = meth.getName();

			if( !methodName.startsWith(prefix) || "<clinit>".equals(methodName) )
			{
				continue;
			}

			String methSig = ASTUtil.getSig(meth);
			methodName = meth.isConstructor()  ? MockUtil.CTOR : meth.getName();
			if( existingMethods.contains(methSig) )
			{
				continue;
			}

			try
			{
				MockMethodCompletionProposal proposal = createMockMethodProposal(context, paramType, cunit, prefix,
						meth, methodName);

				list.add(proposal);
			}
			catch (Exception e)
			{
				Activator.log(e);
			}

		}

		return new ArrayList<ICompletionProposal>(list);
					}

	private void addItFieldProposal(final ContentAssistInvocationContext context, final ITypeBinding paramType,
			final ITypeBinding mockType, final Collection<IJavaCompletionProposal> list, final String prefix)
					throws JavaModelException
					{
		boolean hasItField = false;
		for(IVariableBinding field : mockType.getDeclaredFields())
		{
			if( "it".equals( field.getName() ) )
			{
				hasItField = true;
				break;
			}
		}

		if( !hasItField && "it".startsWith(prefix) )
		{
			String relpacement = paramType.getName() + " it;";
			Image image = JavaPluginImages.get(JavaPluginImages.IMG_FIELD_DEFAULT);

			StyledString displayName = new StyledString("it : " + paramType.getName());
			displayName.append(" - Access the mocked object 'it' of type '"
					+ paramType.getName() +"'", QUALIFIER_STYLER);

			IJavaCompletionProposal proposal
			= new JavaCompletionProposal(relpacement, context.getInvocationOffset()-prefix.length(), prefix.length(),
					image, displayName, MockMethodCompletionProposal.MAX_RELEVANCE);
			list.add(proposal );
		}
	}

	private MockMethodCompletionProposal createMockMethodProposal(final ContentAssistInvocationContext context,
			final ITypeBinding paramType, final ICompilationUnit cunit,
			final String prefix, final IMethodBinding meth, final String methodName) throws JavaModelException
	{
		String params = getParameters(meth);

		StyledString displayName = new StyledString(methodName
				+ "(" + params + ") : " + meth.getReturnType().getName() );

		String desc = meth.isConstructor() ? "constructor" : "method";

		displayName.append(" - Mock " + desc + " of '"
				+ paramType.getName() +"'", QUALIFIER_STYLER);

		String completionProposal = meth.getReturnType().getName() + " " + methodName;

		MockMethodCompletionProposal proposal = new MockMethodCompletionProposal(cunit, meth,
				context.getInvocationOffset()-prefix.length(), prefix.length(), displayName,
				completionProposal);

		return proposal;
	}

	private String getParameters(final IMethodBinding methBinding) throws JavaModelException
	{
		IMethod meth = (IMethod) methBinding.getJavaElement();
		String[] paramNames = meth.getParameterNames();

		String params= "";
		ITypeBinding[] paramTypes = methBinding.getParameterTypes();

		for(int i = 0; i < paramTypes.length; i++)
		{
			String par = paramTypes[i].getName();
			if( i != 0 )
			{
				params += ", ";
			}

			try
			{
				params += par; //Signature.getSimpleName( Signature.toString(par) );
			}
			catch(Exception e)
			{
				Activator.log(e);
				params += par;
			}

			params += " " + paramNames[i];
		}

		return params;
	}

//	public static Set<String> getObjectMethods(final IJavaProject jproj) throws JavaModelException
//	{
//		IType objType = jproj.findType("java.lang.Object");
//
//		return getTypeMethods(objType);
//	}

	@Override
	public final List<IContextInformation> computeContextInformation(final ContentAssistInvocationContext ctx,
			final IProgressMonitor mon)
	{
		return Collections.emptyList();
	}

	@Override
	public final String getErrorMessage()
	{
		return fErrorMessage;
	}

	@Override
	public final void sessionEnded()
	{
	}

	@Override
	public final void sessionStarted()
	{
	}

	@Override
	public final boolean hasAssists(final IInvocationContext context) throws CoreException
	{
		//System.err.println("has assist? ");
		return false;
	}

	@Override
	public final IJavaCompletionProposal[] getAssists(final IInvocationContext context,
			final IProblemLocation[] locations)
					throws CoreException
	{
		//System.err.println("get assist");
		return new IJavaCompletionProposal[]{};
	}
}
