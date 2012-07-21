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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
public class HyperlinkDetector extends AbstractHyperlinkDetector
{

	public HyperlinkDetector()
	{
	}

	@Override
	public final IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region,
			final boolean canShowMultipleHyperlinks)
	{
		ITextEditor textEditor= (ITextEditor) getAdapter(ITextEditor.class);
		if (region == null || !(textEditor instanceof JavaEditor))
		{
			return null;
		}

		ITypeRoot input = EditorUtility.getEditorInputJavaElement(textEditor, false);
		if (input == null)
		{
			return null;
		}

		IAction openAction= textEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (!(openAction instanceof SelectionDispatchAction))
		{
			return null;
		}

		ITypeBinding paramType = null;
		IRegion wordRegion = null;
		IMethodBinding mockMethod = null, realMethod = null;

		CompilationUnit astRoot = ASTUtil.getAstOrParse(input, null);
		if( astRoot == null )
		{
			return null;
		}

		ASTNode node = NodeFinder.perform(astRoot, region.getOffset(), 1);

		if( node instanceof SimpleName && node.getParent() instanceof MethodDeclaration )
		{
			MethodDeclaration mdec = (MethodDeclaration) node.getParent();
			mockMethod = mdec.resolveBinding();

			paramType = MockUtil.findMockedType(mdec, mockMethod);

			wordRegion = new Region(node.getStartPosition(), node.getLength());

			if( paramType != null && mockMethod != null )
			{
				realMethod = MockUtil.findRealMethodInType(paramType, mockMethod, astRoot.getAST());
			}

		}

		if ( realMethod != null && wordRegion != null )
		{
			SelectionDispatchAction dispatchAction = (SelectionDispatchAction) openAction;

			return new IHyperlink[]{new OpenMockedMethodHyperlink(dispatchAction, realMethod, wordRegion)};
		}

		return null;
	}

	static class OpenMockedMethodHyperlink implements IHyperlink
	{

		private IRegion region;
		private IMethodBinding realMethod;
		private SelectionDispatchAction action;

		public OpenMockedMethodHyperlink(final SelectionDispatchAction dispatchAction,
				final IMethodBinding method, final IRegion wordRegion)
		{
			this.region = wordRegion;
			this.realMethod = method;
			this.action = dispatchAction;
		}

		@Override
		public IRegion getHyperlinkRegion()
		{
			return region;
		}

		@Override
		public String getTypeLabel()
		{
			return null;
		}

		@Override
		public String getHyperlinkText()
		{
			return "Open Mocked (Real) Method";
		}

		@Override
		public void open()
		{
			try
			{
				IMethod method= (IMethod) realMethod.getJavaElement();

				if (method != null)
				{
					action.run(new StructuredSelection(method));
				}
			}
			catch(Exception e)
			{
				Activator.log(e);
			}
		}

	}
}
