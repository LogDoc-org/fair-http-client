package org.logdoc.fairhttp.structs.traits;


import org.logdoc.helpers.std.MimeType;

import static org.logdoc.helpers.std.MimeTypes.BINARY;

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

        return BINARY;
    }
}
