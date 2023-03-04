package org.logdoc.fairhttp.structs.websocket.extension;

import org.logdoc.fairhttp.structs.websocket.frames.ControlFrame;
import org.logdoc.fairhttp.structs.websocket.frames.DataFrame;
import org.logdoc.fairhttp.structs.websocket.frames.Frame;

public abstract class CompressionExtension extends DefaultExtension {

    @Override
    public boolean isFrameValid(Frame inputFrame) {
        return ((!(inputFrame instanceof DataFrame)) || (!inputFrame.isRSV2() && !inputFrame.isRSV3())) && ((!(inputFrame instanceof ControlFrame)) || (!inputFrame.isRSV1() && !inputFrame.isRSV2() && !inputFrame.isRSV3()));
    }
}
