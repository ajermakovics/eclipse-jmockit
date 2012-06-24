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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MockUtil
{

	public static final String MOCK = "mockit.Mock";
	public static final String MOCKIT = "mockit.Mockit";
	public static final String MOCK_UP = "mockit.MockUp";
	public static final String MOCK_CLASS = "mockit.MockClass";

	public static final String CTOR = "$init";
	public static final String GET_INST = "getMockInstance";

	public static boolean isMockMethod(final IMethodBinding meth)
	{
		return meth != null && ASTUtil.isAnnotationPresent(meth.getAnnotations(), MOCK);
	}

	public static boolean isReentrantMockMethod(final IMethodBinding meth)
	{
		IAnnotationBinding ann = ASTUtil.findAnnotation(meth.getAnnotations(), MOCK);

		if (ann != null)
		{
			for (IMemberValuePairBinding pair : ann.getDeclaredMemberValuePairs())
			{
				if ("reentrant".equals(pair.getName()))
				{
					return Boolean.valueOf(pair.getValue().toString());
				}
			}
		}

		return false;
	}

	public static boolean hasMockClass(final ITypeBinding type)
	{
		return ASTUtil.findAnnotation(type.getAnnotations(), MOCK_CLASS) != null;
	}

	public static boolean isMockUpType(final ITypeBinding declaringClass)
	{
		return declaringClass != null && declaringClass.getQualifiedName().startsWith(MOCK_UP);
	}

	public static ITypeBinding findMockedType(final MethodInvocation node)
	{
		MethodDeclaration surroundingMeth = ASTUtil.findAncestor(node, MethodDeclaration.class);

		if (surroundingMeth != null)
		{
			return findMockedType(surroundingMeth, surroundingMeth.resolveBinding());
		}
		else
		{
			return null;
		}
	}

	public static ITypeBinding findMockedType(final MethodDeclaration node, final IMethodBinding meth)
	{
		ITypeBinding typePar = null;
		if (meth == null)
		{
			return null;
		}

		ITypeBinding declaringClass = meth.getDeclaringClass();

		boolean isMockClass = hasMockClass(declaringClass);
		boolean isMockUpType = isMockUpType(declaringClass.getSuperclass());

		if (isMockUpType)
		{
			typePar = ASTUtil.getFirstTypeParameter(node.getParent());
		}
		else if (isMockClass)
		{
			typePar = MockUtil.findRealClassType(declaringClass);
		}
		return typePar;
	}

	public static IMethodBinding findRealMethodInType(final ITypeBinding type, final String name,
			final ITypeBinding[] paramTypes)
	{
		IMethodBinding origMethod = ASTUtil.findMethodInType(type, name, paramTypes);

		if (origMethod == null && type.getTypeArguments().length != 0)
		{
			// no method matches exactly, there could be type arguments (which we don't handle yet)
			origMethod = ASTUtil.findMethodInType(type, name, null); // match without params

			if (origMethod != null && origMethod.getParameterTypes().length != paramTypes.length)
			{
				origMethod = null;
			}
		}

		return origMethod;
	}

	public static ITypeBinding findRealClassType(final ITypeBinding mockClass)
	{
		IAnnotationBinding ann = ASTUtil.findAnnotation(mockClass.getAnnotations(), MOCK_CLASS);

		for (IMemberValuePairBinding pair : ann.getDeclaredMemberValuePairs())
		{
			if ("realClass".equals(pair.getName()))
			{
				if (pair.getValue() instanceof ITypeBinding)
				{
					return (ITypeBinding) pair.getValue();
				}
			}
		}

		return null;
	}

	public static ITypeBinding findMockedTypeFromNode(final ASTNode node)
	{
		ITypeBinding declaringType = null;

		if (node instanceof AnonymousClassDeclaration)
		{
			declaringType = ASTUtil.getFirstTypeParameter( (AnonymousClassDeclaration) node);
		}
		else if( node instanceof TypeDeclaration )
		{
			TypeDeclaration typeDec = (TypeDeclaration) node;
			ITypeBinding curType = typeDec.resolveBinding();

			if( hasMockClass(curType) )
			{
				declaringType = findRealClassType(curType);
			}
			else if( isMockUpType(curType.getSuperclass()) )
			{
				declaringType = ASTUtil.getFirstTypeParam(typeDec.getSuperclassType());
			}
		}

		return declaringType;
	}

}
