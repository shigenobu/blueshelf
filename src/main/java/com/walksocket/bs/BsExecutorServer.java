package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * udp server.
 * @author shigenobu
 * @version 0.0.1
 *
 */
public class BsExecutorServer {

  /**
   * receive callback.
   */
  private BsCallback callback;

  /**
   * locals.
   * <pre>
   *   for server, listening on multi ports.
   * </pre>
   */
  private Map<DatagramChannel, BsLocal> localMaps;

  /**
   * read buffer size.
   */
  private int readBufferSize = 1350;

  /**
   * remote timeout check devide number.
   */
  private int devide = 10;

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
   *   multi threads.
   * </pre>
   */
  private ExecutorService callbackPool = Executors.newWorkStealingPool();

  /**
   * remote manager.
   */
  private BsRemoteManagerServer manager;

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
  private BsShutdownExecutor executor;

  /**
   * constructor for single port.
   * @param callback callback
   * @param local local instance
   * @throws BsLocal.BsLocalException local exception
   */
  public BsExecutorServer(BsCallback callback, BsLocal local) throws BsLocal.BsLocalException {
    this(callback, Arrays.asList(local));
  }

  /**
   * constructor for multi ports.
   * @param callback callback
   * @param locals local instances
   * @throws BsLocal.BsLocalException local exception
   */
  public BsExecutorServer(BsCallback callback, List<BsLocal> locals) throws BsLocal.BsLocalException {
    this.callback = callback;
    this.localMaps = new HashMap<>();
    // multi port listen
    for (BsLocal lcl : locals) {
      lcl.setupSendChannel();
      this.localMaps.put(lcl.getReceiveChannel(), lcl);
    }
  }

  /**
   * set read buffer size.
   * @param readBufferSize read buffer size
   * @return this
   */
  public BsExecutorServer readBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
    return this;
  }

  /**
   * set timeout check devide number.
   * @param devide timeout check devide number
   * @return this
   */
  public BsExecutorServer devide(int devide) {
    this.devide = devide;
    return this;
  }

  /**
   * set callback pool.
   * @param callbackPool callback pool
   * @return this
   */
  public BsExecutorServer callbackPool(ExecutorService callbackPool) {
    this.callbackPool = callbackPool;
    return this;
  }

  /**
   * start
   * @throws BsExecutorServerException server exception.
   */
  public void start() throws BsExecutorServerException {
    if (selector != null && selector.isOpen()) {
      return;
    }

    // open selector
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

    // set shutdown handler
    shutdown.setExecutor(executor);
    Runtime.getRuntime().removeShutdownHook(shutdownThread);
    Runtime.getRuntime().addShutdownHook(shutdownThread);

    // create manager
    manager = new BsRemoteManagerServer(devide);
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
                  "server received from %s:%s",
                  remoteAddr.getHostString(),
                  remoteAddr.getPort()));

              // confirm which local binding port was received
              BsLocal local = localMaps.get(localChannel);
              if (local == null) {
                BsLogger.error(localChannel);
                continue;
              }
              BsLogger.debug(() -> String.format(
                  "server received port is %s",
                  local.getLocalAddr().getPort()));

              // generate remote
              BsRemote remote = manager.generate(remoteAddr, local.getSendChannel());

              // execute callback
              buffer.flip();
              byte[] data = new byte[buffer.limit()];
              buffer.get(data);
              callbackPool.submit(() -> {
                synchronized (remote) {
                  remote.updateTimeout();
                  callback.incoming(remote, data);
                }
              });
            }
          }
        } catch (IOException e) {
          BsLogger.error(e);
        }
      }
    });

    // complete server
    StringBuffer buffer = new StringBuffer();
    String sep = "";
    for (BsLocal local : localMaps.values()) {
      buffer.append(sep);
      buffer.append(String.format(
          "%s:%s",
          local.getLocalAddr().getHostString(),
          local.getLocalAddr().getPort()));
      sep = ",";
    }
    BsLogger.info(String.format(
        "server listen on %s (readBufferSize:%s, callbackPool:%s)",
        buffer.toString(),
        readBufferSize,
        callbackPool));
  }

  /**
   * shutdown.
   */
  public void shutdown() {
    // close local
    if (localMaps != null) {
      for (BsLocal local : localMaps.values()) {
        local.destory();
      }
    }

    // close selector
    if (selector != null && selector.isOpen()) {
      try {
        selector.close();
      } catch (IOException e) {
        BsLogger.error(e);
      }
    }

    // shutdown manage
    if (manager != null) {
      manager.shutdownServiceTimeout();
    }

    // shutdown thread pool
    callbackPool.shutdown();
    selectorPool.shutdown();

    BsLogger.info("server shutdown");
  }

  /**
   * server exception.
   * @author shigenobu
   */
  public class BsExecutorServerException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private BsExecutorServerException(IOException e) {
      super(e);
    }
  }
}
