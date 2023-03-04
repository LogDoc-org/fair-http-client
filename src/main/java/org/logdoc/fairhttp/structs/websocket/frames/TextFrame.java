package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

import static org.logdoc.fairhttp.helpers.Utils.isValidUTF8;

public class TextFrame extends DataFrame {

    public TextFrame() {
        super(Opcode.TEXT);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && isValidUTF8(getPayloadData());
    }
}
