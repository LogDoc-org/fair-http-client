package org.logdoc.fairhttp.helpers;

import org.logdoc.fairhttp.structs.traits.Headers;
import org.logdoc.fairhttp.utils.Utils;

import java.net.HttpCookie;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.logdoc.fairhttp.utils.Utils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 05.12.2022 17:03
 * fairhttp ☭ sweat and blood
 */
public class CookieKeeper {
    private final Set<HttpCookie> store = ConcurrentHashMap.newKeySet();

    public void save(final URLConnection connection) {
        String headerName;

        for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++)
            if (headerName.equalsIgnoreCase(Headers.GetCookies)) {
                save(notNull(connection.getHeaderField(i)));
                break;
            }
    }

    public void save(final String cookieHeader) {
        store.addAll(HttpCookie.parse(notNull(cookieHeader)));
    }

    public String load(final String host, final int port, final String path) {
        final String coma = Pattern.quote(",");

        return store.stream()
                .filter(c ->
                        !c.hasExpired()
                                && (isEmpty(c.getDomain()) || HttpCookie.domainMatches(c.getDomain(), host))
                                && (isEmpty(c.getPath()) || path.startsWith(c.getPath()))
                                && (isEmpty(c.getPortlist()) || Arrays.stream(c.getPortlist().split(coma)).map(Utils::getInt).collect(Collectors.toSet()).contains(port))
                )
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(Collectors.joining(";"));
    }

    public List<HttpCookie> cookie(final String name) {
        return isEmpty(name) ? Collections.emptyList() :
                store.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(name))
                        .collect(Collectors.toList());
    }
}
