package org.zephyrsoft.util;

public class NullSafeUtils {
	
	public static boolean safeEquals(Object one, Object two) {
		return ((one==null && two==null)
				|| (one!=null && two!=null && one.equals(two)));
	}
	
}
