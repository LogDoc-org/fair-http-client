package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

public abstract class DataFrame extends AFrame {

    public DataFrame(final Opcode opcode) {
        super(opcode);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
