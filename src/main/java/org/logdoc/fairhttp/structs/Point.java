package org.logdoc.fairhttp.structs;

import org.logdoc.fairhttp.structs.traits.Schemas;

import java.net.URI;
import java.net.URL;

import static org.logdoc.fairhttp.utils.Utils.isEmpty;
import static org.logdoc.fairhttp.utils.Utils.notNull;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 13:17
 * fairhttp ☭ sweat and blood
 *
 * userinfo and fragment are not supported
 */
public class Point {
    public final String host;
    public final int port;
    public final String path;
    public final String query;
    public final Schemas schema;

    private URL url;
    private URI uri;
    private Domain domain;

    public Point(final Schemas schema, final String host, final int port, final String path, final String query) {
        if (schema == null || isEmpty(host))
            throw new NullPointerException();

        this.host = host;
        this.port = port <= 0 ? schema.port : port;
        this.path = path;
        this.query = query;
        this.schema = schema;
    }

    public String descriptor() {
        return notNull(path, "/") + (isEmpty(query) ? "" : "?" + query);
    }

    public URL url() {
        if (url == null)
            synchronized (this) {
                try { url = new URL(schema.name(), host.trim(), port <= 0 ? schema.port : port, (isEmpty(path) ? "/" : (path.startsWith("/") ? "" : "/") + path.trim() + (isEmpty(query) ? "" : (query.trim().startsWith("?") ? "" : "?") + query.trim()))); } catch (final Exception ignore) { }
            }

        return url;
    }

    public URI uri() {
        if (uri == null)
            synchronized (this) {
                try { uri = new URI(schema.name(), null, host, port, path, query, null); } catch (final Exception ignore) { }
            }

        return uri;
    }

    public Domain domain() {
        if (domain == null)
            synchronized (this) {
                try { domain = new Domain(host); } catch (final Exception ignore) { }
            }

        return domain;
    }
}
