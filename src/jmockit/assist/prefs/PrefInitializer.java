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

import jmockit.assist.Activator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public final class PrefInitializer extends AbstractPreferenceInitializer
{
	public PrefInitializer()
	{
	}

	@Override
	public void initializeDefaultPreferences()
	{
		IPreferenceStore prefStore = Activator.getPrefStore();
		prefStore.setDefault(Prefs.PROP_CHECK_SCOPE, Prefs.CheckScope.Workspace.name());
		prefStore.setDefault(Prefs.PROP_ADD_JAVAAGENT, true);
	}
}
