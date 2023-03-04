package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

public interface Frame {

    boolean isFin();

    boolean isRSV1();

    boolean isRSV2();

    boolean isRSV3();

    boolean getTransfereMasked();

    Opcode getOpcode();

    byte[] getPayloadData();

    void append(Frame nextframe);
}
