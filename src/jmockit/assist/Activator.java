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

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

@SuppressWarnings("restriction")
public class Activator implements BundleActivator
{

	private static BundleContext context;

	static BundleContext getContext()
	{
		return context;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public final void start(final BundleContext bundleContext) throws Exception
	{
		Activator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public final void stop(final BundleContext bundleContext) throws Exception
	{
		Activator.context = null;
	}

	public static void log(final Exception e)
	{
		e.printStackTrace();
		Status status = new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), e.getMessage(), e);
		getLog().log(status);
	}

	public static void info(final String msg)
	{
		Status status = new Status(IStatus.INFO, context.getBundle().getSymbolicName(), msg);
		getLog().log(status);
	}

	public static ILog getLog()
	{
		return InternalPlatform.getDefault().getLog(context.getBundle());
	}

}
