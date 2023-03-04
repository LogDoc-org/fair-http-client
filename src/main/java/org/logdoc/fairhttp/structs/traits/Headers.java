package org.logdoc.fairhttp.structs.traits;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 16:22
 * fairhttp ☭ sweat and blood
 */
public interface Headers {
    String SendCookies = "Cookie",
            GetCookies = "Set-Cookie",
            ContentType = "Content-Type",
            ContentLength = "Content-Length",
            TransferEncoding = "Transfer-Encoding",
            ContentDisposition = "Content-disposition",
            Auth = "Authorization",
            Encoding = "Content-Encoding",
            Upgrade = "Upgrade",
            Connection = "Connection",
            Host = "Host",
            SecWebsocketKey = "Sec-WebSocket-Key",
            SecWebsocketVersion = "Sec-WebSocket-Version",
            SecWebsocketExtensions = "Sec-WebSocket-Extensions",
            SecWebsocketProtocols = "Sec-WebSocket-Protocol",
            SecWebsocketAccept = "Sec-WebSocket-Accept";
}
