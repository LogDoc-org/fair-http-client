package org.logdoc.fairhttp.structs.traits;


import org.logdoc.fairhttp.structs.MimeType;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 13:51
 * fairhttp ☭ sweat and blood
 */
public interface ContentTypes {
    static MimeType asm(final String pair) {
        try {
            return new MimeType(pair);
        } catch (final Exception ignore) {
        }

        return binary;
    }

    MimeType textPlain = asm("text/plain"),
            textHtml = asm("text/html"),
            json = asm("application/json"),
            binary = asm("application/octet-stream"),
            xml = asm("application/xml"),
            form = asm("application/x-www-form-urlencoded"),
            multi = asm("multipart/form-data");
}
