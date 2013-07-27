package org.esigate.vars;

/**
 * 
 * Utility methods for ESI variables support.
 * 
 * @author Nicolas Richeton
 * 
 */
public class VarUtils {

	/**
	 * Removes simple quotes if any
	 * 
	 * @param s
	 *            input string
	 * @return input string without simple quotes.
	 */
	public static String removeSimpleQuotes(String s) {
		if (s.startsWith("'") && s.endsWith("'")) {
			return s.substring(1, s.length() - 1);
		}
		return s;

	}
}
