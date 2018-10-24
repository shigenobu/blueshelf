package com.walksocket.bs;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class BsRemoteManagerClient {

  private BsRemote remote;

  /**
   * service timeout.
   */
  private final ScheduledExecutorService serviceTimeout = Executors.newSingleThreadScheduledExecutor();

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
                callback.shutdown(remote);
              }
              return;
            }

            // if remote was timeout, remote is force to timeout
            synchronized (remote) {
              if (remote.isTimeout()) {
                callback.timeout(remote);
              }
            }
          }
        }, start, offset, TimeUnit.MILLISECONDS);
  }

  void shutdownServiceTimeout() {
    if (!serviceTimeout.isShutdown()) {
      serviceTimeout.shutdown();
    }
  }

  BsRemote get() {
    return remote;
  }
}
