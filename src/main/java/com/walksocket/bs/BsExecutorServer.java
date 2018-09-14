package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BsExecutorServer {

  private BsCallback callback;

  protected Map<DatagramChannel, BsLocal> localMaps;

  private Selector selector;

  private ExecutorService serviceSelect;

  private ExecutorService serviceCallback;

  public BsExecutorServer(BsCallback callback, BsLocal local) {
    this(callback, Arrays.asList(local));
  }

  public BsExecutorServer(BsCallback callback, List<BsLocal> locals) {
    this.callback = callback;
    this.localMaps = new HashMap<>();
    for (BsLocal lcl : locals) {
      lcl.setupSendChannel();
      this.localMaps.put(lcl.getReceiveChannel(), lcl);
    }
  }

  public void start() throws BsExecutorServerException {
    if (selector != null && selector.isOpen()) {
      return;
    }

    try {
      selector = Selector.open();
      for (DatagramChannel channel : localMaps.keySet()) {
        if (!channel.isRegistered()) {
          channel.register(selector, SelectionKey.OP_READ);
        }
      }
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsExecutorServerException(e);
    }

    serviceSelect = Executors.newFixedThreadPool(1);
    serviceCallback = Executors.newWorkStealingPool();
    serviceSelect.submit(() -> {
      while (true) {
        try {
          if (selector.select() > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
              DatagramChannel localChannel = (DatagramChannel) key.channel();
              ByteBuffer buffer = ByteBuffer.allocate(64);
              InetSocketAddress addr = (InetSocketAddress) localChannel.receive(buffer);
//              addr.getAddress().getHostAddress()

              BsLocal local = localMaps.get(localChannel);
              if (local == null) {
                BsLogger.error(localChannel);
                continue;
              }
              BsRemote remote = new BsRemote(addr, local.getSendChannel());
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
      for (DatagramChannel channel : localMaps.keySet()) {
        channel.close();
      }
      selector.close();
    } catch (IOException e) {
      BsLogger.error(e);
    }

    serviceCallback.shutdown();
    serviceSelect.shutdown();
  }

  public class BsExecutorServerException extends Exception {

    private BsExecutorServerException(IOException e) {
      super(e);
    }
  }
}
