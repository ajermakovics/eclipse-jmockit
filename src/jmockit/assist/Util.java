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

import java.util.Collection;
import java.util.Collections;

public class Util
{
	public static <T> T firstNonNull(final T...ts)
	{
		for(T t: ts)
		{
			if( t != null )
			{
				return t;
			}
		}

		return null;
	}

	public static <T> Collection<T> emptyIfNull(final Collection<T> coll)
	{
		if( coll == null )
		{
			return Collections.emptyList();
		}

		return coll;
	}
}
