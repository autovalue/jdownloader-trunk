/**
 * LICENCE
 *
 * raztoki v0.1
 * personal use doesn't require a licence,
 * educational use requires 'educational licence',
 * any use outside of this is deemed commercial and requires 'commercial licence'.
 * To obtain licence(s) or queries about licensing please make contact via the details below.
 *
 * @author raztoki
 * @email  raztoki <atsign> gmail <period> com
 *
 */
package jd.utils;

import java.util.regex.Pattern;

/**
 * class for custom StringBuilder ideas
 *
 * @author raztoki
 */
public class RazStringBuilder {
    /**
     * to build 'String' from 'Object[]'. Using default separator ' ' (space),
     *
     * @param array
     * @return
     */
    public static String buildString(final Object[] array) {
        return buildString(array, " ");
    }

    /**
     *
     * @param array
     * @param seperator
     * @return
     */
    public static String buildString(final Object[] array, final String seperator) {
        return buildString(array, seperator, false);
    }

    /**
     * to build 'String' from 'Object[] with set specified separator if not null. pattern = true when you're using with Regular
     * Expressions/Patterns.
     *
     * @param array
     * @param separator
     * @param pattern
     * @return
     */
    public static String buildString(final Object[] array, final String separator, final boolean pattern) {
        if (array != null) {
            final StringBuilder s = new StringBuilder();
            for (final Object elem : array) {
                if (s.length() != 0 && separator != null) {
                    s.append(separator);
                }
                if (elem instanceof String) {
                    final String value = ((String) elem).trim();
                    s.append(pattern ? Pattern.quote(value) : value);
                } else {
                    s.append(String.valueOf(elem));
                }
            }
            return s.toString();
        } else {
            return null;
        }
    }
}
