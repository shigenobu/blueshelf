package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * udp client.
 * @author shigenobu
 * @version 0.0.2
 *
 */
public class BsExecutorClient {

  /**
   * receive callback.
   */
  private BsCallback callback;

  /**
   * local.
   * <pre>
   *   for client, listening on single port.
   * </pre>
   */
  private BsLocal local;

  /**
   * read buffer size.
   */
  private int readBufferSize = 1350;

  /**
   * nonblocking channel seletor.
   */
  private Selector selector;

  /**
   * selector pool.
   * <pre>
   *   single thread.
   * </pre>
   */
  private ExecutorService selectorPool = Executors.newFixedThreadPool(1);

  /**
   * callback pool.
   * <pre>
   *   single thread.
   * </pre>
   */
  private ExecutorService callbackPool = Executors.newFixedThreadPool(1);

  /**
   * remote manager.
   */
  private BsRemoteManagerClient manager;

  /**
   * shutdown handler.
   */
  private final BsShutdown shutdown = new BsShutdown();

  /**
   * shutdown thread.
   */
  private final Thread shutdownThread = new Thread(shutdown);

  /**
   * in shutdown, custom executor.
   */
  private BsShutdownExecutor shutdownExecutor;

  /**
   * constructor.
   * @param callback callback
   * @param local local
   * @param remote remote
   */
  public BsExecutorClient(BsCallback callback, BsLocal local, BsRemote remote) {
    this.callback = callback;
    this.local = local;

    // create manager
    manager = new BsRemoteManagerClient(remote);
  }

  /**
   * set read buffer size.
   * @param readBufferSize read buffer size
   * @return this
   */
  public BsExecutorClient readBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
    return this;
  }

  /**
   * set shutdown executor.
   * @param shutdownExecutor shutdown executor
   * @return this
   */
  public BsExecutorClient shutdownExecutor(BsShutdownExecutor shutdownExecutor) {
    this.shutdownExecutor = shutdownExecutor;
    return this;
  }

  /**
   * start.
   * @throws BsExecutorClientException client exception
   */
  public void start() throws BsExecutorClientException {
    if (selector != null && selector.isOpen()) {
      return;
    }

    // open selector
    try {
      selector = Selector.open();
      if (!local.getReceiveChannel().isRegistered()) {
        local.getReceiveChannel().register(selector, SelectionKey.OP_READ);
      }
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsExecutorClientException(e);
    }

    // set shutdown handler
    shutdown.setExecutor(shutdownExecutor);
    Runtime.getRuntime().removeShutdownHook(shutdownThread);
    Runtime.getRuntime().addShutdownHook(shutdownThread);

    // start manager
    manager.startServiceTimeout(callback, shutdown);

    // execution
    selectorPool.submit(() -> {
      while (true) {
        try {
          if (selector.select() > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for(Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
              SelectionKey key = it.next();
              it.remove();

              // receive message
              DatagramChannel localChannel = (DatagramChannel) key.channel();
              ByteBuffer buffer = ByteBuffer.allocate(readBufferSize);
              InetSocketAddress remoteAddr = (InetSocketAddress) localChannel.receive(buffer);
              BsLogger.debug(() -> String.format(
                  "client received from %s:%s",
                  remoteAddr.getHostString(),
                  remoteAddr.getPort()));

              // get remote
              BsRemote remote = manager.get();

              // execute callback
              buffer.flip();
              byte[] data = new byte[buffer.limit()];
              buffer.get(data);
              callbackPool.submit(() -> {
                synchronized (remote) {
                  // if remote is active, invoke incoming
                  if (remote.isActive()) {
                    remote.updateTimeout();
                    callback.incoming(remote, data);
                  }
                }
              });
            }
          }
        } catch (IOException e) {
          BsLogger.error(e);
        }
      }
    });

    // complete client
    StringBuffer buffer = new StringBuffer();
    buffer.append(String.format(
        "%s:%s",
        local.getLocalAddr().getHostString(),
        local.getLocalAddr().getPort()));
    BsLogger.info(String.format(
        "client listen on %s (readBufferSize:%s)",
        buffer.toString(),
        readBufferSize));
  }

  /**
   * shutdown.
   */
  public void shutdown() {
    // close local
    if (local != null) {
      local.destroy();
    }

    // close selector
    if (selector != null && selector.isOpen()) {
      try {
        selector.close();
      } catch (IOException e) {
        BsLogger.error(e);
      }
    }

    // shutdown manager
    if (manager != null) {
      manager.shutdownServiceTimeout();
    }

    // shutdown thread pool
    selectorPool.shutdown();
    callbackPool.shutdown();

    BsLogger.info("client shutdown");
  }

  /**
   * client exception.
   * @author shigenobu
   */
  public class BsExecutorClientException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private BsExecutorClientException(IOException e) {
      super(e);
    }
  }
}
