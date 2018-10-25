package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;

/**
 * udp local configuration.
 * @author shigenobu
 * @version 0.0.2
 *
 */
public class BsLocal {

  /**
   * binding local address.
   */
  private InetSocketAddress localAddr;

  /**
   * udp receive channel.
   */
  private DatagramChannel receiveChannel;

  /**
   * udp send channel.
   * <pre>
   *   if client, receive channel is equal to send channel.
   *   if server, receive channel is not equal to send channel.
   * </pre>
   */
  private DatagramChannel sendChannel;

  /**
   * constructor.
   * @param host host
   * @param port port
   * @throws BsLocalException local excepiton
   */
  public BsLocal(String host, int port) throws BsLocalException {
    try {
      localAddr = new InetSocketAddress(host, port);
      receiveChannel = DatagramChannel.open(StandardProtocolFamily.INET);
      receiveChannel.bind(localAddr);
      receiveChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      receiveChannel.configureBlocking(false);

      // At first, receive channel is equal to send channel.
      sendChannel = receiveChannel;
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsLocalException(e);
    }
  }

  /**
   * setup send channel called only from BsExecutorServer.
   * <pre>
   *   for server, channels are force to different between receive and send.
   * </pre>
   * @throws BsLocalException local excepiton
   */
  void setupSendChannel() throws BsLocalException {
    try {
      sendChannel = DatagramChannel.open(StandardProtocolFamily.INET);
      sendChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      sendChannel.configureBlocking(false);
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsLocalException(e);
    }
  }

  /**
   * get receive channel.
   * @return receive channel
   */
  DatagramChannel getReceiveChannel() {
    return receiveChannel;
  }

  /**
   * get send channel.
   * @return
   */
  DatagramChannel getSendChannel() {
    return sendChannel;
  }

  /**
   * get local address.
   * @return local address
   */
  InetSocketAddress getLocalAddr() {
    return localAddr;
  }

  /**
   * destroy.
   */
  void destroy() {
    if (receiveChannel != null && receiveChannel.isOpen()) {
      try {
        receiveChannel.close();
      } catch (IOException e) {
        BsLogger.error(e);
      }
    }
    if (sendChannel != null && sendChannel.isOpen()) {
      try {
        sendChannel.close();
      } catch (IOException e) {
        BsLogger.error(e);
      }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "localAddr:%s, receiveChannel:%s, sendChannel:%s",
        localAddr,
        receiveChannel,
        sendChannel);
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
