package org.logdoc.fairhttp;

import com.fasterxml.jackson.databind.JsonNode;
import org.logdoc.fairhttp.flow.FairResponse;
import org.logdoc.fairhttp.flow.FairSocket;
import org.logdoc.fairhttp.structs.MimeType;
import org.logdoc.fairhttp.helpers.SocketConsumer;
import org.logdoc.fairhttp.structs.traits.ContentTypes;
import org.logdoc.fairhttp.structs.traits.Headers;
import org.logdoc.fairhttp.structs.traits.Methods;
import org.logdoc.fairhttp.structs.websocket.extension.DefaultExtension;
import org.logdoc.fairhttp.structs.websocket.extension.IExtension;
import org.logdoc.fairhttp.structs.websocket.protocol.IProtocol;
import org.logdoc.fairhttp.structs.websocket.protocol.Protocol;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import static org.logdoc.helpers.Texts.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 07.01.2023 15:12
 * fairhttp ☭ sweat and blood
 */
public interface CallSugar {
    default FairSocket websocket(final SocketConsumer consumer) {
        return websocket(consumer, Collections.singleton(new DefaultExtension()), Collections.singleton(new Protocol("")));
    }

    default FairCall form(final String fieldName, final Object content) {
        header(Headers.ContentType, ContentTypes.form.toString());

        if (noneEmpty(fieldName, content))
            return ((FairCall) this).payloadAppend(notNull(content).getBytes(StandardCharsets.UTF_8));

        return (FairCall) this;
    }

    default FairCall multipart(final String fieldName, final String content) {
        header(Headers.ContentType, ContentTypes.multi.toString());

        if (noneEmpty(fieldName, content))
            return multipart(null, fieldName, ContentTypes.textPlain, content.getBytes(StandardCharsets.UTF_8));

        return (FairCall) this;
    }

    default FairCall multipart(final String fieldName, final Path file) {
        header(Headers.ContentType, ContentTypes.multi.toString());

        if (!isEmpty(fieldName) && file != null && Files.exists(file)) try {
            return multipart(file.getFileName().toString(), fieldName, ContentTypes.binary, Files.readAllBytes(file));
        } catch (final Exception ignore) {
        }

        return (FairCall) this;
    }

    default FairCall cookie(final String name, final String value) {
        if (noneEmpty(name, value)) header(Headers.SendCookies, name + "=" + value);

        return (FairCall) this;
    }

    default FairCall basicAuth(final String login, final String password) {
        header(Headers.Auth, login == null ? null : Base64.getEncoder().encodeToString((notNull(login) + ":" + notNull(password)).getBytes(StandardCharsets.UTF_8)));

        return (FairCall) this;
    }

    default FairCall basicAuth(final String encodedAuthPayload) {
        header(Headers.Auth, isEmpty(encodedAuthPayload) ? null : encodedAuthPayload);

        return (FairCall) this;
    }

    default FairCall bearerAuth(final String bearerToken) {
        header(Headers.Auth, isEmpty(bearerToken) ? null : bearerToken.trim().toLowerCase().startsWith("bearer ") ? bearerToken : "Bearer " + bearerToken.trim());

        return (FairCall) this;
    }

    default FairResponse patch(final byte[] data) {
        ((Payloads) this).bytes(data);
        return patch();
    }

    default FairResponse patch(final Path file) {
        ((Payloads) this).fileBytes(file);
        return patch();
    }

    default FairResponse patch(final InputStream stream) {
        ((Payloads) this).fromStream(stream);
        return patch();
    }

    default FairResponse patch(final JsonNode json) {
        ((Payloads) this).json(json);
        return patch();
    }

    default FairResponse patch(final Document xml) {
        ((Payloads) this).xml(xml);
        return patch();
    }

    default FairResponse patch(final String text) {
        ((Payloads) this).chars(text);
        return patch();
    }

    default FairResponse patch(final Supplier<byte[]> chunks) {
        withChunksWriter(chunks);
        return patch();
    }

    default FairResponse post(final byte[] data) {
        ((Payloads) this).bytes(data);
        return post();
    }

    default FairResponse post(final Path file) {
        ((Payloads) this).fileBytes(file);
        return post();
    }

    default FairResponse post(final InputStream stream) {
        ((Payloads) this).fromStream(stream);
        return post();
    }

    default FairResponse post(final JsonNode json) {
        ((Payloads) this).json(json);
        return post();
    }

    default FairResponse post(final Document xml) {
        ((Payloads) this).xml(xml);
        return post();
    }

    default FairResponse post(final String text) {
        ((Payloads) this).chars(text);
        return post();
    }

    default FairResponse post(final Supplier<byte[]> chunks) {
        withChunksWriter(chunks);
        return post();
    }

    default FairResponse put(final byte[] data) {
        ((Payloads) this).bytes(data);
        return put();
    }

    default FairResponse put(final Path file) {
        ((Payloads) this).fileBytes(file);
        return put();
    }

    default FairResponse put(final InputStream stream) {
        ((Payloads) this).fromStream(stream);
        return put();
    }

    default FairResponse put(final JsonNode json) {
        ((Payloads) this).json(json);
        return put();
    }

    default FairResponse put(final Document xml) {
        ((Payloads) this).xml(xml);
        return put();
    }

    default FairResponse put(final String text) {
        ((Payloads) this).chars(text);
        return put();
    }

    default FairResponse put(final Supplier<byte[]> chunks) {
        withChunksWriter(chunks);
        return put();
    }

    default FairResponse patch() {
        return ((FairCall) this).httpCall(Methods.PATCH);
    }

    default FairResponse post() {
        return ((FairCall) this).httpCall(Methods.POST);
    }

    default FairResponse put() {
        return ((FairCall) this).httpCall(Methods.PUT);
    }

    default FairResponse get() {
        return ((FairCall) this).httpCall(Methods.GET);
    }

    default FairResponse options() {
        return ((FairCall) this).httpCall(Methods.OPTIONS);
    }

    default FairResponse head() {
        return ((FairCall) this).httpCall(Methods.HEAD);
    }

    default FairResponse trace() {
        return ((FairCall) this).httpCall(Methods.TRACE);
    }

    default FairResponse delete() {
        return ((FairCall) this).httpCall(Methods.DELETE);
    }

    default FairResponse connect() {
        return ((FairCall) this).httpCall(Methods.CONNECT);
    }

    FairSocket websocket(SocketConsumer consumer, Collection<IExtension> extensions, Collection<IProtocol> protocols);

    FairCall header(String name, String value);

    FairCall withChunksWriter(Supplier<byte[]> chunks);

    FairCall multipart(String filename, String partName, MimeType partContentType, byte[] partBody);
}
