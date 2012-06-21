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

import jmockit.assist.prefs.Prefs;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


public class Activator extends AbstractUIPlugin
{
	public static final String VM_ARGS = "org.eclipse.jdt.launching.VM_ARGUMENTS";
	public static final  String PROJ_ATTR = "org.eclipse.jdt.launching.PROJECT_ATTR";
	public static final  String JMOCKIT_VAR = "jmockit_javaagent";

	private static BundleContext context;
	private static Activator plugin;

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
		
		launchMan.addLaunchListener(new ILaunchListener()
		{
			@Override
			public void launchRemoved(ILaunch launch)
			{
			}
			
			@Override
			public void launchChanged(ILaunch launch)
			{
				//System.out.println("launch changed");
			}
	
			@SuppressWarnings("restriction")
			@Override
			public void launchAdded(ILaunch launch)
			{
				if( getPrefStore().contains(Prefs.PROP_ADD_JAVAAGENT) && !getPrefStore().getBoolean(Prefs.PROP_ADD_JAVAAGENT) )
				{
					return;
				}
				
				ILaunchConfiguration conf = launch.getLaunchConfiguration();
				try
				{
					String pluginId = conf.getType().getPluginIdentifier();

					if( JUnitCorePlugin.getPluginId().equals(pluginId) )
					{
						addJavaAgentVmArg(conf);
					}					
				}
				catch (CoreException e)
				{
					log(e);
				}
			}
		});
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
	}

	public void addJavaAgentVmArg(ILaunchConfiguration conf) throws CoreException, JavaModelException
	{
		String vmargs = conf.getAttribute(VM_ARGS, "");
		String project = conf.getAttribute(PROJ_ATTR, "");
		
		IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IJavaProject jproj = javaModel.getJavaProject(project);
		
		IType mockitType = jproj.findType("mockit.Mockit");
		
		if( mockitType != null && !vmargs.contains("${" + JMOCKIT_VAR + "}") )
		{
			IPackageFragmentRoot root = (IPackageFragmentRoot) mockitType.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			
			if( root != null && root.isArchive() ) // its a jar
			{
				String javaagentArg = "-javaagent:"+ root.getPath().toOSString();
				setOrCreateVariable(javaagentArg);

				vmargs += " ${" + JMOCKIT_VAR + "}";

				ILaunchConfigurationWorkingCopy confWc = conf.getWorkingCopy();
				confWc.setAttribute(VM_ARGS, vmargs);
				confWc.doSave();
			}
		}
	}

	public void setOrCreateVariable(String value) throws CoreException
	{
		IStringVariableManager varMan = VariablesPlugin.getDefault().getStringVariableManager();
		IValueVariable var = varMan.getValueVariable(JMOCKIT_VAR);
		
		if( var == null )
		{
			var = varMan.newValueVariable(JMOCKIT_VAR, value, false, value);
			varMan.addVariables( new IValueVariable[]{var} );
		}
		else
		{
			var.setValue(value);
		}
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
