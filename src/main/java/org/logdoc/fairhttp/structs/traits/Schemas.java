package org.logdoc.fairhttp.structs.traits;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 13:14
 * fairhttp ☭ sweat and blood
 */
public enum Schemas {
    http(80), https(443), ws(80), wss(443);

    public final int port;

    Schemas(final int port) {
        this.port = port;
    }

    public int port() {
        return port;
    }
}
