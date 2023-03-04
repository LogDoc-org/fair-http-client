package org.logdoc.fairhttp.helpers;

import org.logdoc.fairhttp.structs.SocketMessage;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.01.2023 15:47
 * fairhttp ☭ sweat and blood
 */
public interface SocketConsumer {
    void onMessage(SocketMessage message);
    default void onClose(int code, String reason, boolean remote) {}

    default boolean autoJsonParse() {
        return false;
    }

    default boolean autoXmlParse() {
        return false;
    }
}
