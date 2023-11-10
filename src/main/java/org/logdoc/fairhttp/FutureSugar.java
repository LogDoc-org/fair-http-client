package org.logdoc.fairhttp;

import com.fasterxml.jackson.databind.JsonNode;
import org.logdoc.fairhttp.flow.FairResponse;
import org.logdoc.fairhttp.flow.FairSocket;
import org.logdoc.fairhttp.helpers.SocketConsumer;
import org.logdoc.fairhttp.structs.traits.Headers;
import org.logdoc.fairhttp.structs.traits.Methods;
import org.logdoc.fairhttp.structs.websocket.extension.DefaultExtension;
import org.logdoc.fairhttp.structs.websocket.extension.IExtension;
import org.logdoc.fairhttp.structs.websocket.protocol.IProtocol;
import org.logdoc.fairhttp.structs.websocket.protocol.Protocol;
import org.logdoc.helpers.std.MimeType;
import org.logdoc.helpers.std.MimeTypes;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static org.logdoc.helpers.Texts.*;
import static org.logdoc.helpers.std.MimeTypes.BINARY;
import static org.logdoc.helpers.std.MimeTypes.Signs.Multiform;
import static org.logdoc.helpers.std.MimeTypes.TEXTPLAIN;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 08.01.2023 16:52
 * fairhttp ☭ sweat and blood
 */
public interface FutureSugar {
    default CompletionStage<FairSocket> websocket(final SocketConsumer consumer) {
        return websocket(consumer, Collections.singleton(new DefaultExtension()), Collections.singleton(new Protocol("")));
    }

    default FairFuture form(final String fieldName, final Object content) {
        header(Headers.ContentType, MimeTypes.Signs.HttpForm);

        if (noneEmpty(fieldName, content))
            return ((FairFuture) this).payloadAppend((notNull(fieldName) + "=" + URLEncoder.encode(notNull(content), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));

        return (FairFuture) this;
    }

    default FairFuture multipart(final String fieldName, final String content) {
        header(Headers.ContentType, Multiform);

        if (noneEmpty(fieldName, content))
            return multipart(null, fieldName, TEXTPLAIN, content.getBytes(StandardCharsets.UTF_8));

        return (FairFuture) this;
    }

    default FairFuture multipart(final String fieldName, final Path file) {
        header(Headers.ContentType, Multiform);

        if (!isEmpty(fieldName) && file != null && Files.exists(file)) try {
            return multipart(file.getFileName().toString(), fieldName, BINARY, Files.readAllBytes(file));
        } catch (final Exception ignore) {
        }

        return (FairFuture) this;
    }

    default FairFuture cookie(final String name, final String value) {
        if (noneEmpty(name, value)) header(Headers.SendCookies, name + "=" + value);

        return (FairFuture) this;
    }

    default FairFuture basicAuth(final String login, final String password) {
        header(Headers.Auth, login == null ? null : Base64.getEncoder().encodeToString((notNull(login) + ":" + notNull(password)).getBytes(StandardCharsets.UTF_8)));

        return (FairFuture) this;
    }

    default FairFuture basicAuth(final String encodedAuthPayload) {
        header(Headers.Auth, isEmpty(encodedAuthPayload) ? null : encodedAuthPayload);

        return (FairFuture) this;
    }

    default FairFuture bearerAuth(final String bearerToken) {
        header(Headers.Auth, isEmpty(bearerToken) ? null : bearerToken.trim().toLowerCase().startsWith("bearer ") ? bearerToken : "Bearer " + bearerToken.trim());

        return (FairFuture) this;
    }

    default CompletionStage<FairResponse> patch(final byte[] data) {
        ((Payloads) this).bytes(data);
        return patch();
    }

    default CompletionStage<FairResponse> patch(final Path file) {
        ((Payloads) this).fileBytes(file);
        return patch();
    }

    default CompletionStage<FairResponse> patch(final InputStream stream) {
        ((Payloads) this).fromStream(stream);
        return patch();
    }

    default CompletionStage<FairResponse> patch(final JsonNode json) {
        ((Payloads) this).json(json);
        return patch();
    }

    default CompletionStage<FairResponse> patch(final Document xml) {
        ((Payloads) this).xml(xml);
        return patch();
    }

    default CompletionStage<FairResponse> patch(final String text) {
        ((Payloads) this).chars(text);
        return patch();
    }

    default CompletionStage<FairResponse> patch(final Supplier<byte[]> chunks) {
        withChunksWriter(chunks);
        return patch();
    }

    default CompletionStage<FairResponse> post(final byte[] data) {
        ((Payloads) this).bytes(data);
        return post();
    }

    default CompletionStage<FairResponse> post(final Path file) {
        ((Payloads) this).fileBytes(file);
        return post();
    }

    default CompletionStage<FairResponse> post(final InputStream stream) {
        ((Payloads) this).fromStream(stream);
        return post();
    }

    default CompletionStage<FairResponse> post(final JsonNode json) {
        ((Payloads) this).json(json);
        return post();
    }

    default CompletionStage<FairResponse> post(final Document xml) {
        ((Payloads) this).xml(xml);
        return post();
    }

    default CompletionStage<FairResponse> post(final String text) {
        ((Payloads) this).chars(text);
        return post();
    }

    default CompletionStage<FairResponse> post(final Supplier<byte[]> chunks) {
        withChunksWriter(chunks);
        return post();
    }

    default CompletionStage<FairResponse> put(final byte[] data) {
        ((Payloads) this).bytes(data);
        return put();
    }

    default CompletionStage<FairResponse> put(final Path file) {
        ((Payloads) this).fileBytes(file);
        return put();
    }

    default CompletionStage<FairResponse> put(final InputStream stream) {
        ((Payloads) this).fromStream(stream);
        return put();
    }

    default CompletionStage<FairResponse> put(final JsonNode json) {
        ((Payloads) this).json(json);
        return put();
    }

    default CompletionStage<FairResponse> put(final Document xml) {
        ((Payloads) this).xml(xml);
        return put();
    }

    default CompletionStage<FairResponse> put(final String text) {
        ((Payloads) this).chars(text);
        return put();
    }

    default CompletionStage<FairResponse> put(final Supplier<byte[]> chunks) {
        withChunksWriter(chunks);
        return put();
    }

    default CompletionStage<FairResponse> patch() {
        return ((FairFuture) this).httpCall(Methods.PATCH);
    }

    default CompletionStage<FairResponse> post() {
        return ((FairFuture) this).httpCall(Methods.POST);
    }

    default CompletionStage<FairResponse> put() {
        return ((FairFuture) this).httpCall(Methods.PUT);
    }

    default CompletionStage<FairResponse> get() {
        return ((FairFuture) this).httpCall(Methods.GET);
    }

    default CompletionStage<FairResponse> options() {
        return ((FairFuture) this).httpCall(Methods.OPTIONS);
    }

    default CompletionStage<FairResponse> head() {
        return ((FairFuture) this).httpCall(Methods.HEAD);
    }

    default CompletionStage<FairResponse> trace() {
        return ((FairFuture) this).httpCall(Methods.TRACE);
    }

    default CompletionStage<FairResponse> delete() {
        return ((FairFuture) this).httpCall(Methods.DELETE);
    }

    default CompletionStage<FairResponse> connect() {
        return ((FairFuture) this).httpCall(Methods.CONNECT);
    }

    CompletionStage<FairSocket> websocket(SocketConsumer consumer, Collection<IExtension> extensions, Collection<IProtocol> protocols);

    FairFuture header(String name, String value);

    FairFuture withChunksWriter(Supplier<byte[]> chunks);

    FairFuture multipart(String filename, String partName, MimeType partContentType, byte[] partBody);
}
