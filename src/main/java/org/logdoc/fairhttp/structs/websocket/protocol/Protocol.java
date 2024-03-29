/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package org.logdoc.fairhttp.structs.websocket.protocol;

import java.util.regex.Pattern;

import static org.logdoc.helpers.Texts.isEmpty;


public class Protocol implements IProtocol {
    private static final Pattern patternSpace = Pattern.compile(" "), patternComma = Pattern.compile(",");

    private final String providedProtocol;

    public Protocol(final String providedProtocol) {
        if (providedProtocol == null)
            throw new IllegalArgumentException();

        this.providedProtocol = providedProtocol;
    }

    @Override
    public boolean acceptProtocol(final String input) {
        if (isEmpty(providedProtocol))
            return true;

        final String[] headers = patternComma.split(patternSpace.matcher(input).replaceAll(""));

        for (String header : headers)
            if (providedProtocol.equals(header))
                return true;

        return false;
    }

    @Override
    public String getProvidedProtocol() {
        return this.providedProtocol;
    }

    @Override
    public String toString() {
        return getProvidedProtocol();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Protocol protocol = (Protocol) o;

        return providedProtocol.equals(protocol.providedProtocol);
    }

    @Override
    public int hashCode() {
        return providedProtocol.hashCode();
    }
}
