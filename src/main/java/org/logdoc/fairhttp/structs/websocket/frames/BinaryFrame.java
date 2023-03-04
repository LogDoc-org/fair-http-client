package org.logdoc.fairhttp.structs.websocket.frames;


import org.logdoc.fairhttp.structs.websocket.Opcode;

public class BinaryFrame extends DataFrame {

    public BinaryFrame() {
        super(Opcode.BINARY);
    }
}
