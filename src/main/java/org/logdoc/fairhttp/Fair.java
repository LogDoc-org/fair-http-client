package org.logdoc.fairhttp;

import org.logdoc.fairhttp.diag.CallData;
import org.logdoc.fairhttp.helpers.CookieKeeper;
import org.logdoc.fairhttp.helpers.FairErrorHandler;
import org.logdoc.fairhttp.structs.Point;
import org.logdoc.fairhttp.structs.traits.Schemas;

import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.logdoc.fairhttp.helpers.Utils.isEmpty;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 13:12
 * fairhttp ☭ sweat and blood
 */
public final class Fair {
    public enum Option {FOLLOW_REDIRECTS, SSL_TRUST_ALL_CERTS, SSL_DO_NOT_VERIFY_HOSTNAME, SKIP_RESPONSE}

    static final AtomicReference<Consumer<String>> callTracer = new AtomicReference<>(null);
    static final CookieKeeper commonKeeper = new CookieKeeper();
    static final Map<String, String> commonHeaders = new HashMap<>(0);
    static final AtomicReference<Proxy> commonProxy = new AtomicReference<>(Proxy.NO_PROXY);
    static final AtomicReference<Executor> commonExecutor = new AtomicReference<>(null);
    static final AtomicReference<FairErrorHandler> commonHandler = new AtomicReference<>(new FairErrorHandler() {
        @Override
        public void notification(final NotificationLevel level, final String notification, final CallData callData) {
            System.out.println("FairHttp #" + callData.id + " :: " + level + " :: " + notification);
            System.out.println(callData.trace());
        }

        @Override
        public void exception(final String details, final Throwable t, final CallData callData) {
            System.out.println("FairHttp #" + callData.id + " :: " + NotificationLevel.ERROR + " :: " + details + " :: " + t.getMessage());
            t.printStackTrace();
            System.out.println(callData.trace());
        }
    });

    private static FairCall build(final Point point) {
        return point == null ? null : new FairCall(point);
    }

    public static FairCall url(final String url) {
        return build(PointFactory.point(url));
    }

    public static FairCall url(final URL url) {
        return build(PointFactory.point(url));
    }

    public static FairCall url(final URI uri) {
        return build(PointFactory.point(uri));
    }

    private static class PointFactory {
        private static Point point(final String url) {
            try {
                return point(new URL(url));
            } catch (final Exception ignore) {
            }

            return null;
        }

        private static Point point(final URL url) {
            try {
                return new Point(
                        Schemas.valueOf(url.getProtocol().toLowerCase().trim()),
                        url.getHost(),
                        url.getPort(),
                        url.getPath(),
                        url.getQuery()
                );
            } catch (final Exception ignore) {
            }

            return null;
        }

        private static Point point(final URI uri) {
            try {
                return new Point(
                        Schemas.valueOf(uri.getScheme().toLowerCase().trim()),
                        uri.getHost(),
                        uri.getPort(),
                        uri.getPath(),
                        uri.getQuery()
                );
            } catch (final Exception ignore) {
            }
            return null;
        }

    }

    public static void commonExecutor(final Executor executor) {
        commonExecutor.set(executor);
    }

    public static void commonProxy(final Proxy proxy) {
        commonProxy.set(proxy == null ? Proxy.NO_PROXY : proxy);
    }

    public static String commonHeader(final String name) {
        return name == null ? null : commonHeaders.get(name);
    }

    public static void commonHeader(final String name, final String value) {
        if (isEmpty(name))
            return;

        if (isEmpty(value))
            commonHeaders.remove(name);
        else
            commonHeaders.put(name, value);
    }

    public static void commonErrorHandler(final FairErrorHandler handler) {
        if (handler != null)
            commonHandler.set(handler);
    }

    public static void eachCallTraceConsumer(final Consumer<String> traceConsumer) {
        callTracer.set(traceConsumer);
    }
}
