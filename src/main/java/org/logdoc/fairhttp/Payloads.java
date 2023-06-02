package org.logdoc.fairhttp;

import com.fasterxml.jackson.databind.JsonNode;
import org.logdoc.fairhttp.structs.traits.ContentTypes;
import org.logdoc.fairhttp.structs.traits.Headers;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.logdoc.helpers.Bytes.copy;
import static org.logdoc.helpers.Xmls.xml2StringBytes;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 08.01.2023 18:10
 * fairhttp ☭ sweat and blood
 */
abstract class Payloads {
    void json(JsonNode json) {
        header(Headers.ContentType, ContentTypes.json.toString());

        if (json != null) this.payload(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    void chars(String text) {
        header(Headers.ContentType, ContentTypes.textPlain.toString());

        if (text != null) this.payload(text.getBytes(StandardCharsets.UTF_8));
    }

    void xml(final Document xml) {
        header(Headers.ContentType, ContentTypes.xml.toString());

        if (xml != null)
            this.payload(xml2StringBytes(xml));
    }

    void fromStream(final InputStream stream) {
        header(Headers.ContentType, ContentTypes.binary.toString());

        if (stream != null)
            try (final ByteArrayOutputStream os = new ByteArrayOutputStream(1024 * 16)) {
                copy(stream, os);
                os.flush();

                this.payload(os.toByteArray());
            } catch (final Exception ignore) {
            }
    }

    void bytes(byte[] data) {
        header(Headers.ContentType, ContentTypes.binary.toString());

        if (data != null) this.payload(data);
    }

    void fileBytes(final Path file) {
        header(Headers.ContentType, ContentTypes.binary.toString());

        if (file != null && Files.exists(file))
            try {
                this.payload(Files.readAllBytes(file));
            } catch (final Exception ignore) {
            }
    }

    abstract Payloads header(String name, String value);
    abstract void payload(byte[] bytes);
}
