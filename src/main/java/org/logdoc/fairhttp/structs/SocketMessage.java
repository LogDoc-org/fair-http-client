package org.logdoc.fairhttp.structs;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.01.2023 15:32
 * fairhttp ☭ sweat and blood
 */
public final class SocketMessage {
    public final long indate;

    public final byte[] data;
    public final String text;
    public final JsonNode json;
    public final Document xml;

    public SocketMessage(final byte[] data, final String text, final JsonNode json, final Document xml) {
        this.data = data;
        this.text = text;
        this.json = json;
        this.xml = xml;

        indate = System.currentTimeMillis();
    }

    public boolean hasText() {
        return text != null;
    }

    public boolean hasJson() {
        return json != null;
    }

    public boolean hasXml() {
        return xml != null;
    }
}
