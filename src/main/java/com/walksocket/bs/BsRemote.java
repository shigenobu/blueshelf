package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * udp remote configuration.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class BsRemote {

  /**
   * sending remote address.
   */
  private InetSocketAddress remoteAddr;

  /**
   * udp send channel.
   */
  private DatagramChannel sendChannel;

  /**
   * constructor.
   * @param remoteAddr remote address
   * @param sendChannel send channel
   */
  public BsRemote(InetSocketAddress remoteAddr, DatagramChannel sendChannel) {
    this.remoteAddr = remoteAddr;
    this.sendChannel = sendChannel;
  }

  /**
   * constructor.
   * @param remoteHost remote host
   * @param remotePort remote port
   * @param sendChannel send channel
   */
  public BsRemote(String remoteHost, int remotePort, DatagramChannel sendChannel) {
    remoteAddr = new InetSocketAddress(remoteHost, remotePort);
    this.sendChannel = sendChannel;
  }

  /**
   * send message.
   * @param bytes message
   * @throws BsSendException send exception
   */
  public void send(byte[] bytes) throws BsSendException {
    try {
      sendChannel.send(ByteBuffer.wrap(bytes), remoteAddr);
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsSendException(e);
    }
  }

  /**
   * get remote address.
   * @return remote address
   */
  public InetSocketAddress getRemoteAddr() {
    return remoteAddr;
  }

  /**
   * get remote host and port.
   * @return host and port
   */
  public String getRemoteHostAndPort() {
    return String.format("%s:%s", remoteAddr.getHostString(), remoteAddr.getPort());
  }

  /**
   * send exception.
   * @author shigenobu
   *
   */
  public class BsSendException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private BsSendException(Throwable e) {
      super(e);
    }
  }
}
