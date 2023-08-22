package org.logdoc.fairhttp;

import org.logdoc.fairhttp.helpers.FairErrorHandler;
import org.logdoc.fairhttp.flow.FairResponse;
import org.logdoc.fairhttp.flow.FairSocket;
import org.logdoc.fairhttp.structs.Point;
import org.logdoc.fairhttp.helpers.SocketConsumer;
import org.logdoc.fairhttp.structs.traits.Methods;
import org.logdoc.fairhttp.structs.websocket.extension.IExtension;
import org.logdoc.fairhttp.structs.websocket.protocol.IProtocol;
import org.logdoc.helpers.std.MimeType;

import java.net.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 08.01.2023 16:50
 * fairhttp ☭ sweat and blood
 */
public class FairFuture extends Payloads implements FutureSugar {

    private final FairBase base;

    FairFuture(final Point destination) {
        base = new FairBase(destination);
    }

    public FairFuture(final FairBase base) {
        this.base = base;
    }

    public FairCall call() {
        return new FairCall(base);
    }

    public FairFuture contentType(final String contentType) {
        base.contentType(contentType);

        return this;
    }

    public FairFuture contentType(final MimeType contentType) {
        base.contentType(contentType);

        return this;
    }

    public FairFuture withChunksReader(final Consumer<byte[]> chunkReader) {
        base.chunkReader = chunkReader;

        return this;
    }

    public FairFuture errorHandler(final FairErrorHandler errorHandler) {
        base.errorHandler(errorHandler);

        return this;
    }

    public FairFuture incognito() {
        base.incognito();

        return this;
    }

    public FairFuture executor(final Executor executor) {
        base.executor = executor;

        return this;
    }

    public FairFuture withChunksWriter(final Supplier<byte[]> chunksWriter) {
        base.withChunksWriter(chunksWriter);

        return this;
    }

    public FairFuture withtOption(final Fair.Option option, final boolean state) {
        base.option(option, state);

        return this;
    }

    public FairFuture skipReply(final boolean state) {
        base.skipReply = state;

        return this;
    }

    public FairFuture multipart(final String filename, final String partName, final MimeType partContentType, final byte[] partBody) {
        base.multipart(filename, partName, partContentType, partBody);

        return this;
    }

    public FairFuture timeout(final int timeoutMs) {
        base.timeout(timeoutMs);

        return this;
    }

    public FairFuture header(final String name, final String value) {
        base.header(name, value);

        return this;
    }

    public FairFuture proxy(final Proxy proxy) {
        base.proxy(proxy);

        return this;
    }


    public CompletionStage<FairResponse> postAsJson(final Object o) {
        base.payloadAsJson(o);

        return httpCall(Methods.POST);
    }

    public CompletionStage<FairResponse> putAsJson(final Object o) {
        base.payloadAsJson(o);

        return httpCall(Methods.PUT);
    }

    public CompletionStage<FairResponse> patchAsJson(final Object o) {
        base.payloadAsJson(o);

        return httpCall(Methods.PATCH);
    }

    @Override
    public CompletionStage<FairSocket> websocket(final SocketConsumer consumer, Collection<IExtension> extensions, Collection<IProtocol> protocols) {
        if (base.executor != null)
            return CompletableFuture.supplyAsync(() -> base.websocket(consumer, extensions, protocols), base.executor);

        return CompletableFuture.supplyAsync(() -> base.websocket(consumer, extensions, protocols));
    }


    CompletionStage<FairResponse> httpCall(final Methods method) {
        base.method = method;

        if (base.executor != null)
            return CompletableFuture.supplyAsync(base::httpCall, base.executor);

        return CompletableFuture.supplyAsync(base::httpCall);
    }

    void payload(final byte[] bytes) {
        base.payload = bytes;
    }

    FairFuture payloadAppend(final byte[] append) {
        base.payloadAppend(append);

        return this;
    }
}
