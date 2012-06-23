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



import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;



public class Activator extends AbstractUIPlugin
{
	private static BundleContext context;
	private static Activator plugin;

	private final LaunchListener launchListener = new LaunchListener();
	
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
		Status status = new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), e.getMessage(), e);
		plugin.getLog().log(status);
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

}
