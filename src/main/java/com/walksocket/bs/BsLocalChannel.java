package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;

/**
 * udp local channel.
 * @author shigenobu
 * @version 0.0.5
 *
 */
public class BsLocalChannel {

  /**
   * binding local address.
   */
  private InetSocketAddress localAddr;

  /**
   * udp receive and send channel.
   */
  private DatagramChannel channel;

  /**
   * constructor.
   * @param host host
   * @param port port
   * @throws IOException selector exception
   */
  BsLocalChannel(String host, int port) throws IOException {
    localAddr = new InetSocketAddress(host, port);
    channel = DatagramChannel.open(StandardProtocolFamily.INET);
    channel.bind(localAddr);
    channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    channel.configureBlocking(false);
  }

  /**
   * get local address.
   * @return local address
   */
  InetSocketAddress getLocalAddr() {
    return localAddr;
  }

  /**
   * get channel.
   * @return channel
   */
  DatagramChannel getChannel() {
    return channel;
  }

  /**
   * destroy.
   */
  void destroy() {
    if (channel != null && channel.isOpen()) {
      try {
        channel.close();
      } catch (IOException e) {
        BsLogger.error(e);
      }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "localAddr:%s, channel:%s",
        localAddr,
        channel);
  }
}
