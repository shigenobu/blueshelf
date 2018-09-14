package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BsExecutorClient {

  private BsCallback callback;

  private BsLocal local;

  private BsRemote remote;

  private Selector selector;

  private ExecutorService serviceSelect;

  private ExecutorService serviceCallback;

  public BsExecutorClient(BsCallback callback, BsLocal local, BsRemote remote) {
    this.callback = callback;
    this.local = local;
    this.remote = remote;
  }


  public void start() {
    if (selector != null && selector.isOpen()) {
      return;
    }

    try {
      selector = Selector.open();
      if (!local.getReceiveChannel().isRegistered()) {
        local.getReceiveChannel().register(selector, SelectionKey.OP_READ);
      }
    } catch (IOException e) {
      BsLogger.error(e);
//      throw new BsExecutorServer.BsExecutorServerException(e);
    }

    serviceSelect = Executors.newFixedThreadPool(1);
    serviceCallback = Executors.newFixedThreadPool(1);
    serviceSelect.submit(() -> {
      while (true) {
        try {
          if (selector.select() > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
              DatagramChannel localChannel = (DatagramChannel) key.channel();
              ByteBuffer buffer = ByteBuffer.allocate(64);
              localChannel.receive(buffer);
//              InetSocketAddress addr = (InetSocketAddressketAddress) localChannel.receive(buffer);
//              addr.getAddress().getHostAddress()

//              BsRemote remote = new BsRemote(addr, local.getSendChannel());
//              remote.setLocal(local);
              serviceCallback.submit(() -> callback.incoming(remote, buffer.array()));
            }
          }
        } catch (IOException e) {
          BsLogger.error(e);
        }
      }
    });
  }

  public void shutdown() {
    if (selector == null || !selector.isOpen()) {
      return;
    }

    try {
      local.getReceiveChannel().close();
      selector.close();
    } catch (IOException e) {
      BsLogger.error(e);
    }

    serviceCallback.shutdown();
    serviceSelect.shutdown();

    BsLogger.info("client shutdown");
  }
}
