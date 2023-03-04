package org.logdoc.fairhttp;

import org.logdoc.fairhttp.helpers.FairErrorHandler;
import org.logdoc.fairhttp.flow.FairResponse;
import org.logdoc.fairhttp.flow.FairSocket;
import org.logdoc.fairhttp.structs.MimeType;
import org.logdoc.fairhttp.structs.Point;
import org.logdoc.fairhttp.helpers.SocketConsumer;
import org.logdoc.fairhttp.structs.traits.Methods;
import org.logdoc.fairhttp.structs.websocket.extension.IExtension;
import org.logdoc.fairhttp.structs.websocket.protocol.IProtocol;

import java.net.Proxy;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 07.01.2023 15:00
 * fairhttp ☭ sweat and blood
 */
public class FairCall extends Payloads implements CallSugar {

    private final FairBase base;

    FairCall(final Point destination) {
        base = new FairBase(destination);
    }

    public FairCall(final FairBase base) {
        this.base = base;
    }

    public FairFuture future() {
        return new FairFuture(base);
    }

    public FairCall contentType(final String contentType) {
        base.contentType(contentType);

        return this;
    }

    public FairCall contentType(final MimeType contentType) {
        base.contentType(contentType);

        return this;
    }

    public FairCall withChunksReader(final Consumer<byte[]> chunkReader) {
        base.chunkReader = chunkReader;

        return this;
    }

    public FairCall errorHandler(final FairErrorHandler errorHandler) {
        base.errorHandler(errorHandler);

        return this;
    }

    public FairCall incognito() {
        base.incognito();

        return this;
    }

    public FairCall executor(final Executor executor) {
        base.executor = executor;

        return this;
    }

    public FairCall withChunksWriter(final Supplier<byte[]> chunksWriter) {
        base.withChunksWriter(chunksWriter);

        return this;
    }

    public FairCall withtOption(final Fair.Option option, final boolean state) {
        base.option(option, state);

        return this;
    }

    FairResponse httpCall(final Methods method) {
        base.method = method;
        return base.httpCall();
    }

    @Override
    public FairSocket websocket(final SocketConsumer consumer, Collection<IExtension> extensions, Collection<IProtocol> protocols) {
       return base.websocket(consumer, extensions, protocols);
    }

    FairCall payloadAppend(final byte[] append) {
        base.payloadAppend(append);

        return this;
    }

    public FairCall multipart(final String filename, final String partName, final MimeType partContentType, final byte[] partBody) {
        base.multipart(filename, partName, partContentType, partBody);

        return this;
    }

    public FairCall timeout(final int timeoutMs) {
        base.timeout(timeoutMs);

        return this;
    }

    void payload(final byte[] bytes) {
        base.payload = bytes;
    }

    public FairCall header(final String name, final String value) {
        base.header(name, value);

        return this;
    }

    public FairCall proxy(final Proxy proxy) {
        base.proxy(proxy);

        return this;
    }

    public FairResponse postAsJson(final Object o) {
        base.payloadAsJson(o);

        return httpCall(Methods.POST);
    }

    public FairResponse putAsJson(final Object o) {
        base.payloadAsJson(o);

        return httpCall(Methods.PUT);
    }

    public FairResponse patchAsJson(final Object o) {
        base.payloadAsJson(o);

        return httpCall(Methods.PATCH);
    }

    public FairCall skipReply(final boolean state) {
        base.skipReply = state;

        return this;
    }
}
