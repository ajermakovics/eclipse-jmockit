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
package jmockit.assist.prefs;

import static jmockit.assist.JMockitCompilationParticipant.MARKER;
import jmockit.assist.Activator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public final class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{
	public PreferencePage()
	{
		super(GRID);
		setPreferenceStore(Activator.getPrefStore());
		setDescription("JMockit Eclipse plug-in preferences");
	}

	@Override
	public void init(final IWorkbench workbench)
	{
	}

	@Override
	protected void createFieldEditors()
	{
		String[][] values = new String[][] {
				{"Workspace", Prefs.CheckScope.Workspace.name()},
				{"Current file", Prefs.CheckScope.File.name()},
				{"Current project", Prefs.CheckScope.Project.name()},
				{"Disabled", Prefs.CheckScope.Disabled.name()}
		};

		addField(new ComboFieldEditor(Prefs.PROP_CHECK_SCOPE, "Check scope",
				values , getFieldEditorParent()));

		addField(new BooleanFieldEditor(Prefs.PROP_ADD_JAVAAGENT,
				"Add -javaagent:jmockit.jar when running JUnit", getFieldEditorParent()));
	}

	@Override
	public boolean performOk()
	{
		try
		{
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(MARKER, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			Activator.log(e);
		}
		return super.performOk();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event)
	{
		super.propertyChange(event);

		if( event.getSource() instanceof FieldEditor )
		{
			FieldEditor field = (FieldEditor) event.getSource();

			if( Prefs.PROP_CHECK_SCOPE.equals(field.getPreferenceName()) )
			{
				setMessage("Checks will be performed on next build", INFORMATION);
			}
		}
	}
}
