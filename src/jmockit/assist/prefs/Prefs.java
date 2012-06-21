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

public final class Prefs
{
	public static final String PROP_CHECK_SCOPE = "checkScope";
	public static final String PROP_ADD_JAVAAGENT= "addJavaAgent";


	public enum CheckScope
	{
		Workspace,
		Project,
		File,
		Disabled
	}
}
