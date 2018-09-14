package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class BsRemote {

  public SocketAddress addr;

  private DatagramChannel sendChannel;


  public BsRemote(SocketAddress addr, DatagramChannel sendChannel) {
    this.addr = addr;
    this.sendChannel = sendChannel;
    BsLogger.debug(() -> "addr:" + sendChannel);
  }

  public BsRemote(String host, int port, DatagramChannel sendChannel) {
    addr = new InetSocketAddress(host, port);
    this.sendChannel = sendChannel;
    BsLogger.debug(() -> "host:" + sendChannel);
  }

  public void send(byte[] bytes) {
    try {
      sendChannel.send(ByteBuffer.wrap(bytes), addr);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
