package org.logdoc.fairhttp.structs;

import java.util.Objects;

import static org.logdoc.fairhttp.helpers.Utils.*;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 06.12.2022 14:04
 * fairhttp ☭ sweat and blood
 */
public class Domain implements Comparable<Domain> {
    public final String domain;
    public final int level;

    public Domain(final String domain) {
        if (isEmpty(domain))
            throw new IllegalArgumentException();

        this.domain = domain.toLowerCase().trim().replace(".*", "");

        if (isIPv4LiteralAddress(this.domain) || isIPv6LiteralAddress(this.domain))
            level = -1;
        else {
            int l = 1;
            for (int i = 0; i < this.domain.length(); i++)
                if (this.domain.charAt(i) == '.')
                    l++;

            level = l;
        }
    }

    public boolean related(final String host) {
        if (isEmpty(host))
            return false;

        if (domain.equals(host))
            return true;

        return related(new Domain(host));
    }

    public boolean related(final Domain other) {
        if (other == null)
            return false;

        if (level > other.level)
            return other.related(ancestor());

        if (level == other.level)
            return domain.equals(other.domain) || (level > 1 && ancestor().related(other.ancestor()));

        Domain tmp = other;

        while (tmp != null && tmp.level > level)
            tmp = tmp.ancestor();

        return tmp != null && (domain.equals(other.domain) || (level > 1 && ancestor().related(other.ancestor())));
    }

    public Domain ancestor() {
        return level > 1
                ? new Domain(domain.substring(domain.indexOf('.') + 1))
                : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Domain domain1 = (Domain) o;
        return domain.equals(domain1.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain);
    }

    @Override
    public int compareTo(final Domain o) {
        return domain.compareTo(o.domain);
    }

    public Domain treeTop() {
        if (level < 2)
            return null;

        if (level == 2)
            return this;

        return ancestor().treeTop();
    }
}
