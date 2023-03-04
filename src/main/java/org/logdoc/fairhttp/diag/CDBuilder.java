package org.logdoc.fairhttp.diag;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 09.01.2023 17:37
 * fairhttp ☭ sweat and blood
 */
public class CDBuilder {
    private final long reqStart = System.currentTimeMillis();

    private CallData data;
    private String url;
    private String method;
    private Map<String, String> headers;
    private byte[] payload;
    private boolean chunkedInput;

    private int timeout, resCode;
    private boolean followRedirects, allTrusted, skipHostVerify, skipReply, chunkedOutput;
    private long reqWritten, writtenBytes, resStart, resDone, readBytes;
    private String resMessage;
    private byte[] resPayload;
    private Map<String, String> resHeaders;
    private Throwable brokenBy;

    private CDBuilder() {
    }

    public static CDBuilder start(final String url, final String method) {
        final CDBuilder builder = new CDBuilder();
        builder.url = url;
        builder.method = method;

        return builder;
    }

    public static CDBuilder start(final String url, final String method, final Map<String, String> headers, final byte[] payload, final boolean chunkedInput) {
        final CDBuilder builder = new CDBuilder();
        builder.url = url;
        builder.method = method;
        builder.headers = new HashMap<>(headers);
        builder.payload = payload;
        builder.chunkedInput = chunkedInput;

        return builder;
    }

    public CDBuilder broken(final Throwable e) {
        brokenBy = e;
        return this;
    }

    public CallData data() {
        if (data == null)
            build();

        return data;
    }

    private void build() {
        data = new CallData(
                reqStart, reqWritten, resStart, resDone,
                new CallData.Request(method, url, headers, payload, timeout, chunkedInput, writtenBytes, followRedirects, allTrusted, skipHostVerify, skipReply),
                resCode > 0 ? new CallData.Response(resCode, resMessage, resHeaders, resPayload, chunkedOutput, readBytes) : CallData.Response.NOT_HAPPEN(),
                brokenBy
        );
    }

    public CDBuilder options(final int timeout, final boolean followRedirects, final boolean allTrusted, final boolean skipHostVerify, final boolean skipReply) {
        this.timeout = timeout;
        this.followRedirects = followRedirects;
        this.allTrusted = allTrusted;
        this.skipHostVerify = skipHostVerify;
        this.skipReply = skipReply;

        return this;
    }

    public CDBuilder written(final long written) {
        reqWritten = System.currentTimeMillis();
        writtenBytes = written;

        return this;
    }

    public CDBuilder responseStarted(final int code, final String message) {
        resStart = System.currentTimeMillis();
        resCode = code;
        resMessage = message;

        return this;
    }

    public CDBuilder responseDone(final long readBytes, final byte[] resPayload, final Map<String, String> resHeaders, final boolean chunkedOutput) {
        resDone = System.currentTimeMillis();
        this.readBytes = readBytes;
        this.resPayload = resPayload;
        this.resHeaders = new HashMap<>(resHeaders);
        this.chunkedOutput = chunkedOutput;

        return this;
    }

    public CDBuilder headers(final Map<String, String> headers) {
        this.headers = new HashMap<>(headers);

        return this;
    }
}
