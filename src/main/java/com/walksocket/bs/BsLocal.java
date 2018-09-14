package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BsLocal {

  private SocketAddress addr;

  private DatagramChannel receiveChannel;

  private DatagramChannel sendChannel;


  public BsLocal(String host, int port) {
    try {
      addr = new InetSocketAddress(host, port);
      receiveChannel = DatagramChannel.open(StandardProtocolFamily.INET);
      receiveChannel.bind(addr);
      receiveChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      receiveChannel.configureBlocking(false);
      sendChannel = receiveChannel;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void setupSendChannel() {
    try {
      sendChannel = DatagramChannel.open(StandardProtocolFamily.INET);
      sendChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      sendChannel.configureBlocking(false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public SocketAddress getAddr() {
    return addr;
  }

  public DatagramChannel getReceiveChannel() {
    return receiveChannel;
  }

  public DatagramChannel getSendChannel() {
    return sendChannel;
  }
}
