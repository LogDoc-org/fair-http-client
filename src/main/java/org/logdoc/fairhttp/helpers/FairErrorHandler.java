package org.logdoc.fairhttp.helpers;

import org.logdoc.fairhttp.diag.CallData;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 07.01.2023 17:46
 * fairhttp ☭ sweat and blood
 */
public interface FairErrorHandler {
    default boolean breakOnHttpErrors() {
        return false;
    }

    default Class<? extends RuntimeException> throwOnHttpErrors() {
        return IllegalStateException.class;
    }

    default boolean isError(final int httpStatusCode) {
        return httpStatusCode <= 0 || httpStatusCode >= 400;
    }

    void notification(NotificationLevel level, String notification, CallData callData);

    void exception(String details, Throwable t, CallData callData);

    enum NotificationLevel {
        INFO, WARN, ERROR
    }
}
