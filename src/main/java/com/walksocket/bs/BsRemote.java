package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.UUID;

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
   * life timestamp milliseconds.
   */
  private long lifeTimestampMilliseconds;

  /**
   * idle milliseconds.
   */
  private int idleMilliSeconds = 10000;

  /**
   * constructor.
   * @param remoteHost remote host
   * @param remotePort remote port
   * @param sendChannel send channel
   */
  public BsRemote(String remoteHost, int remotePort, DatagramChannel sendChannel) {
    this(new InetSocketAddress(remoteHost, remotePort), sendChannel);
  }

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
   * is timeout.
   * @return if timeout, true
   */
  boolean isTimeout() {
    return BsDate.timestampMilliseconds() > lifeTimestampMilliseconds;
  }

  /**
   * update timeout.
   */
  void updateTimeout() {
    this.lifeTimestampMilliseconds = BsDate.timestampMilliseconds() + idleMilliSeconds;
  }

  /**
   * get idle milliseconds.
   * @return idle milliseconds
   */
  public int getIdleMilliSeconds() {
    return idleMilliSeconds;
  }

  /**
   * set idle milliseconds.
   * @param idleMilliSeconds idle milliseconds
   */
  public void setIdleMilliSeconds(int idleMilliSeconds) {
    this.idleMilliSeconds = idleMilliSeconds;
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

  @Override
  public String toString() {
    return String.format(
        "remoteAddr:%s, sendChannel:%s",
        remoteAddr,
        sendChannel);
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
