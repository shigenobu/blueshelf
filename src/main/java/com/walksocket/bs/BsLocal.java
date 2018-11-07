package com.walksocket.bs;

import java.io.IOException;

/**
 * udp local configuration.
 * @author shigenobu
 * @version 0.0.5
 *
 */
public class BsLocal {

  /**
   * udp local channel.
   */
  private BsLocalChannel localChannel;

  /**
   * constructor.
   * @param host host
   * @param port port
   * @throws BsLocalException local excepiton
   */
  public BsLocal(String host, int port) throws BsLocalException {
    try {
      localChannel = new BsLocalChannel(host, port);
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsLocalException(e);
    }
  }

  /**
   * get local channel.
   * @return local channel.
   */
  BsLocalChannel getLocalChannel() {
    return localChannel;
  }

  @Override
  public String toString() {
    if (localChannel == null) {
      return "";
    }
    return localChannel.toString();
  }

  /**
   * local exception.
   * @author shigenobu
   */
  public class BsLocalException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private BsLocalException(Throwable e) {
      super(e);
    }
  }
}
