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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.SharedASTProvider;

@SuppressWarnings("restriction")
public class ASTUtil
{
	public static ITypeBinding getFirstTypeParameter(final ASTNode node)
	{
		ITypeBinding declaringType = null;

		if( node.getParent() instanceof ClassInstanceCreation ) // for anonymous
		{
			ClassInstanceCreation creation = (ClassInstanceCreation) node.getParent();
			Type ctype = creation.getType();

			declaringType = getFirstTypeParam(ctype);
		}
		else if( node instanceof TypeDeclaration )
		{
			Type ctype = ((TypeDeclaration) node).getSuperclassType();
			declaringType = getFirstTypeParam(ctype);
		}

		return declaringType;
	}

	@SuppressWarnings("unchecked")
	public static ITypeBinding getFirstTypeParam(final Type ctype)
	{
		ITypeBinding declaringType = null;

		if( ctype instanceof ParameterizedType )
		{
			ParameterizedType paramType = (ParameterizedType) ctype;
			List<Type> typeArgs = paramType.typeArguments();

			if( !typeArgs.isEmpty() )
			{
				Type arg1 = typeArgs.get(0);
				declaringType = arg1.resolveBinding();
			}
		}
		return declaringType;
	}

	public static void addAnnotation(final String annotation,
			final IJavaProject project, final ASTRewrite rewrite, final MethodDeclaration decl,
			final IMethodBinding binding)
	{
		String version= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);

		if (!binding.getDeclaringClass().isInterface()
				|| !JavaModelUtil.isVersionLessThan(version, JavaCore.VERSION_1_6))
		{
			final Annotation marker= rewrite.getAST().newMarkerAnnotation();
			marker.setTypeName(rewrite.getAST().newSimpleName(annotation)); //$NON-NLS-1$
			rewrite.getListRewrite(decl, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);
		}
	}

	public static boolean isAnnotationPresent(final IAnnotationBinding[] annotations, final String annName)
	{
		return findAnnotation(annotations, annName) != null;
	}

	public static IAnnotationBinding findAnnotation(final IAnnotationBinding[] annotations, final String annName)
	{
		for(IAnnotationBinding ann: annotations)
		{
			if( annName.equals( ann.getAnnotationType().getQualifiedName()) )
			{
				return ann;
			}
		}

		return null;
	}

	public static CompilationUnit getAstOrParse(final ITypeRoot iTypeRoot, final IProgressMonitor mon)
	{
		CompilationUnit cu = SharedASTProvider.getAST(iTypeRoot, SharedASTProvider.WAIT_NO, mon);

		if( cu == null )
		{
			cu = parse(iTypeRoot, mon);
		}

		return cu;
	}

	public static CompilationUnit parse(final ITypeRoot unit, final IProgressMonitor mon)
	{
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setProject(unit.getJavaProject());
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(true);
		return (CompilationUnit) parser.createAST(mon); // parse
	}

	@SuppressWarnings("unchecked")
	public static <T extends ASTNode> T findAncestor(final ASTNode node, final Class<T> clazz)
	{
		ASTNode parent = node.getParent();
		while( parent != null )
		{
			if( parent.getClass() == clazz )
			{
				break;
			}
			parent = parent.getParent();
		}
		return (T) parent;
	}

	public static IMethodBinding findMethodInType(final ITypeBinding type, final String name,
			final ITypeBinding[] paramTypes)
	{
		IMethodBinding origMethod;

		if (type.isInterface())
		{
			origMethod = Bindings.findMethodInHierarchy(type, name, paramTypes);
		}
		else
		{
			origMethod = Bindings.findMethodInType(type, name, paramTypes);
		}

		return origMethod;
	}

	public static Set<String> getMethodSignatures(final ITypeBinding objType) throws JavaModelException
	{
		Set<String> methods = new TreeSet<String>();

		for(IMethodBinding m: objType.getDeclaredMethods() )
		{
			methods.add( getSig(m) );
		}

		return methods;
	}

	public static String getSig(final IMethodBinding m) throws JavaModelException
	{
		String sig = m.getKey().split(";", 2)[1]; // remove declaring type
		return m.getName() +  sig;
	}

	public static Collection<IMethodBinding> getAllMethods(final ITypeBinding paramType)
			throws JavaModelException
	{
		List<IMethodBinding> methods = new ArrayList<IMethodBinding>();
		methods.addAll(Arrays.asList( paramType.getDeclaredMethods() ) );

		if( paramType.isInterface() )
		{
			for(ITypeBinding superType: Bindings.getAllSuperTypes(paramType))
			{
				if( !Object.class.getName().equals(superType.getQualifiedName()) )
				{
					methods.addAll( Arrays.asList(superType.getDeclaredMethods()) );
				}
			}
		}

		Collections.sort(methods, new Comparator<IMethodBinding>()
		{
			@Override
			public int compare(final IMethodBinding m1, final IMethodBinding m2)
			{
				return m1.getName().compareTo( m2.getName() );
			}
		});

		return methods;
	}
}
