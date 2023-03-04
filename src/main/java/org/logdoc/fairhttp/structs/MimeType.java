package org.logdoc.fairhttp.structs;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 27.12.2022 13:53
 * fairhttp ☭ sweat and blood
 */
public class MimeType {

    private String primaryType;
    private String subType;
    private ParamsList parameters;

    private static final String TSPECIALS = "()<>@,;:/[]?=\\\"";

    public MimeType() {
        primaryType = "application";
        subType = "*";
        parameters = new ParamsList();
    }

    public MimeType(final String rawdata) throws Exception {
        parse(rawdata);
    }

    public MimeType(final String primary, final String sub) throws Exception {
        if (isValidToken(primary))
            primaryType = primary.toLowerCase();
        else
            throw new Exception("Primary type is invalid.");

        if (isValidToken(sub))
            subType = sub.toLowerCase();
        else
            throw new Exception("Sub type is invalid.");

        parameters = new ParamsList();
    }

    private void parse(final String rawdata) throws Exception {
        int slashIndex = rawdata.indexOf('/');
        int semIndex = rawdata.indexOf(';');

        if (slashIndex < 0 && semIndex < 0)
            throw new Exception("Unable to find a sub type.");

        if (slashIndex < 0)
            throw new Exception("Unable to find a sub type.");

        if (semIndex < 0) {
            primaryType = rawdata.substring(0, slashIndex).trim().toLowerCase();
            subType = rawdata.substring(slashIndex + 1).trim().toLowerCase();
            parameters = new ParamsList();
        } else if (slashIndex < semIndex) {
            primaryType = rawdata.substring(0, slashIndex).trim().toLowerCase();
            subType = rawdata.substring(slashIndex + 1, semIndex).trim().toLowerCase();
            parameters = new ParamsList(rawdata.substring(semIndex));
        } else
            throw new Exception("Unable to find a sub type.");

        if (!isValidToken(primaryType))
            throw new Exception("Primary type is invalid.");

        if (!isValidToken(subType))
            throw new Exception("Sub type is invalid.");
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(final String primary) throws Exception {
        if (!isValidToken(primaryType))
            throw new Exception("Primary type is invalid.");

        primaryType = primary.toLowerCase();
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(final String sub) throws Exception {
        if (!isValidToken(subType))
            throw new Exception("Sub type is invalid.");

        subType = sub.toLowerCase();
    }

    public ParamsList getParameters() {
        return parameters;
    }

    public String getParameter(final String name) {
        return parameters.get(name);
    }

    public void setParameter(final String name, final String value) {
        parameters.set(name, value);
    }

    public void removeParameter(final String name) {
        parameters.remove(name);
    }

    public String toString() {
        return getBaseType() + parameters.toString();
    }

    public String getBaseType() {
        return primaryType + "/" + subType;
    }

    public boolean match(final MimeType type) {
        return primaryType.equals(type.getPrimaryType())
                && (subType.equals("*")
                || type.getSubType().equals("*")
                || (subType.equals(type.getSubType())));
    }

    public boolean match(final String rawdata) throws Exception {
        return match(new MimeType(rawdata));
    }

    private static boolean isTokenChar(final char c) {
        return ((c > 040) && (c < 0177)) && (TSPECIALS.indexOf(c) < 0);
    }

    private boolean isValidToken(final String s) {
        int len = s.length();
        if (len > 0) {
            for (int i = 0; i < len; ++i) {
                char c = s.charAt(i);
                if (!isTokenChar(c)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static class ParamsList {
        private static final String TSPECIALS = "()<>@,;:/[]?=\\\"";
        private final Map<String, String> parameters;

        public ParamsList() {
            parameters = new HashMap<>();
        }

        public ParamsList(final String parameterList) throws Exception {
            parameters = new HashMap<>();

            parse(parameterList);
        }

        protected void parse(final String parameterList) throws Exception {
            if (parameterList == null)
                return;

            int length = parameterList.length();

            if (length == 0)
                return;

            int i;
            char c;
            for (i = skipWhiteSpace(parameterList, 0);
                 i < length && (c = parameterList.charAt(i)) == ';';
                 i = skipWhiteSpace(parameterList, i)) {
                int lastIndex;
                String name;
                String value;

                i++;
                i = skipWhiteSpace(parameterList, i);

                if (i >= length)
                    return;

                lastIndex = i;
                while ((i < length) && isTokenChar(parameterList.charAt(i)))
                    i++;

                name = parameterList.substring(lastIndex, i).toLowerCase();

                i = skipWhiteSpace(parameterList, i);

                if (i >= length || parameterList.charAt(i) != '=')
                    throw new Exception("Couldn't find the '=' that separates a parameter name from its value.");

                i++;
                i = skipWhiteSpace(parameterList, i);

                if (i >= length)
                    throw new Exception("Couldn't find a value for parameter named " + name);

                c = parameterList.charAt(i);
                if (c == '"') {
                    i++;
                    if (i >= length)
                        throw new Exception("Encountered unterminated quoted parameter value.");

                    lastIndex = i;

                    while (i < length) {
                        c = parameterList.charAt(i);
                        if (c == '"')
                            break;
                        if (c == '\\')
                            i++;
                        i++;
                    }

                    if (c != '"')
                        throw new Exception("Encountered unterminated quoted parameter value.");

                    value = unquote(parameterList.substring(lastIndex, i));
                    i++;
                } else if (isTokenChar(c)) {
                    lastIndex = i;
                    while (i < length && isTokenChar(parameterList.charAt(i)))
                        i++;
                    value = parameterList.substring(lastIndex, i);
                } else
                    throw new Exception("Unexpected character encountered at index " + i);

                parameters.put(name, value);
            }

            if (i < length)
                throw new Exception("More characters encountered in input than expected.");
        }

        public int size() {
            return parameters.size();
        }

        public boolean isEmpty() {
            return parameters.isEmpty();
        }

        public String get(String name) {
            return parameters.get(name.trim().toLowerCase());
        }

        public void set(String name, String value) {
            parameters.put(name.trim().toLowerCase(), value);
        }

        public void remove(String name) {
            parameters.remove(name.trim().toLowerCase());
        }

        public Collection<String> getNames() {
            return parameters.keySet();
        }

        private static boolean isTokenChar(char c) {
            return ((c > 040) && (c < 0177)) && (TSPECIALS.indexOf(c) < 0);
        }

        private static int skipWhiteSpace(String rawdata, int i) {
            int length = rawdata.length();

            while ((i < length) && Character.isWhitespace(rawdata.charAt(i)))
                i++;

            return i;
        }

        private static String quote(String value) {
            boolean needsQuotes = false;

            int length = value.length();
            for (int i = 0; (i < length) && !needsQuotes; i++) {
                needsQuotes = !isTokenChar(value.charAt(i));
            }

            if (needsQuotes) {
                final StringBuilder buffer = new StringBuilder();
                buffer.ensureCapacity((int) (length * 1.5));

                buffer.append('"');

                for (int i = 0; i < length; ++i) {
                    char c = value.charAt(i);
                    if ((c == '\\') || (c == '"'))
                        buffer.append('\\');
                    buffer.append(c);
                }

                buffer.append('"');

                return buffer.toString();
            }

            return value;
        }

        private static String unquote(String value) {
            int valueLength = value.length();
            final StringBuilder buffer = new StringBuilder();
            buffer.ensureCapacity(valueLength);

            boolean escaped = false;
            for (int i = 0; i < valueLength; ++i) {
                char currentChar = value.charAt(i);
                if (!escaped && (currentChar != '\\')) {
                    buffer.append(currentChar);
                } else if (escaped) {
                    buffer.append(currentChar);
                    escaped = false;
                } else
                    escaped = true;
            }

            return buffer.toString();
        }

        public String toString() {
            final StringBuilder s = new StringBuilder();

            for (final String key : parameters.keySet())
                s.append("; ").append(key).append('=').append(quote(parameters.get(key)));

            return s.toString();
        }
    }
}
