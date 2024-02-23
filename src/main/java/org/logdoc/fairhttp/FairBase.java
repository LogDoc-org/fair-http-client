package org.logdoc.fairhttp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.logdoc.fairhttp.diag.CDBuilder;
import org.logdoc.fairhttp.flow.FairResponse;
import org.logdoc.fairhttp.flow.FairSocket;
import org.logdoc.fairhttp.helpers.CookieKeeper;
import org.logdoc.fairhttp.helpers.FairErrorHandler;
import org.logdoc.fairhttp.helpers.SocketConsumer;
import org.logdoc.fairhttp.structs.Point;
import org.logdoc.fairhttp.structs.traits.Headers;
import org.logdoc.fairhttp.structs.traits.Methods;
import org.logdoc.fairhttp.structs.traits.Schemas;
import org.logdoc.fairhttp.structs.websocket.extension.DefaultExtension;
import org.logdoc.fairhttp.structs.websocket.extension.IExtension;
import org.logdoc.fairhttp.structs.websocket.protocol.IProtocol;
import org.logdoc.fairhttp.structs.websocket.protocol.Protocol;
import org.logdoc.helpers.std.MimeType;
import org.logdoc.helpers.std.MimeTypes;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.logdoc.fairhttp.structs.traits.Headers.*;
import static org.logdoc.fairhttp.structs.websocket.protocol.IProtocol.RFC_KEY_UUID;
import static org.logdoc.fairhttp.structs.websocket.protocol.IProtocol.WS_VERSION;
import static org.logdoc.helpers.Bytes.copy;
import static org.logdoc.helpers.Digits.getInt;
import static org.logdoc.helpers.Inets.trustAllManager;
import static org.logdoc.helpers.Sporadics.generateSeed;
import static org.logdoc.helpers.Sporadics.getRnd;
import static org.logdoc.helpers.Texts.isEmpty;
import static org.logdoc.helpers.Texts.notNull;
import static org.logdoc.helpers.std.MimeTypes.BINARY;
import static org.logdoc.helpers.std.MimeTypes.JSON;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 08.01.2023 17:29
 * fairhttp ☭ sweat and blood
 */
class FairBase {
    private static final byte[] FEED = new byte[]{'\r', '\n'};

    final Map<String, String> headers = new HashMap<>();
    Supplier<byte[]> chunksWriter;
    Consumer<byte[]> chunkReader;
    FairErrorHandler errorHandler;
    Executor executor;
    Proxy proxy;
    CookieKeeper cookieKeeper;
    List<MultiPart> multiParts;
    Methods method;
    Point destination;
    boolean followRedirects, allTrusted, skipHostVerify, skipReply;
    int timeout;
    byte[] payload;

    private ObjectMapper om;

    FairBase(final Point destination) {
        this.destination = destination;
        proxy = Fair.commonProxy.get();
        cookieKeeper = Fair.commonKeeper;

        executor = Fair.commonExecutor.get();

        if (!isEmpty(Fair.commonHeaders)) headers.putAll(Fair.commonHeaders);

        errorHandler = Fair.commonHandler.get();
    }

    void contentType(final MimeType contentType) {
        contentType(contentType.toString());
    }

    void contentType(final String contentType) {
        header(Headers.ContentType, contentType);
    }

    FairSocket websocket(final SocketConsumer consumer, Collection<IExtension> extensions, Collection<IProtocol> protocols) {
        final CDBuilder builder = CDBuilder.start(destination.uri().toASCIIString(), Methods.GET.name());
        final FairSocket fairSocket = new FairSocket();
        final Map<String, String> hh = new HashMap<>(0);

        try {
            if (isEmpty(extensions)) extensions = Collections.singletonList(new DefaultExtension());

            if (isEmpty(protocols)) protocols = Collections.singletonList(new Protocol(""));

            final byte[] key = generateSeed(16);

            header(Upgrade, "websocket");
            header(Connection, Upgrade);
            header(Host, destination.host);
            header(SecWebsocketKey, Base64.getEncoder().encodeToString(key));
            header(SecWebsocketVersion, WS_VERSION);
            header(SecWebsocketExtensions, extensions.stream().map(IExtension::getProvidedExtensionAsClient).collect(Collectors.joining(", ")));
            header(SecWebsocketProtocols, protocols.stream().map(IProtocol::getProvidedProtocol).collect(Collectors.joining(", ")));
            loadCookies();

            builder.headers(headers);

            Socket socket = new Socket(proxy);

            if (destination.schema == Schemas.wss) {
                final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, allTrusted ? trustAllManager : null, null);

                socket = sslContext.getSocketFactory().createSocket(socket, destination.host, destination.port, true);
            }

            socket.setTcpNoDelay(false);
            socket.setReuseAddress(false);

            socket.connect(new InetSocketAddress(destination.host, destination.port), timeout);

            final InputStream is = socket.getInputStream();
            final OutputStream os = socket.getOutputStream();

            os.write(("GET " + destination.descriptor() + " HTTP/1.1\r\n").getBytes(StandardCharsets.US_ASCII));
            headers.forEach((k, v) -> {
                try {
                    os.write(safeHeaderRecord(k, v).getBytes(StandardCharsets.US_ASCII));
                } catch (final Exception ignore) {
                }
            });
            os.write(FEED);
            os.flush();
            builder.written(0);

            String line = readHeaderLine(is);

            if (line == null) throw new IllegalStateException("No headers");

            final String[] firstLineTokens = line.split(" ", 3);

            if (firstLineTokens.length != 3) throw new IllegalStateException("Invalid status line: " + line);

            final int code = getInt(firstLineTokens[1]);
            builder.responseStarted(code, firstLineTokens[2]);

            if (code != 101)
                throw new IllegalStateException("Invalid status code received: " + firstLineTokens[1] + " Status line: " + line);

            if (!"HTTP/1.1".equalsIgnoreCase(firstLineTokens[0]))
                throw new IllegalStateException("Invalid status line received: " + firstLineTokens[0] + " Status line: " + line);

            line = readHeaderLine(is);
            while (line != null && !line.isEmpty()) {
                String[] pair = line.split(":", 2);
                if (pair.length != 2) throw new IllegalStateException("not an http header");

                hh.put(notNull(pair[0]), notNull(pair[1]));

                line = readHeaderLine(is);
            }

            builder.responseDone(0, new byte[0], hh, false);

            if (line == null) throw new IllegalStateException("Incomplete handshake");

            if (hh.get(SecWebsocketAccept) == null) throw new IllegalStateException("Missing Sec-WebSocket-Accept");

            if (!hh.get(SecWebsocketAccept).equals(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA1").digest((headers.get(SecWebsocketKey) + RFC_KEY_UUID).getBytes()))))
                throw new IllegalStateException("Wrong key for Sec-WebSocket-Key.");

            final IExtension extension = extensions.stream().filter(e0 -> e0.acceptProvidedExtensionAsClient(hh.get(SecWebsocketExtensions))).findFirst().orElseThrow(() -> new IllegalStateException("No acceptable extension"));

            if (protocols.stream().noneMatch(p0 -> p0.acceptProtocol(hh.get(SecWebsocketProtocols))))
                throw new IllegalStateException("No acceptable protocol");

            socket.setSoTimeout(50);

            fairSocket.init(is, os, consumer, extension, errorHandler);

            if (executor != null) executor.execute(fairSocket);
            else ForkJoinPool.commonPool().execute(fairSocket);
        } catch (final Exception e) {
            errorHandler.exception(e.getMessage(), e, builder.broken(e).data());
        }

        fairSocket.callData = builder.data();
        if (Fair.callTracer.get() != null)
            Fair.callTracer.get().accept(fairSocket.callData.trace());
        return fairSocket;
    }

    void prepareHttpData() throws IOException {
        if (method == null) throw new IllegalArgumentException("Method is not defined");

        if (destination.schema != Schemas.https && destination.schema != Schemas.http)
            throw new IllegalArgumentException("Unknown call protocol");

        if (!isEmpty(multiParts)) {
            final String boundary = "===" + System.currentTimeMillis() + "===";
            contentType(MimeTypes.MULTIPART + "; boundary=" + boundary);

            try (final ByteArrayOutputStream os = new ByteArrayOutputStream(1024 * 128)) {
                for (final MultiPart p : multiParts) {
                    os.write(FEED);
                    os.write(("--" + boundary).getBytes(StandardCharsets.ISO_8859_1));
                    os.write(FEED);
                    os.write((Headers.ContentDisposition + ":form-data;charset=UTF-8;name=\"" + p.partName + "\";" + (isEmpty(p.fileName) ? "" : "filename=\"" + p.fileName + "\"")).getBytes(StandardCharsets.UTF_8));
                    os.write(FEED);
                    os.write((Headers.ContentType + ": " + p.partContentType.toString()).getBytes());
                    os.write(FEED);
                    os.write((ContentLength + ": " + p.part.length).getBytes());
                    os.write(FEED);
                    os.write((Headers.TransferEncoding + ": binary").getBytes());
                    os.write(FEED);
                    os.write(FEED);
                    os.write(p.part);
                    os.write(FEED);
                }

                os.write(FEED);
                os.write(("--" + boundary + "--").getBytes());
                os.write(FEED);

                os.flush();

                payload = os.toByteArray();

                multiParts.clear();
            }
        }

        if (chunksWriter != null) header(Headers.TransferEncoding, "chunked");
        else {
            if (headers.get(ContentType) == null) contentType(BINARY);

            header(Headers.ContentLength, String.valueOf(payload == null ? 0 : payload.length));
        }

        loadCookies();
    }

    void header(final String name0, final String value) {
        final String name = notNull(name0);
        if (!isEmpty(name)) {
            if (isEmpty(value)) headers.remove(name);
            else {
                if (headers.get(name) == null || name.equals(Headers.ContentType) || name.equals(Headers.ContentLength))
                    headers.put(name, notNull(value));
                else headers.merge(name, notNull(value).replaceFirst("^ +", ""), (a, b) -> a + "; " + b);
            }
        }
    }

    void withChunksWriter(final Supplier<byte[]> chunksWriter) {
        contentType(BINARY);
        header(Headers.TransferEncoding, "chunked");

        this.chunksWriter = chunksWriter;
    }

    void errorHandler(final FairErrorHandler errorHandler) {
        if (errorHandler != null) this.errorHandler = errorHandler;
    }

    void incognito() {
        executor = null;
        proxy = Proxy.NO_PROXY;
        cookieKeeper = new CookieKeeper();
        headers.clear();
    }

    void multipart(final String filename, final String partName, final MimeType partContentType, final byte[] partBody) {
        if (multiParts == null) multiParts = new ArrayList<>(1);

        multiParts.add(new MultiPart(partName, partContentType, partBody, filename));
    }

    void proxy(final Proxy proxy) {
        this.proxy = proxy == null ? Proxy.NO_PROXY : proxy;
    }

    public void option(final Fair.Option option, final boolean state) {
        if (option != null) {
            switch (option) {
                case FOLLOW_REDIRECTS:
                    followRedirects = state;
                    break;
                case SSL_DO_NOT_VERIFY_HOSTNAME:
                    skipHostVerify = state;
                    break;
                case SSL_TRUST_ALL_CERTS:
                    allTrusted = state;
                    break;
                case SKIP_RESPONSE:
                    skipReply = state;
                    break;
            }
        }
    }

    void timeout(final int timeoutMs) {
        timeout = Math.max(0, timeoutMs);
    }

    void loadCookies() {
        final String cc = cookieKeeper.load(destination.host, destination.port, destination.path);
        if (!isEmpty(cc)) header(Headers.SendCookies, cc);
    }

    void payloadAppend(final byte[] append) {
        if (isEmpty(payload)) payload = append;
        else {
            final byte[] tmp = new byte[payload.length + append.length + 1];
            System.arraycopy(payload, 0, tmp, 0, payload.length);
            tmp[payload.length] = '&';
            System.arraycopy(append, 0, tmp, payload.length + 1, append.length);

            payload = tmp;
        }
    }

    FairResponse httpCall() {
        final CDBuilder builder = CDBuilder
                .start(destination.url().toExternalForm(), method.name(), headers, payload, chunksWriter != null)
                .options(timeout, followRedirects, allTrusted, skipHostVerify, skipReply);

        FairResponse result = new FairResponse();

        try {
            prepareHttpData();

            final HttpURLConnection huc = (HttpURLConnection) destination.url().openConnection(proxy);
            huc.setDoInput(!skipReply); // read
            huc.setDoOutput(!isEmpty(payload) || chunksWriter != null); // write
            huc.setRequestMethod(method.name());
            huc.setInstanceFollowRedirects(followRedirects);
            huc.setUseCaches(false);

            if (timeout > 0) {
                huc.setConnectTimeout(timeout);
                huc.setReadTimeout(timeout);
            }

            headers.forEach(huc::setRequestProperty);

            if (destination.schema == Schemas.https) {
                if (allTrusted) {
                    final SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllManager, getRnd());
                    ((HttpsURLConnection) huc).setSSLSocketFactory(sslContext.getSocketFactory());
                }

                if (skipHostVerify) ((HttpsURLConnection) huc).setHostnameVerifier((hostname, session) -> true);
            }

            huc.connect();

            long written = 0;
            if (!isEmpty(payload) || chunksWriter != null)
                try (final OutputStream os = huc.getOutputStream()) {
                    if ("chunked".equals(headers.get(Headers.TransferEncoding))) {
                        byte[] chunk;

                        if (chunksWriter != null) while (!isEmpty(chunk = chunksWriter.get())) {
                            os.write(Integer.toHexString(chunk.length).getBytes());
                            os.write(FEED);
                            os.write(chunk);
                            os.write(FEED);
                            os.flush();
                            written += chunk.length;
                        }

                        os.write(0);
                        os.write(FEED);
                        os.write(FEED);
                    } else if (!isEmpty(payload)) {
                        os.write(payload);
                        written = payload.length;
                    }

                    os.flush();
                }

            builder.written(written);

            result.code = huc.getResponseCode();
            result.message = huc.getResponseMessage();

            builder.responseStarted(result.code, result.message);

            String headerName;
            for (int i = 1;  // 0 - это status line (код и сообщение)
                 (headerName = huc.getHeaderFieldKey(i)) != null; i++)
                result.headers.put(headerName, huc.getHeaderField(headerName));

            cookieKeeper.save(huc);

            if (errorHandler.isError(result.code) && errorHandler.breakOnHttpErrors())
                throw errorHandler.throwOnHttpErrors().getDeclaredConstructor(String.class).newInstance("Interrupting on http error response: " + result.code + " [" + notNull(result.message, "no response message") + "]");

            long read = 0;
            final boolean chunked = notNull(huc.getContentEncoding()).equalsIgnoreCase("chunked");

            final InputStream is = result.code >= 400 ? huc.getErrorStream() : huc.getInputStream();

            if (!skipReply)
                try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 64)) {
                    if (chunked) {
                        int chunkLen;

                        if (chunkReader != null) {
                            while ((chunkLen = readChunkLen(is)) > 0) {
                                chunkReader.accept(readChunkBody(chunkLen, is));
                                read += chunkLen;
                            }

                            chunkReader.accept(new byte[0]);
                        } else {
                            while ((chunkLen = readChunkLen(is)) > 0) bos.write(readChunkBody(chunkLen, is));

                            bos.flush();
                            result.body = bos.toByteArray();
                            read = result.body.length;
                        }
                    } else {
                        final long len = huc.getContentLengthLong();

                        if (len > 0) for (long i = 0; i < len; i++)
                            bos.write(is.read());
                        else copy(is, bos);

                        bos.flush();
                        result.body = bos.toByteArray();
                        read = result.body.length;
                    }
                }

            builder.responseDone(read, result.body, result.headers, chunked);
        } catch (final Exception e) {
            errorHandler.exception(e.getMessage(), e, builder.broken(e).data());
        }

        result.callData = builder.data();

        if (Fair.replyVerificator.get() != null)
            Fair.replyVerificator.get().accept(result);

        if (Fair.callTracer.get() != null)
            Fair.callTracer.get().accept(result.callData.trace());

        return result;
    }

    void payloadAsJson(final Object o) {
        contentType(JSON);

        if (o != null) try {
            if (om == null) om = new ObjectMapper();

            payload = om.valueToTree(o).toString().getBytes(StandardCharsets.UTF_8);
        } catch (final Exception ignore) {
        }
    }

    private byte[] readChunkBody(final int size, final InputStream is) throws IOException {
        final byte[] body = new byte[size];

        for (int i = 0; i < size; i++)
            body[i] = (byte) is.read();

        return body;
    }

    private String safeHeaderRecord(final String key, final String value) {
        if (isEmpty(key) || isEmpty(value))
            return "";

        return key + ": " + value + "\r\n";
    }

    private String readHeaderLine(final InputStream is) throws IOException {
        byte b, prev = -1;

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(4096)) {
            while ((b = (byte) is.read()) != -1) {
                if (b == '\n' && prev == '\r')
                    return new String(Arrays.copyOfRange(os.toByteArray(), 0, os.size() - 1), StandardCharsets.US_ASCII);

                os.write(b);
                prev = b;
            }
        }

        return null;
    }

    private int readChunkLen(final InputStream is) throws IOException {
        int b, prev = '0';

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(8)) {
            while ((b = is.read()) != '\n' || prev != '\r') {
                if (b == '\n')
                    os.reset();
                else if (Character.digit(b, 16) != -1)
                    os.write(b);
                prev = b;
            }

            return Integer.parseInt(os.toString(StandardCharsets.US_ASCII), 16);
        }
    }

    private static class MultiPart {
        public final String partName, fileName;
        public final MimeType partContentType;
        public final byte[] part;

        public MultiPart(final String partName, final MimeType partContentType, final byte[] part, final String fileName) {
            this.partName = partName;
            this.fileName = fileName;
            this.partContentType = partContentType;
            this.part = part;
        }
    }
}
