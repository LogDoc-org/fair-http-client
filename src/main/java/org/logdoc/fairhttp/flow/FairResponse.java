package org.logdoc.fairhttp.flow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logdoc.fairhttp.diag.CallData;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 08.01.2023 19:03
 * fairhttp ☭ sweat and blood
 */
public final class FairResponse {
    public int code;
    public String message;
    public byte[] body;

    public final Map<String, String> headers = new HashMap<>();
    public CallData callData;

    public String asString() {
        if (body == null)
            return null;

        return new String(body, StandardCharsets.UTF_8);
    }

    public JsonNode asJson() {
        if (body != null)
            try {
                return new ObjectMapper().readTree(body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        return null;
    }

    public Document asXml() {
        if (body != null)
            try {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new ByteArrayInputStream(body)));
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

        return null;
    }

    public <T> T fromJson(final Class<T> cls) {
        if (body == null)
            return null;

        final ObjectMapper m = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            return m.treeToValue(m.readTree(body), cls);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
