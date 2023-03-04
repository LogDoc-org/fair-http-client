package org.logdoc.fairhttp.structs.websocket.frames;

import org.logdoc.fairhttp.structs.websocket.Opcode;

public class PingFrame extends ControlFrame {

  public PingFrame() {
    super(Opcode.PING);
  }
}
