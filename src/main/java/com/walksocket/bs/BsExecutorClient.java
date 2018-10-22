package com.walksocket.bs;

import java.io.IOException;
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
 * @version 0.0.1
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
   * remote.
   * <pre>
   *   for client, remote server.
   * </pre>
   */
  private BsRemote remote;

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
   * constructor.
   * @param callback callback
   * @param local local
   * @param remote remote
   */
  public BsExecutorClient(BsCallback callback, BsLocal local, BsRemote remote) {
    this.callback = callback;
    this.local = local;
    this.remote = remote;
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
              localChannel.receive(buffer);

              // execute callback
              buffer.flip();
              byte[] data = new byte[buffer.limit()];
              buffer.get(data);
              callbackPool.submit(() -> callback.incoming(remote, data));
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
    if (selector == null || !selector.isOpen()) {
      return;
    }

    try {
      local.getReceiveChannel().close();
      selector.close();
    } catch (IOException e) {
      BsLogger.error(e);
    }

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
