package com.walksocket.bs;

/**
 * received callback.
 * @author shigenobu
 * @version 0.0.2
 *
 */
public interface BsCallback {

  /**
   * incoming.
   * @param remote remote
   * @param message message
   */
  void incoming(BsRemote remote, byte[] message);

  /**
   * timeout.
   * @param remote remote
   */
  default void timeout(BsRemote remote) {
    BsLogger.debug(() -> String.format("default timeout, remote:%s", remote));
  }

  /**
   * shutdown.
   * @param remote remote
   */
  default void shutdown(BsRemote remote) {
    BsLogger.debug(() -> String.format("default shutdown, remote:%s", remote));
  }
}
