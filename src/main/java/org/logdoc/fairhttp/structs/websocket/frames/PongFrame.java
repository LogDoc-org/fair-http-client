package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

public class PongFrame extends ControlFrame {

    public PongFrame() {
        super(Opcode.PONG);
    }

    public PongFrame(final PingFrame pingFrame) {
        super(Opcode.PONG);
        setPayload(pingFrame.getPayloadData());
    }
}
