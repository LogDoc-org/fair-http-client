package org.logdoc.fairhttp.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logdoc.fairhttp.diag.CallData;
import org.logdoc.fairhttp.helpers.FairErrorHandler;
import org.logdoc.fairhttp.helpers.SocketConsumer;
import org.logdoc.fairhttp.helpers.Utils;
import org.logdoc.fairhttp.structs.SocketMessage;
import org.logdoc.fairhttp.structs.websocket.Opcode;
import org.logdoc.fairhttp.structs.websocket.extension.DefaultExtension;
import org.logdoc.fairhttp.structs.websocket.extension.IExtension;
import org.logdoc.fairhttp.structs.websocket.frames.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.logdoc.fairhttp.helpers.FairErrorHandler.NotificationLevel.*;
import static org.logdoc.fairhttp.helpers.Utils.byteInt;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 08.01.2023 15:07
 * fairhttp ☭ sweat and blood
 */
public class FairSocket implements Runnable {
    private final AtomicBoolean closing, sending, running;
    private final BlockingQueue<AFrame> queue;
    public CallData callData;

    private ObjectMapper om;
    private DocumentBuilder xb;
    private Transformer tr;
    private Frame incompleteframe;

    private FairErrorHandler errorHandler;
    private SocketConsumer consumer;
    private InputStream is;
    private OutputStream os;
    private IExtension extension;

    {
        running = new AtomicBoolean(false);
        closing = new AtomicBoolean(false);
        sending = new AtomicBoolean(false);
        queue = new ArrayBlockingQueue<>(128);
    }

    public boolean isRunning() {
        return running.get() && !closing.get();
    }

    // public api
    public void send(final JsonNode message) {
        if (message == null)
            throw new NullPointerException("Message");

        if (om == null)
            om = new ObjectMapper();

        try {
            send(om.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void send(final Document message) {
        if (message == null)
            throw new NullPointerException("Message");

        if (tr == null)
            try {
                tr = TransformerFactory.newInstance().newTransformer();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }

        try {
            final StringWriter writer = new StringWriter();
            tr.transform(new DOMSource(message), new StreamResult(writer));
            send(writer.getBuffer().toString().replaceAll("[\n\r]", ""));
        } catch (final TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    public void send(final String message) {
        if (message == null)
            throw new NullPointerException("Message");

        final TextFrame frame = new TextFrame();
        frame.setPayload(message.getBytes(StandardCharsets.UTF_8));
        frame.setMasked(true);

        send(frame);
    }

    public void send(final byte[] message) {
        if (message == null)
            throw new NullPointerException("Message");

        final BinaryFrame frame = new BinaryFrame();
        frame.setMasked(true);
        frame.setPayload(message);

        send(frame);
    }

    public void close() {
        close(CloseFrame.NORMAL, null, false);
    }

    // thread implementation
    public void run() {
        if (!running.compareAndSet(false, true))
            return; // already running

        while (!closing.get())
            try {
                final byte b1 = (byte) is.read();
                final byte b2 = (byte) is.read();
                final Opcode optcode = toOpcode((byte) (b1 & 15));

                final AFrame frame = AFrame.get(optcode);

                frame.setFin(b1 >> 8 != 0);
                frame.setRSV1((b1 & 0x40) != 0);
                frame.setRSV2((b1 & 0x20) != 0);
                frame.setRSV3((b1 & 0x10) != 0);
                final boolean mask = (b2 & -128) != 0;
                int payloadlength = (byte) (b2 & ~(byte) 128);

                if (payloadlength > 125) {
                    if (optcode == Opcode.PING || optcode == Opcode.PONG || optcode == Opcode.CLOSING)
                        throw new IllegalArgumentException("more than 125 octets");

                    if (payloadlength == 126) {
                        byte[] sizebytes = new byte[3];
                        sizebytes[1] = (byte) is.read();
                        sizebytes[2] = (byte) is.read();
                        payloadlength = new BigInteger(sizebytes).intValue();
                    } else {
                        final byte[] bytes = new byte[8];
                        for (int i = 0; i < bytes.length; i++)
                            bytes[i] = (byte) is.read();

                        payloadlength = (int) new BigInteger(bytes).longValue();
                    }
                }

                final byte[] payload = new byte[payloadlength];
                if (mask) {
                    byte[] maskskey = new byte[4];
                    for (int i = 0; i < maskskey.length; i++)
                        maskskey[i] = (byte) is.read();

                    for (int i = 0; i < payloadlength; i++) {
                        payload[i] = ((byte) (is.read() ^ maskskey[i % 4]));
                    }
                } else
                    for (int i = 0; i < payloadlength; i++)
                        payload[i] = ((byte) (is.read()));

                frame.setPayload(payload);

                IExtension ext = null;

                if (frame.getOpcode() != Opcode.CONTINUOUS && (frame.isRSV1() || frame.isRSV2() || frame.isRSV3()))
                    ext = extension;

                if (ext == null)
                    ext = new DefaultExtension();

                if (ext.isFrameValid(frame))
                    ext.decodeFrame(frame);

                if (frame.isValid())
                    process(frame);
                else
                    errorHandler.notification(WARN, "Invalid frame catched: " + frame, callData);
            } catch (final SocketTimeoutException ignore) {
            } catch (final Exception e) {
                if (!closing.get()) // стрим закрылся, итс ок
                    errorHandler.exception(e.getMessage(), e, callData);
            }

        close();
    }

    public void init(final InputStream is, final OutputStream os, final SocketConsumer consumer, final IExtension extension, final FairErrorHandler errorHandler) {
        this.is = is;
        this.os = os;
        this.consumer = consumer;
        this.extension = extension;
        this.errorHandler = errorHandler;
    }

    // private api
    private void send(final AFrame frame) {
        if (frame == null)
            throw new NullPointerException("Frame");

        if (!frame.isValid())
            throw new IllegalStateException("Invalid frame");

        if (!queue.add(frame))
            errorHandler.notification(ERROR, "Cant send message: queue is full", callData);


        if (sending.compareAndSet(false, true))
            CompletableFuture.runAsync(() -> {
                try {
                    Frame framedata;

                    while ((framedata = queue.poll()) != null) {
                        extension.encodeFrame(framedata);

                        final byte[] mes = framedata.getPayloadData();
                        final int sizebytes = getSizeBytes(mes);
                        final byte optcode = fromOpcode(framedata.getOpcode());
                        byte one = (byte) (framedata.isFin() ? -128 : 0);
                        one |= optcode;
                        if (framedata.isRSV1()) one |= getRSVByte(1);
                        if (framedata.isRSV2()) one |= getRSVByte(2);
                        if (framedata.isRSV3()) one |= getRSVByte(3);
                        os.write(one);

                        final byte[] payloadlengthbytes = toByteArray(mes.length, sizebytes);

                        if (sizebytes == 1) {
                            os.write((byte) (payloadlengthbytes[0] | (byte) -128));
                        } else if (sizebytes == 2) {
                            os.write((byte) ((byte) 126 | (byte) -128));
                            os.write(payloadlengthbytes);
                        } else if (sizebytes == 8) {
                            os.write((byte) ((byte) 127 | (byte) -128));
                            os.write(payloadlengthbytes);
                        } else
                            throw new IllegalStateException("Size representation not supported/specified");

                        final byte[] maskkey = byteInt(Utils.rnd.nextInt());

                        for (int i = 0; i < mes.length; i++)
                            os.write((byte) (mes[i] ^ maskkey[i % 4]));

                        os.flush();
                    }
                } catch (final Exception e) {
                    errorHandler.exception("Cant send messages: " + e.getMessage(), e, callData);
                } finally {
                    sending.set(false);
                }
            });
    }

    private int getSizeBytes(final byte[] mes) {
        if (mes.length <= 125)
            return 1;

        if (mes.length <= 65535)
            return 2;

        return 8;
    }

    private byte getRSVByte(int rsv) {
        switch (rsv) {
            case 1: // 0100 0000
                return 0x40;
            case 2: // 0010 0000
                return 0x20;
            case 3: // 0001 0000
                return 0x10;
            default:
                return 0;
        }
    }

    private byte[] toByteArray(long val, int bytecount) {
        byte[] buffer = new byte[bytecount];
        int highest = 8 * bytecount - 8;
        for (int i = 0; i < bytecount; i++) {
            buffer[i] = (byte) (val >>> (highest - 8 * i));
        }
        return buffer;
    }

    private void process(final Frame frame) {
        final Opcode curop = frame.getOpcode();

        if (curop == Opcode.CLOSING) {
            int code = CloseFrame.NOCODE;
            String reason = "";

            if (frame instanceof CloseFrame) {
                code = ((CloseFrame) frame).getCloseCode();
                reason = ((CloseFrame) frame).getMessage();
            }

            close(code, reason, true);
        } else if (curop == Opcode.PING) {
            send(new PongFrame((PingFrame) frame));
            errorHandler.notification(INFO, "Ping received", callData);
        } else if (curop == Opcode.PONG)
            errorHandler.notification(INFO, "Pong received", callData);
        else if (!frame.isFin() || curop == Opcode.CONTINUOUS)
            processFrameContinuousAndNonFin(frame, curop);
        else if (incompleteframe != null)
            throw new IllegalStateException("Continuous frame sequence not completed.");
        else
            frameReady(frame);
    }

    private void frameReady(final Frame frame) {
        final byte[] data = frame.getPayloadData();

        if (frame.getOpcode() == Opcode.TEXT) {
            final String text = new String(data, StandardCharsets.UTF_8).trim();
            JsonNode json = null;
            Document xml = null;

            if (consumer.autoJsonParse() && ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]")))) {
                if (om == null)
                    om = new ObjectMapper();

                try {
                    json = om.readTree(text);
                } catch (final JsonProcessingException e) {
                    errorHandler.exception("Cant parse json from '" + text + "' :: " + e.getMessage(), e, callData);
                }
            }

            if (json == null && consumer.autoXmlParse() && text.toLowerCase().startsWith("<") && text.endsWith(">")) {
                if (xb == null)
                    try {
                        xb = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        errorHandler.exception("Cant create XML parser :: " + e.getMessage(), e, callData);
                    }

                if (xb != null)
                    try {
                        xml = xb.parse(new InputSource(new ByteArrayInputStream(data)));
                    } catch (final Exception e) {
                        errorHandler.exception("Cant parse xml from '" + text + "' :: " + e.getMessage(), e, callData);
                    }
            }

            consumer.onMessage(new SocketMessage(data, json == null && xml == null ? text : null, json, xml));
        } else if (frame.getOpcode() == Opcode.BINARY)
            consumer.onMessage(new SocketMessage(data, null, null, null));
    }

    private void processFrameContinuousAndNonFin(final Frame frame, final Opcode curop) {
        if (curop != Opcode.CONTINUOUS) {
            incompleteframe = frame;
        } else if (frame.isFin()) {
            if (incompleteframe == null)
                throw new IllegalStateException("Continuous frame sequence was not started.");

            incompleteframe.append(frame);

            ((AFrame) incompleteframe).isValid();

            frameReady(incompleteframe);

            incompleteframe = null;
        } else if (incompleteframe == null)
            throw new IllegalStateException("Continuous frame sequence was not started.");

        if (curop == Opcode.CONTINUOUS && incompleteframe != null)
            incompleteframe.append(frame);
    }

    private void close(final int code, final String reason, final boolean remote) {
        if (!closing.compareAndSet(false, true))
            return;

        try {
            consumer.onClose(code, reason, remote);
        } catch (final Exception ignore) {
        }
        try {
            is.close();
        } catch (final Exception ignore) {
        }
        try {
            os.close();
        } catch (final Exception ignore) {
        }
    }

    private Opcode toOpcode(final byte opcode) {
        switch (opcode) {
            case 0:
                return Opcode.CONTINUOUS;
            case 1:
                return Opcode.TEXT;
            case 2:
                return Opcode.BINARY;
            case 8:
                return Opcode.CLOSING;
            case 9:
                return Opcode.PING;
            case 10:
                return Opcode.PONG;
            default:
                throw new IllegalArgumentException("Unknown opcode " + (short) opcode);
        }
    }

    private byte fromOpcode(final Opcode opcode) {
        switch (opcode) {
            case CONTINUOUS:
                return 0;
            case TEXT:
                return 1;
            case BINARY:
                return 2;
            case CLOSING:
                return 8;
            case PING:
                return 9;
            case PONG:
                return 10;
            default:
                throw new IllegalArgumentException("Don't know how to handle " + opcode);
        }
    }
}
