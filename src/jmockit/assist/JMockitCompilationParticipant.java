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

import static org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
import static org.eclipse.core.resources.IMarker.SEVERITY_WARNING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jmockit.assist.prefs.Prefs;
import jmockit.assist.prefs.Prefs.CheckScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ui.progress.IProgressConstants;


/**
 * Compilation participant to check mock classes and methods for problems
 */
public final class JMockitCompilationParticipant extends CompilationParticipant
{
	private static final int JOB_DELAY = 2000;
	//private static final int JOB_DELAY = 2000;
	public static final String MARKER = "jmockit.eclipse.marker";
	private AnalysisJob job = new AnalysisJob();

	@Override
	public void buildFinished(final IJavaProject project)
	{
	}

	@Override
	public void buildStarting(final BuildContext[] files, final boolean isBatch)
	{
		CheckScope scope = getCheckScope();

		IResource res = Activator.getActiveResource();
		String activeProj = null;
		if( res != null )
		{
			activeProj = res.getProject().getName();
		}

		List<BuildContext> filesToParse = new ArrayList<BuildContext>();
		for (BuildContext f : files)
		{
			IFile file = f.getFile();

			if( (scope == CheckScope.Project || scope == CheckScope.File )
					&& !file.getProject().getName().equals(activeProj))
			{
				continue;
			}

			if( scope == CheckScope.File && (res == null || !file.equals(res)) )
			{
				continue;
			}

			filesToParse.add(f);
		}

		job.addFiles(filesToParse);

		if( isBatch )
		{
			job.schedule(JOB_DELAY);
		}
		else
		{
			job.schedule();
		}
	}

	public static CheckScope getCheckScope()
	{
		String propVal = Activator.getPrefStore().getString(Prefs.PROP_CHECK_SCOPE);
		CheckScope scope = CheckScope.Disabled;

		try
		{
			scope = CheckScope.valueOf(propVal);
		}
		catch(Exception e)
		{
			Activator.log(e);
		}
		return scope;
	}

	@Override
	public boolean isActive(final IJavaProject project)
	{
		CheckScope checkScope = getCheckScope();

		if( checkScope == CheckScope.Disabled || !project.isOpen() )
		{
			return false;
		}

		if( (checkScope == CheckScope.Project || checkScope == CheckScope.File )
				&& !project.getProject().getName().equals(Activator.getActiveProject()) )
		{
			return false;
		}

		IType mockitType = null;
		try
		{
			mockitType = project.findType(MockUtil.MOCKIT);
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
		if( getCheckScope() == CheckScope.Disabled )
		{
			return;
		}

		try
		{
			ICompilationUnit cunit = context.getWorkingCopy();

			if( cunit.isStructureKnown() )
			{
				MockASTVisitor visitor = new MockASTVisitor(cunit);
				context.getAST3().accept(visitor);
				CategorizedProblem[] probs = visitor.getProblems();

				if (probs.length != 0)
				{
					context.putProblems(probs[0].getMarkerType(), probs);
				}
			}
		}
		catch (JavaModelException e)
		{
			Activator.log(e);
		}
	}

	private static class AnalysisJob extends WorkspaceJob
	{
		private Queue<BuildContext> files = new ConcurrentLinkedQueue<BuildContext>();

		public AnalysisJob()
		{
			super("JMockit analysis");

			setSystem(false);
			setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.FALSE);
		}

		void addFiles( final Collection<BuildContext> buildContexts)
		{
			files.addAll(buildContexts);
		}

		@Override
		public IStatus runInWorkspace(final IProgressMonitor mon) throws CoreException
		{
			String taskName = "JMockit file analysis";
			int workSize = files.size(), worked = 0;
			mon.beginTask(taskName, workSize);
			BuildContext f = files.poll();

			while( f != null && !mon.isCanceled() )
			{
				IFile file = f.getFile();

				if( file.isAccessible() && !file.isDerived(IResource.CHECK_ANCESTORS) )
				{
					ICompilationUnit cunit = JavaCore.createCompilationUnitFrom(file);

					try
					{
						mon.setTaskName(taskName + " - " + cunit.getElementName());
						analyseFile(file, cunit, mon);
					}
					catch (Exception e)
					{
						Activator.log(e);
						setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
						files.clear();
						return Activator.createStatus(e);
					}
				}

				mon.worked(1);
				f = files.poll();

				worked++;
				workSize--;

				if( workSize < files.size() ) // more work
				{
					workSize = files.size();
					mon.beginTask(taskName, workSize);
					mon.worked(worked);
				}
			}

			if( mon.isCanceled() )
			{
				files.clear();
				return Status.CANCEL_STATUS;
			}

			mon.done();

			return Status.OK_STATUS;
		}

		public void analyseFile(final IFile file, final ICompilationUnit cunit, final IProgressMonitor mon)
				throws JavaModelException, CoreException
		{
			if ( cunit != null && cunit.exists() && cunit.isStructureKnown() )
			{
				CompilationUnit cu = ASTUtil.getAstOrParse(cunit, mon);

				MockASTVisitor visitor = new MockASTVisitor(cunit);
				cu.accept(visitor);

				CategorizedProblem[] probs = visitor.getProblems();
				file.deleteMarkers(MARKER, true, IResource.DEPTH_INFINITE);

				for (CategorizedProblem prob : probs) //f.recordNewProblems(probs);
				{
					createProblemMarker(file, prob);
				}
			}
		}

		private static void createProblemMarker(final IFile file, final CategorizedProblem prob) throws CoreException
		{
			IMarker marker = file.createMarker(MARKER);
			marker.setAttribute(IMarker.TRANSIENT, true);
			marker.setAttribute(IMarker.MESSAGE, prob.getMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, prob.getSourceLineNumber());
			marker.setAttribute(IMarker.CHAR_START, prob.getSourceStart());
			marker.setAttribute(IMarker.CHAR_END, prob.getSourceEnd());
			marker.setAttribute(IMarker.SEVERITY, prob.isError()?SEVERITY_ERROR:SEVERITY_WARNING);
		}
	}

}