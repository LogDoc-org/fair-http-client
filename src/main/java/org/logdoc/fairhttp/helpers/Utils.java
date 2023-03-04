package org.logdoc.fairhttp.helpers;

import org.logdoc.fairhttp.structs.traits.ContentTypes;
import org.w3c.dom.Node;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 13:22
 * fairhttp ☭ sweat and blood
 */
public class Utils {
    public static final SecureRandom rnd = new SecureRandom();
    public static final byte[] FEED = new byte[]{'\r', '\n'};
    private static final Pattern SINGLE_QUOTE_REPLACE = Pattern.compile("'", Pattern.LITERAL);

    public static final TrustManager[] trustAllManager = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }};

    static {
        rnd.setSeed(System.currentTimeMillis());
    }

    public static byte[] randomBytes(final int len) {
        if (len <= 0)
            return new byte[0];

        final byte[] bytes = new byte[len];
        rnd.nextBytes(bytes);

        return bytes;
    }

    public static String urlEnc(final String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (final Exception ignore) {
        }

        return s;
    }

    public static String toString(final Node doc) {
        try {
            return toStringUnsafe(doc);
        } catch (final TransformerException e) {
            return doc + " " + e;
        }
    }

    public static byte[] xml2StringBytes(final Node doc) {
        try { return toStringUnsafe(doc).getBytes(StandardCharsets.UTF_8); } catch (final Exception ignore) { }

        return null;
    }

    public static String toStringUnsafe(final Node doc) throws TransformerException {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString().replaceAll("[\n\r]", "");
    }

    public static String readHeaderLine(final InputStream is) throws IOException {
        byte b, prev = -1;

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(4096)) {
            while ((b = (byte) is.read()) != -1) {
                if (b == '\n' && prev == '\r')
                    return new String(Arrays.copyOfRange(os.toByteArray(), 0, os.size() - 1), StandardCharsets.US_ASCII);

                os.write(b);
                prev = b;
            }
        }

        return null;
    }

    public static void copy(final InputStream in, final OutputStream out) {
        copy(false, in, out);
    }

    public static void copy(final boolean rethrow, final InputStream in, final OutputStream out) {
        final byte[] tmp = new byte[1024 * 1024];
        int read;

        try {
            while ((read = in.read(tmp, 0, tmp.length)) != -1)
                out.write(tmp, 0, read);

            out.flush();
        } catch (final IOException e) {
            if (rethrow)
                throw new RuntimeException(e);
        }
    }

    public static byte[] readChunkBody(final int size, final InputStream is) throws IOException {
        final byte[] body = new byte[size];

        for (int i = 0; i < size; i++)
            body[i] = (byte) is.read();

        return body;
    }

    public static int readChunkLen(final InputStream is) throws IOException {
        int b, prev = '0';

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(8)) {
            while ((b = is.read()) != '\n' || prev != '\r') {
                if (b == '\n')
                    os.reset();
                else if (Character.digit(b, 16) != -1)
                    os.write(b);
                prev = b;
            }

            return Integer.parseInt(new String(os.toByteArray(), StandardCharsets.US_ASCII), 16);
        }
    }

    public static boolean isEmpty(final Object o) {
        return o == null
                || (CharSequence.class.isAssignableFrom(o.getClass()) && o.toString().trim().isEmpty())
                || (o instanceof Collection && ((Collection<?>) o).isEmpty())
                || (o.getClass().isArray() && Array.getLength(o) == 0)
                || (o instanceof Map && ((Map<?, ?>) o).isEmpty());
    }

    public static boolean anyNotEmpty(final Object... o) {
        return o == null || o.length == 0 || Arrays.stream(o).anyMatch(o1 -> !isEmpty(o1));
    }

    public static boolean allEmpty(final Object... o) {
        return o == null || o.length == 0 || Arrays.stream(o).allMatch(Utils::isEmpty);
    }

    public static boolean noneEmpty(final Object... o) {
        return o != null && o.length > 0 && Arrays.stream(o).noneMatch(Utils::isEmpty);
    }

    public static String notNull(final Object o, final Object def) {
        if (!isEmpty(def) && isEmpty(o))
            return notNull(def);

        return notNull(o);
    }

    public static int getInt(Object parameter, int max, int min) {
        int i = getInt(parameter);
        return i > max ? max : Math.max(i, min);
    }

    public static String quote(String unsafe) {
        return SINGLE_QUOTE_REPLACE.matcher(unsafe).replaceAll(Matcher.quoteReplacement("'\\''"));
    }

    public static int getInt(Object parameter) {
        String param = notNull(parameter);

        try {
            return Integer.decode(param);
        } catch (Exception var5) {
            try {
                return Integer.parseInt(param.replaceAll("([^0-9-])", ""));
            } catch (Exception var4) {
                return 0;
            }
        }
    }

    public static String safeHeaderRecord(final String key, final String value) {
        if (isEmpty(key) || isEmpty(value))
            return "";

        return key + ": " + value + "\r\n";
    }

    public static String notNull(final Object o) {
        if (o == null)
            return "";

        return String.valueOf(o).trim();
    }

    public static String guessMime(final byte[] head) {
        final int[] ints = new int[head.length];
        for (int i = 0; i < head.length; i++)
            ints[i] = head[i];

        return guessMime(ints);
    }

    public static String guessMime(final int[] head) {
        if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8')
            return "image/gif";

        if (head[0] == '#' && head[1] == 'd' && head[2] == 'e' && head[3] == 'f')
            return "image/x-bitmap";

        if (head[0] == 0xCA && head[1] == 0xFE && head[2] == 0xBA && head[3] == 0xBE)
            return "application/java-vm";

        if (head[0] == 0xAC && head[1] == 0xED)
            return "application/x-java-serialized-object";

        if (head[0] == 0x2E && head[1] == 0x73 && head[2] == 0x6E && head[3] == 0x64)
            return "audio/basic";  // .au BE

        if (head[0] == 0x64 && head[1] == 0x6E && head[2] == 0x73 && head[3] == 0x2E)
            return "audio/basic";  // .au LE

        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F')
            return "audio/x-wav";

        if (head[0] == '<') {
            if (head[1] == '!'
                    || ((head[1] == 'h' && (head[2] == 't' && head[3] == 'm' && head[4] == 'l' ||
                    head[2] == 'e' && head[3] == 'a' && head[4] == 'd') ||
                    (head[1] == 'b' && head[2] == 'o' && head[3] == 'd' && head[4] == 'y'))) ||
                    ((head[1] == 'H' && (head[2] == 'T' && head[3] == 'M' && head[4] == 'L' ||
                            head[2] == 'E' && head[3] == 'A' && head[4] == 'D') ||
                            (head[1] == 'B' && head[2] == 'O' && head[3] == 'D' && head[4] == 'Y'))))
                return "text/html";

            if (head[1] == '?' && head[2] == 'x' && head[3] == 'm' && head[4] == 'l' && head[5] == ' ')
                return "application/xml";
        }

        if (head[0] == 0xef && head[1] == 0xbb && head[2] == 0xbf && head[3] == '<' && head[4] == '?' && head[5] == 'x')
            return "application/xml";

        if (head[0] == '!' && head[1] == ' ' && head[2] == 'X' && head[3] == 'P' && head[4] == 'M' && head[5] == '2')
            return "image/x-pixmap";

        if (head[0] == 0xfe && head[1] == 0xff && head[2] == 0 && head[3] == '<' && head[4] == 0 && head[5] == '?' && head[6] == 0 && head[7] == 'x')
            return "application/xml";

        if (head[0] == 0xff && head[1] == 0xfe && head[2] == '<' && head[3] == 0 && head[4] == '?' && head[5] == 0 && head[6] == 'x' && head[7] == 0)
            return "application/xml";

        if (head[0] == 137 && head[1] == 80 && head[2] == 78 && head[3] == 71 && head[4] == 13 && head[5] == 10 && head[6] == 26 && head[7] == 10)
            return "image/png";

        if (head[0] == 0xFF && head[1] == 0xD8 && head[2] == 0xFF) {
            if (head[3] == 0xE0 || head[3] == 0xEE)
                return "image/jpeg";

            if (head[3] == 0xE1 && head[6] == 'E' && head[7] == 'x' && head[8] == 'i' && head[9] == 'f' && head[10] == 0)
                return "image/jpeg";
        }

        if (head[0] == 0x00 && head[1] == 0x00 && head[2] == 0xfe && head[3] == 0xff && head[4] == 0 && head[5] == 0 && head[6] == 0 && head[7] == '<' &&
                head[8] == 0 && head[9] == 0 && head[10] == 0 && head[11] == '?' &&
                head[12] == 0 && head[13] == 0 && head[14] == 0 && head[15] == 'x')
            return "application/xml";

        if (head[0] == 0xff && head[1] == 0xfe && head[2] == 0x00 && head[3] == 0x00 && head[4] == '<' && head[5] == 0 && head[6] == 0 && head[7] == 0 &&
                head[8] == '?' && head[9] == 0 && head[10] == 0 && head[11] == 0 &&
                head[12] == 'x' && head[13] == 0 && head[14] == 0 && head[15] == 0)
            return "application/xml";

        return ContentTypes.binary.toString();
    }

    // http://bjoern.hoehrmann.de/utf-8/decoder/dfa/
    private static final int[] utf8d = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, // 00..1f
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, // 20..3f
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, // 40..5f
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, // 60..7f
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
            9, // 80..9f
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, // a0..bf
            8, 8, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, // c0..df
            0xa, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x4, 0x3, 0x3, // e0..ef
            0xb, 0x6, 0x6, 0x6, 0x5, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, // f0..ff
            0x0, 0x1, 0x2, 0x3, 0x5, 0x8, 0x7, 0x1, 0x1, 0x1, 0x4, 0x6, 0x1, 0x1, 0x1, 0x1, // s0..s0
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1,
            1, // s1..s2
            1, 2, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1,
            1, // s3..s4
            1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 1, 1, 1, 1, 1,
            1, // s5..s6
            1, 3, 1, 1, 1, 1, 1, 3, 1, 3, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
            // s7..s8
    };

    public static boolean isValidUTF8(final byte[] data, int off) {
        final int len = data.length;
        if (len < off)
            return false;

        for (int i = off, state = 0; i < len; ++i) {
            state = utf8d[256 + (state << 4) + utf8d[(0xff & data[i])]];

            if (state == 1)
                return false;
        }

        return true;
    }

    public static boolean isValidUTF8(final byte[] data) {
        return isValidUTF8(data, 0);
    }

    public static String stringUtf8(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeInt(final int in, final OutputStream os) throws IOException {
        os.write((in >>> 24) & 0xff);
        os.write((in >>> 16) & 0xff);
        os.write((in >>> 8) & 0xff);
        os.write((in) & 0xff);
    }

    public static byte[] byteInt(final int in) {
        final byte[] intb = new byte[4];
        intb[0] = (byte) ((in >>> 24) & 0xff);
        intb[1] = (byte) ((in >>> 16) & 0xff);
        intb[2] = (byte) ((in >>> 8) & 0xff);
        intb[3] = (byte) ((in) & 0xff);

        return intb;
    }

    private static final int INADDR4SZ = 4;
    private static final int INADDR16SZ = 16;
    private static final int INT16SZ = 2;
    private static final int HEXADECIMAL = 16;
    private static final int DECIMAL = 10;

    public static boolean isIPv4LiteralAddress(String src) {
        return textToNumericFormatV4(src) != null;
    }

    public static boolean isIPv6LiteralAddress(String src) {
        return textToNumericFormatV6(src) != null;
    }

    public static byte[] textToNumericFormatV6(String src) {
        if (src.length() < 2)
            return null;

        int colonp;
        char ch;
        boolean saw_xdigit;
        int val;
        char[] srcb = src.toCharArray();
        byte[] dst = new byte[INADDR16SZ];

        int srcb_length = srcb.length;
        int pc = src.indexOf('%');
        if (pc == srcb_length - 1)
            return null;

        if (pc != -1)
            srcb_length = pc;

        colonp = -1;
        int i = 0, j = 0;

        if (srcb[i] == ':')
            if (srcb[++i] != ':')
                return null;
        int curtok = i;
        saw_xdigit = false;
        val = 0;
        while (i < srcb_length) {
            ch = srcb[i++];
            int chval = digit(ch, 16);
            if (chval != -1) {
                val <<= 4;
                val |= chval;
                if (val > 0xffff)
                    return null;
                saw_xdigit = true;
                continue;
            }
            if (ch == ':') {
                curtok = i;
                if (!saw_xdigit) {
                    if (colonp != -1)
                        return null;
                    colonp = j;
                    continue;
                } else if (i == srcb_length) {
                    return null;
                }
                if (j + INT16SZ > INADDR16SZ)
                    return null;
                dst[j++] = (byte) ((val >> 8) & 0xff);
                dst[j++] = (byte) (val & 0xff);
                saw_xdigit = false;
                val = 0;
                continue;
            }
            if (ch == '.' && ((j + INADDR4SZ) <= INADDR16SZ)) {
                String ia4 = src.substring(curtok, srcb_length);
                int dot_count = 0, index = 0;
                while ((index = ia4.indexOf('.', index)) != -1) {
                    dot_count++;
                    index++;
                }
                if (dot_count != 3) {
                    return null;
                }
                byte[] v4addr = textToNumericFormatV4(ia4);
                if (v4addr == null) {
                    return null;
                }
                for (int k = 0; k < INADDR4SZ; k++) {
                    dst[j++] = v4addr[k];
                }
                saw_xdigit = false;
                break;
            }
            return null;
        }
        if (saw_xdigit) {
            if (j + INT16SZ > INADDR16SZ)
                return null;
            dst[j++] = (byte) ((val >> 8) & 0xff);
            dst[j++] = (byte) (val & 0xff);
        }

        if (colonp != -1) {
            int n = j - colonp;

            if (j == INADDR16SZ)
                return null;
            for (i = 1; i <= n; i++) {
                dst[INADDR16SZ - i] = dst[colonp + n - i];
                dst[colonp + n - i] = 0;
            }
            j = INADDR16SZ;
        }
        if (j != INADDR16SZ)
            return null;

        final byte[] newdst = convertFromIPv4MappedAddress(dst);

        if (newdst != null)
            return newdst;

        return dst;
    }

    public static byte[] convertFromIPv4MappedAddress(byte[] addr) {
        if (isIPv4MappedAddress(addr)) {
            byte[] newAddr = new byte[INADDR4SZ];
            System.arraycopy(addr, 12, newAddr, 0, INADDR4SZ);
            return newAddr;
        }

        return null;
    }

    private static boolean isIPv4MappedAddress(byte[] addr) {
        if (addr.length < INADDR16SZ)
            return false;

        return (addr[0] == 0x00) && (addr[1] == 0x00) &&
                (addr[2] == 0x00) && (addr[3] == 0x00) &&
                (addr[4] == 0x00) && (addr[5] == 0x00) &&
                (addr[6] == 0x00) && (addr[7] == 0x00) &&
                (addr[8] == 0x00) && (addr[9] == 0x00) &&
                (addr[10] == (byte) 0xff) &&
                (addr[11] == (byte) 0xff);
    }

    public static byte[] textToNumericFormatV4(final String src) {
        byte[] res = new byte[INADDR4SZ];

        long tmpValue = 0;
        int currByte = 0;
        boolean newOctet = true;

        int len = src.length();
        if (len == 0 || len > 15) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);
            if (c == '.') {
                if (newOctet || tmpValue < 0 || tmpValue > 0xff || currByte == 3) {
                    return null;
                }
                res[currByte++] = (byte) (tmpValue & 0xff);
                tmpValue = 0;
                newOctet = true;
            } else {
                int digit = digit(c, 10);
                if (digit < 0) {
                    return null;
                }
                tmpValue *= 10;
                tmpValue += digit;
                newOctet = false;
            }
        }
        if (newOctet || tmpValue < 0 || tmpValue >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        switch (currByte) {
            case 0:
                res[0] = (byte) ((tmpValue >> 24) & 0xff);
            case 1:
                res[1] = (byte) ((tmpValue >> 16) & 0xff);
            case 2:
                res[2] = (byte) ((tmpValue >> 8) & 0xff);
            case 3:
                res[3] = (byte) ((tmpValue) & 0xff);
        }
        return res;
    }

    public static int digit(char c, int radix) {
        if (radix == HEXADECIMAL)
            return parseAsciiHexDigit(c);

        final int val = c - '0';

        return (val < 0 || val >= radix) ? -1 : val;
    }

    private static int parseAsciiHexDigit(char digit) {
        char c = Character.toLowerCase(digit);
        if (c >= 'a' && c <= 'f')
            return c - 'a' + 10;

        return digit(c, DECIMAL);
    }
}
