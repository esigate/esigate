package org.esigate.vars;

public class VarUtils {

	public static String removeSimpleQuotes(String s) {
		if (s.startsWith("'") && s.endsWith("'")) {
			return s.substring(1, s.length() - 1);
		}
		return s;

	}
}
