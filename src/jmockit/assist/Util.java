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
