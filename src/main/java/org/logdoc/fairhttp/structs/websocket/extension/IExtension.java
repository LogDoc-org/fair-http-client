package org.logdoc.fairhttp.structs.websocket.extension;

import org.logdoc.fairhttp.structs.websocket.frames.Frame;

public interface IExtension {

  void decodeFrame(Frame inputFrame) throws ExtensionError;

  void encodeFrame(Frame inputFrame) throws ExtensionError;

  boolean acceptProvidedExtensionAsServer(String inputExtensionHeader);

  boolean acceptProvidedExtensionAsClient(String inputExtensionHeader);

  boolean isFrameValid(Frame inputFrame);

  String getProvidedExtensionAsClient();

  String getProvidedExtensionAsServer();

  IExtension copyInstance();

  void reset();

  String toString();
}
