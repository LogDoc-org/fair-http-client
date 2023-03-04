package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

public abstract class ControlFrame extends AFrame {

    public ControlFrame(final Opcode opcode) {
        super(opcode);
    }

    @Override
    public boolean isValid() {
        return isFin() && !isRSV1() && !isRSV2() && !isRSV3();
    }
}
