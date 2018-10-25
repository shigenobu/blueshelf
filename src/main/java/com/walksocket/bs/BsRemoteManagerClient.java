package com.walksocket.bs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * remote manager for client.
 * @author shigenobu
 * @version 0.0.2
 *
 */
public class BsRemoteManagerClient {

  /**
   * remote.
   * <pre>
   *   Fixed, to server.
   * </pre>
   */
  private BsRemote remote;

  /**
   * service timeout.
   */
  private final ScheduledExecutorService serviceTimeout = Executors.newSingleThreadScheduledExecutor();

  /**
   * constructor.
   * @param remote remote
   */
  BsRemoteManagerClient(BsRemote remote) {
    this.remote = remote;
  }

  /**
   * start service timeout.
   * @param callback callback for timeout
   * @param shutdown shutdown executor
   */
  void startServiceTimeout(BsCallback callback, BsShutdown shutdown) {
    int start = 1000;
    int offset = 1000;
    serviceTimeout.scheduleAtFixedRate(
        new Runnable() {

          @Override
          public void run() {
            // shutdown
            if (shutdown.inShutdown()) {
              synchronized (remote) {
                // if active, invoke shutdown.
                if (remote.isActive()) {
                  remote.setActive(false);
                  callback.shutdown(remote);
                }
              }
              return;
            }

            // timeout
            synchronized (remote) {
              // if already timeout and active, invoke timeout.
              if (remote.isTimeout() && remote.isActive()) {
                remote.setActive(false);
                callback.timeout(remote);
              }
            }
          }
        }, start, offset, TimeUnit.MILLISECONDS);
  }

  /**
   * shutdown service timeout.
   */
  void shutdownServiceTimeout() {
    if (!serviceTimeout.isShutdown()) {
      serviceTimeout.shutdown();
    }
  }

  /**
   * get remote.
   * @return remote.
   */
  BsRemote get() {
    return remote;
  }
}
