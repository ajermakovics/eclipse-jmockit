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



import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;



public class Activator extends AbstractUIPlugin
{
	private static BundleContext context;
	private static Activator plugin;

	private final JunitLaunchListener launchListener = new JunitLaunchListener();

	static BundleContext getContext()
	{
		return context;
	}

	public Activator()
	{
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public final void start(final BundleContext bundleContext) throws Exception
	{
		super.start(bundleContext);
		Activator.context = bundleContext;

		ILaunchManager launchMan = DebugPlugin.getDefault().getLaunchManager();

		launchMan.addLaunchListener(launchListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public final void stop(final BundleContext bundleContext) throws Exception
	{
		super.stop(bundleContext);
		Activator.context = null;

		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(launchListener);
	}

	public static void log(final Exception e)
	{
		e.printStackTrace();
		Status status = createStatus(e);
		plugin.getLog().log(status);
	}

	public static Status createStatus(final Exception e)
	{
		return new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), e.getMessage(), e);
	}

	public static void info(final String msg)
	{
		Status status = new Status(IStatus.INFO, context.getBundle().getSymbolicName(), msg);
		plugin.getLog().log(status);
	}

	public static IPreferenceStore getPrefStore()
	{
		return plugin.getPreferenceStore();
	}

	public static String getActiveProject()
	{
		IResource resource = getActiveResource();

		IProject project = null;

		if (resource != null)
		{
			project = resource.getProject();
		}

		if (project != null && project.isAccessible())
		{
			return project.getName();
		}

		return null;
	}

	public static IResource getActiveResource()
	{
		final AtomicReference<IEditorPart> editorRef = new AtomicReference<IEditorPart>();

		Display display = plugin.getWorkbench().getDisplay();

		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				editorRef.set( getActiveEditor() );
			}
		};

		if( Display.getCurrent() != null )
		{
			runnable.run();
		}
		else
		{
			display.syncExec(runnable);
		}

		IEditorPart editor = editorRef.get();

		if( editor == null )
		{
			return null;
		}

		IAdaptable adaptable = editor.getEditorInput();
		IResource resource = (IResource) adaptable.getAdapter(IResource.class);

		return resource;
	}

	/**
	 * Gets active editor
	 * Must be run from UI thread
	 *
	 * @return IEditorPart
	 */
	public static IEditorPart getActiveEditor()
	{
		if( getWorkbenchWindow() == null )
		{
			return null;
		}

		IWorkbenchPage activePage= getWorkbenchWindow().getActivePage();
		if (activePage != null)
		{
			IEditorPart activeEditor= activePage.getActiveEditor();

			return activeEditor;
		}

		return null;
	}

	public static IWorkbenchWindow getWorkbenchWindow()
	{
		return plugin.getWorkbench().getActiveWorkbenchWindow();
	}
}
