package org.logdoc.fairhttp.diag;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.logdoc.helpers.Texts.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 09.01.2023 17:11
 * fairhttp ☭ sweat and blood
 */
public class CallData {
    private static final Pattern SINGLE_QUOTE_REPLACE = Pattern.compile("'", Pattern.LITERAL);

    public final long id = System.nanoTime();
    public final long requestStarted, requestWritten, responseStarted, responseRead, finita;
    public final Request request;
    public final Response response;
    public final Throwable brokenBy;

    public CallData(final long requestStarted, final long requestWritten, final long responseStarted, final long responseRead, final Request request, final Response response, final Throwable brokenBy) {
        this.requestStarted = requestStarted;
        this.requestWritten = requestWritten;
        this.responseStarted = responseStarted;
        this.responseRead = responseRead;
        this.request = request;
        this.response = response;
        this.brokenBy = brokenBy;
        this.finita = System.currentTimeMillis();
    }

    public String trace() {
        return "*** TRACE " + id + " (" + (finita - requestStarted) + "ms complete call) ***"
                + "\n===>\nREQUEST @ " + LocalDateTime.from(Instant.ofEpochMilli(requestStarted).atZone(ZoneId.systemDefault())) + " - " + LocalDateTime.from(Instant.ofEpochMilli(requestWritten).atZone(ZoneId.systemDefault())) + " :"
                + "\n" + request.trace()
                + "\n===>\nRESPONSE @ " + LocalDateTime.from(Instant.ofEpochMilli(responseStarted).atZone(ZoneId.systemDefault())) + " - " + LocalDateTime.from(Instant.ofEpochMilli(responseRead).atZone(ZoneId.systemDefault())) + " :"
                + "\n" + response.trace()
                + "\n";
    }

    public String asCurl() {
        return request.asCurl();
    }

    public boolean isHttpTransactionDone() {
        return !hasInvokeError() && response != null && response.code > 0;
    }

    public boolean hasInvokeError() {
        return brokenBy != null;
    }

    public long duration() {
        return responseRead - requestStarted;
    }

    public long requestDuration() {
        return requestWritten - requestStarted;
    }

    public long responseDuration() {
        return responseRead - responseStarted;
    }

    public static class Request {
        public final String method;
        public final String url;
        public final Map<String, String> headers;
        public final byte[] payload;
        public final int timeout;
        public final boolean chunked, followRedirects, allTrusted, skipHostVerify, skipReply;
        public final long written;

        public Request(final String method, final String url, final Map<String, String> headers, final byte[] payload, final int timeout, final boolean chunked, final long written, final boolean followRedirects, final boolean allTrusted, final boolean skipHostVerify, final boolean skipReply) {
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.payload = payload;
            this.timeout = timeout;
            this.chunked = chunked;
            this.written = written;
            this.followRedirects = followRedirects;
            this.allTrusted = allTrusted;
            this.skipHostVerify = skipHostVerify;
            this.skipReply = skipReply;
        }

        public String trace() {
            final StringBuilder b = new StringBuilder("> URI | ").append(method).append(" ").append(url).append("\n");
            if (followRedirects || allTrusted || skipHostVerify || skipReply)
                b.append("> Options | ")
                        .append(followRedirects ? "'Follow redirects' " : "")
                        .append(allTrusted ? "'SSL: trust all server certificates' " : "")
                        .append(skipHostVerify ? "'SSL: skip host name verification' " : "")
                        .append(skipReply ? "'Invoke request only, do no read response body' " : "");

            if (timeout > 0) b.append("> Timeout | ").append(timeout).append(" ms\n");

            headers.forEach((k, v) -> b.append("> Header | '").append(k).append("' = '").append(quote(v)).append('\'').append('\n'));

            if (payload != null)
                b.append("> Payload ").append(chunked ? "[chunked] " : "").append("(").append(written).append(" bytes) | '").append(quote(new String(payload, StandardCharsets.UTF_8))).append("'\n");

            return b.toString();
        }

        public String asCurl() {
            final StringBuilder c = new StringBuilder("curl -v -X ").append(allTrusted ? "-k " : "").append(method).append(" '").append(quote(url)).append("' ");

            if (timeout > 0)
                c.append("--connect-timeout ").append(Duration.ofMillis(timeout).get(ChronoUnit.SECONDS)).append(' ');

            headers.forEach((k, v) -> c.append("-H '").append(k).append(": ").append(quote(v)).append('\'').append(' '));

            if (payload != null)
                c.append(" -d '").append(quote(new String(payload, StandardCharsets.UTF_8))).append('\'');

            return c.toString();
        }
    }

    public static class Response {
        public final int code;
        public final String message;
        public final Map<String, String> headers;
        public final byte[] payload;
        public final boolean chunked;
        public final long read;

        public Response(final int code, final String message, final Map<String, String> headers, final byte[] payload, final boolean chunked, final long read) {
            this.code = code;
            this.message = message;
            this.headers = headers;
            this.payload = payload;
            this.chunked = chunked;
            this.read = read;
        }

        public String trace() {
            final StringBuilder b = new StringBuilder(16);

            b.append("< Status | ").append(code);
            if (!isEmpty(message))
                b.append(" '").append(message).append("'");
            b.append('\n');

            if (!isEmpty(headers))
                headers.forEach((k, v) -> b.append("< Header | '").append(k).append("' = '").append(v).append('\'').append('\n'));

            if (!isEmpty(payload))
                b.append("< Data ").append(chunked ? "[chunked] " : "").append("(").append(read).append(" bytes) | '").append(new String(payload, StandardCharsets.UTF_8)).append('\'');

            return b.toString();
        }

        public static Response NOT_HAPPEN() {
            return new Response(-1, null, null, null, false, -1);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CallData callData = (CallData) o;
        return id == callData.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static String quote(String unsafe) {
        return SINGLE_QUOTE_REPLACE.matcher(unsafe).replaceAll(Matcher.quoteReplacement("'\\''"));
    }
}
