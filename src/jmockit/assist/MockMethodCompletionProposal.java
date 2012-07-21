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

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.viewers.StyledString;

@SuppressWarnings("restriction")
/**
 * Mock method completion proposal. Inserts mock method in code.
 */
public class MockMethodCompletionProposal extends JavaTypeCompletionProposal
implements ICompletionProposalExtension4
{

	static final int MAX_RELEVANCE = 100;
	static final int METHOD_RELEVANCE = 90;
	static final int OBJ_METHOD_RELEVANCE = 80;

	private final IMethodBinding method;

	private final IJavaProject fJavaProject;
	private ICompilationUnit cunit;

	private ImportRewriteContext context;
	private ImportRewrite importRewrite;

	public MockMethodCompletionProposal(final ICompilationUnit cu,
			final IMethodBinding meth, final int start, final int length,
			final StyledString displayName, final String completionProposal)
					throws IllegalArgumentException, JavaModelException
	{
		super(completionProposal, cu, start, length, null, displayName, METHOD_RELEVANCE );

		Assert.isNotNull(meth);
		Assert.isNotNull(cu);

		method = meth;
		cunit = cu;
		fJavaProject = cu.getJavaProject();

		StringBuffer buffer = new StringBuffer();
		buffer.append("@Mock ");
		buffer.append(completionProposal);
		buffer.append(" { }");

		if( Object.class.getName().equals( method.getDeclaringClass().getQualifiedName() ) ) // from Object
		{
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));
			setRelevance(OBJ_METHOD_RELEVANCE);
		}
		else
		{
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_MISC_DEFAULT));
		}

		setReplacementString(buffer.toString());
	}

	@Override
	public final CharSequence getPrefixCompletionText(final IDocument document, final int completionOffset)
	{
		return method.getName();
	}

	@Override
	protected final boolean updateReplacementString(final IDocument document, final char trigger, final int offset,
			final ImportRewrite importRw) throws CoreException, BadLocationException
	{
		try
		{
			Document recoveredDocument = new Document();
			CompilationUnit unit = getRecoveredAST(document, offset, recoveredDocument);
			initContext(offset, importRw, unit);

			ASTNode node = NodeFinder.perform(unit, offset, 1);
			AST ast = unit.getAST();
			ASTRewrite rewrite = ASTRewrite.create(ast);

			CodeGenerationSettings settings = getCodeGenSettings();

			MethodDeclaration stub = createMockMethodStub(ast, rewrite, settings);

			String methodDeclarationText = generateMethodDeclaration(document, recoveredDocument,
					node, rewrite, settings, stub);

			setReplacementString(methodDeclarationText);

		}
		catch (Exception exception)
		{
			Activator.log(exception);
		}


		return true;
	}

	private String generateMethodDeclaration(final IDocument document, final Document recoveredDocument,
			final ASTNode node, final ASTRewrite rewrite, final CodeGenerationSettings settings,
			final MethodDeclaration stub) throws BadLocationException
	{
		ChildListPropertyDescriptor descriptor = getPropDescriptor(node);
		ListRewrite rewriter = rewrite.getListRewrite(node, descriptor);
		rewriter.insertFirst(stub, null);

		ITrackedNodePosition position = rewrite.track(stub);

		rewrite.rewriteAST(recoveredDocument, fJavaProject.getOptions(true)).apply(recoveredDocument);

		String generatedCode = recoveredDocument.get(position.getStartPosition(), position.getLength());

		int generatedIndent = IndentManipulation.measureIndentUnits(
				getIndentAt(recoveredDocument, position.getStartPosition(), settings), settings.tabWidth,
				settings.indentWidth);

		String indent = getIndentAt(document, getReplacementOffset(), settings);
		String methodDeclarationText = IndentManipulation.changeIndent(generatedCode, generatedIndent,
				settings.tabWidth, settings.indentWidth, indent,
				TextUtilities.getDefaultLineDelimiter(document));

		return methodDeclarationText;
	}

	private MethodDeclaration createMockMethodStub(final AST ast, final ASTRewrite rewrite,
			final CodeGenerationSettings settings) throws CoreException, JavaModelException
	{
		ITypeBinding declaringType = method.getDeclaringClass();

		MethodDeclaration stub = StubUtility2.createImplementationStub(fCompilationUnit, rewrite,
				importRewrite, context, method, declaringType.getName(), settings, false);

		if( !Object.class.getName().equals( method.getDeclaringClass().getQualifiedName() ) )
		{
			stub.modifiers().clear();
		}

		ASTUtil.addAnnotation("Mock", fJavaProject, rewrite, stub, method);
		importRewrite.addImport(MockUtil.MOCK, context);

		if( method.isConstructor() )
		{
			stub.setName( ast.newSimpleName(MockUtil.CTOR) );
			stub.getBody().statements().clear();
		}
		else
		{
			setReturnStatement(stub, method, declaringType, ast, rewrite);
		}
		
		if( "void".equals(method.getReturnType().getName()) )
		{
			stub.getBody().statements().clear();
		}
		
		return stub;
	}

	private static ImportRewriteContext createImportRewriteContext()
	{
		ImportRewriteContext importContext;
		importContext = new ImportRewriteContext()
		{ // forces that all imports are fully qualified
			@Override
			public int findInContext(final String qualifier, final String name, final int kind)
			{
				return RES_NAME_CONFLICT;
			}
		};
		return importContext;
	}

	@SuppressWarnings("unchecked")
	private void setReturnStatement(final MethodDeclaration stub, final IMethodBinding methodToOverride,
			final ITypeBinding declaringType, final AST ast, final ASTRewrite rewrite)
					throws JavaModelException, CoreException
					{
		Expression expression= ASTNodeFactory.newDefaultExpression(ast,
				stub.getReturnType2(), stub.getExtraDimensions());

		if (expression != null)
		{
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(expression);

			String delimiter= cunit.findRecommendedLineSeparator();
			Map<String, String> options= fJavaProject.getOptions(true);
			String bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, delimiter, options);

			String placeHolder= CodeGeneration.getMethodBodyContent(cunit,
					declaringType.getName(), methodToOverride.getName(), false, bodyStatement, delimiter);

			if (placeHolder != null)
			{
				ASTNode todoNode = rewrite.createStringPlaceholder(placeHolder, ASTNode.RETURN_STATEMENT);
				stub.getBody().statements().clear();
				stub.getBody().statements().add(todoNode);
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	@Override
	public final boolean isAutoInsertable()
	{
		return false;
	}

	private void initContext(final int offset, final ImportRewrite importRw, final CompilationUnit unit)
	{
		importRewrite = importRw;

		if (importRewrite != null)
		{
			context = new ContextSensitiveImportRewriteContext(unit, offset, importRewrite);
		}
		else
		{
			importRewrite = StubUtility.createImportRewrite(unit, true); // create a dummy import rewriter to have one
			context = createImportRewriteContext();
		}
	}

	private CodeGenerationSettings getCodeGenSettings()
	{
		CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fJavaProject);
		settings.overrideAnnotation = false;
		settings.createComments = false;

		return settings;
	}

	private ChildListPropertyDescriptor getPropDescriptor(final ASTNode node)
	{
		ChildListPropertyDescriptor descriptor = TypeDeclaration.BODY_DECLARATIONS_PROPERTY;

		if (node instanceof AnonymousClassDeclaration)
		{
			descriptor = AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		}

		return descriptor;
	}

	private static String getIndentAt(final IDocument document, final int offset, final CodeGenerationSettings settings)
	{
		try
		{
			IRegion region = document.getLineInformationOfOffset(offset);
			return IndentManipulation.extractIndentString(document.get(region.getOffset(), region.getLength()),
					settings.tabWidth, settings.indentWidth);
		}
		catch (BadLocationException e)
		{
			return "";
		}
	}

	private CompilationUnit getRecoveredAST(final IDocument document, final int offset,
			final Document recoveredDocument)
	{
		CompilationUnit ast = SharedASTProvider.getAST(fCompilationUnit, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
		if (ast != null)
		{
			recoveredDocument.set(document.get());
			return ast;
		}

		char[] content = document.get().toCharArray();

		// clear prefix to avoid compile errors
		int index = offset - 1;
		while (index >= 0 && Character.isJavaIdentifierPart(content[index]))
		{
			content[index] = ' ';
			index--;
		}

		recoveredDocument.set(new String(content));

		final ASTParser parser = ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(true);
		parser.setSource(content);
		parser.setUnitName(fCompilationUnit.getElementName());
		parser.setProject(fCompilationUnit.getJavaProject());

		return (CompilationUnit) parser.createAST(new NullProgressMonitor());
	}
}
