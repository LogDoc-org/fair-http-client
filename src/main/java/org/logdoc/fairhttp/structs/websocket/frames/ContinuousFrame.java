package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

public class ContinuousFrame extends DataFrame {

    public ContinuousFrame() {
        super(Opcode.CONTINUOUS);
    }
}
