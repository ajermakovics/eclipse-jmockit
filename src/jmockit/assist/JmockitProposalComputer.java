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

import static org.eclipse.jface.viewers.StyledString.QUALIFIER_STYLER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
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
	private Set<String> objectMethods = new HashSet<String>();

	public JmockitProposalComputer()
	{//
	}

	@Override
	public final List<ICompletionProposal> computeCompletionProposals(final ContentAssistInvocationContext context,
			final IProgressMonitor mon)
	{
		IType mockType = null, paramType = null;
		ICompilationUnit cunit = null;
		Set<String> existingMethods = new HashSet<String>();

		if (context instanceof JavaContentAssistInvocationContext)
		{
			JavaContentAssistInvocationContext jcontext = (JavaContentAssistInvocationContext) context;
			CompletionContext corecontext = jcontext.getCoreContext();

			if (corecontext != null)
			{
				cunit = jcontext.getCompilationUnit();

				if (cunit != null)
				{
					try
					{
						if( objectMethods == null )
						{
							objectMethods = getObjectMethods(jcontext.getProject());
						}

						mockType = findMockType(cunit, jcontext );
						paramType = findMockedType(cunit, mockType);
					}
					catch (Exception e)
					{
						fErrorMessage = e.getMessage();
						Activator.log(e);
					}
				}
			}
		}

		if ( paramType != null && mockType != null )
		{
			try
			{
				existingMethods.addAll( getTypeMethods(mockType) );
				existingMethods.removeAll(objectMethods);

				return getProposals(context, paramType, cunit, existingMethods, mon, mockType);
			}
			catch (Exception e)
			{
				fErrorMessage = e.getMessage();
				Activator.log(e);
			}
		}

		return Collections.emptyList();
	}

	private IType findMockType(final ICompilationUnit cunit,
			final JavaContentAssistInvocationContext jcontext)
					throws JavaModelException
	{
		IType paramType = null;
		IJavaElement element = cunit.getElementAt(jcontext.getInvocationOffset());

		if (element != null && element.getElementType() == IJavaElement.TYPE)
		{
			IType mockType = (IType) element;

			paramType = mockType;
		}

		return paramType;
	}

	private IType findMockedType(final ICompilationUnit cunit,
			final IType mockType) throws JavaModelException
	{
		IType paramType = null;

		if ( mockType != null && mockType.getSuperclassName() != null)
		{
			String superclassTypeSignature = mockType.getSuperclassTypeSignature();
			String resolvedTypeName = JavaModelUtil.getResolvedTypeName(superclassTypeSignature, mockType);
			String[] typeParamSig = Signature.getTypeArguments(superclassTypeSignature);

			if ( "mockit.MockUp".equals(resolvedTypeName) && typeParamSig.length != 0 )
			{
				String typeParam = typeParamSig[0];
				String resolvedTypeParam = JavaModelUtil.getResolvedTypeName(typeParam, mockType);

				paramType = cunit.getJavaProject().findType(resolvedTypeParam);
			}

		}

		if( paramType == null && mockType != null )
		{
			paramType = findMockAnnotationType(cunit, mockType);
		}

		return paramType;
	}

	private IType findMockAnnotationType(final ICompilationUnit cunit, final IType itype)
			throws JavaModelException
	{
		IType paramType = null;
		IAnnotation[] annotations = itype.getAnnotations();

		for(IAnnotation annot: annotations)
		{
			if( "MockClass".equals( annot.getElementName()) )
			{
				for(IMemberValuePair pair : annot.getMemberValuePairs())
				{
					if( "realClass".equals( pair.getMemberName() ) )
					{
						Object value = pair.getValue();
						String className = value.toString();

						String[][] resolvedTypes = itype.resolveType(className);

						if( resolvedTypes.length != 0 )
						{
							String fullType = JavaModelUtil.concatenateName(resolvedTypes[0][0], resolvedTypes[0][1]);
							paramType = cunit.getJavaProject().findType(  fullType );
						}
					}
				}
			}
		}
		return paramType;
	}

	private List<ICompletionProposal> getProposals(final ContentAssistInvocationContext context, final IType paramType,
			final ICompilationUnit cunit, final Set<String> existingMethods,
			final IProgressMonitor pm, final IType mockType)
					throws JavaModelException, BadLocationException
	{
		Collection<IJavaCompletionProposal> list = new ArrayList<IJavaCompletionProposal>();
		String prefix = context.computeIdentifierPrefix().toString();

		addItFieldProposal(context, paramType, mockType, list, prefix);

		for (final IMethod meth : getAllMethods(paramType, pm) )
		{
			String methodName = meth.getElementName();
			String methSig = methodName+ meth.getSignature();

			if( objectMethods.contains(methSig)
					|| !methodName.startsWith(prefix)
					|| "<clinit>".equals(methodName) )
			{
				continue;
			}

			methodName = meth.isConstructor()  ? "$init" : meth.getElementName();
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

	private void addItFieldProposal(final ContentAssistInvocationContext context, final IType paramType,
			final IType mockType, final Collection<IJavaCompletionProposal> list, final String prefix)
					throws JavaModelException
	{
		boolean hasItField = false;
		for(IField field : mockType.getFields())
		{
			if( field.getElementName().equals("it") )
			{
				hasItField = true;
				break;
			}
		}

		if( !hasItField && "it".startsWith(prefix) )
		{
			String relpacement = paramType.getElementName() + " it;";
			Image image = JavaPluginImages.get(JavaPluginImages.IMG_FIELD_DEFAULT);
			StyledString displayName = new StyledString("it : " + paramType.getElementName());
			displayName.append(" - Access the mocked object 'it' of type "
					+ paramType.getElementName() +"'", QUALIFIER_STYLER);

			IJavaCompletionProposal proposal
			= new JavaCompletionProposal(relpacement, context.getInvocationOffset()-prefix.length(), prefix.length(),
					image, displayName, 1);
			list.add(proposal );
		}
	}

	private MockMethodCompletionProposal createMockMethodProposal(final ContentAssistInvocationContext context,
			final IType paramType, final ICompilationUnit cunit,
			final String prefix, final IMethod meth, final String methodName)
			throws JavaModelException
	{
		String params = getParameters(meth);

		StyledString displayName = new StyledString(methodName
				+ "(" + params + ") : " + Signature.getSimpleName(Signature.toString(meth.getReturnType())));

		String desc = meth.isConstructor() ? "constructor" : "method";
		displayName.append(" - Mock " + desc + " in '"
				+ paramType.getElementName() +"'", QUALIFIER_STYLER);
		String completionProposal = Signature.toString(meth.getReturnType()) + " " + methodName;

		MockMethodCompletionProposal proposal = new MockMethodCompletionProposal(cunit, meth,
				context.getInvocationOffset()-prefix.length(), prefix.length(), displayName,
				completionProposal);

		return proposal;
	}

	private static Collection<IMethod> getAllMethods(final IType paramType, final IProgressMonitor pm)
			throws JavaModelException
	{
		List<IMethod> methods = new ArrayList<IMethod>();
		methods.addAll(Arrays.asList( paramType.getMethods() ) );

		if( paramType.isInterface() )
		{
			for(IType superType: JavaModelUtil.getAllSuperTypes(paramType, pm) )
			{
				if( !"java.lang.Object".equals(superType.getFullyQualifiedName()) )
				{
					methods.addAll( Arrays.asList(superType.getMethods()) );
				}
			}

		}

		Collections.sort(methods, new Comparator<IMethod>()
		{
				@Override
				public int compare(final IMethod o1, final IMethod o2)
				{
					return o1.getElementName().compareTo( o2.getElementName() );
				}
		});

		return methods;
	}

	private String getParameters(final IMethod meth) throws JavaModelException
	{
		String[] paramNames = meth.getParameterNames();

		String params= "";
		String[] paramTypes = meth.getParameterTypes();

		for(int i = 0; i < paramTypes.length; i++)
		{
			String par = paramTypes[i];
			if( i != 0 )
			{
				params += ", ";
			}

			try
			{
				params += Signature.getSimpleName( Signature.toString(par) );
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

	private static Set<String> getObjectMethods(final IJavaProject jproj) throws JavaModelException
	{
		IType objType = jproj.findType("java.lang.Object");

		return getTypeMethods(objType);
	}

	private static Set<String> getTypeMethods(final IType objType) throws JavaModelException
	{
		Set<String> methods = new TreeSet<String>();

		for(IMethod m: objType.getMethods() )
		{
			methods.add(m.getElementName() + m.getSignature() );
		}

		return methods;
	}

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
		return false;
	}

	@Override
	public final IJavaCompletionProposal[] getAssists(final IInvocationContext context,
			final IProblemLocation[] locations)
			throws CoreException
	{
		return new IJavaCompletionProposal[]{};
	}
}
