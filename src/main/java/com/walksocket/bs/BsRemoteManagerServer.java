package com.walksocket.bs;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class BsRemoteManagerServer {

  private int devide;

  private final List<ReentrantLock> locks;

  private final List<ConcurrentHashMap<InetSocketAddress, BsRemote>> remotes;

  /**
   * service timeout.
   */
  private final ScheduledExecutorService serviceTimeout = Executors.newSingleThreadScheduledExecutor();

  /**
   * service no.
   */
  private final AtomicInteger serviceNo = new AtomicInteger(0);

  BsRemoteManagerServer(int devide) {
    this.devide = devide;
    this.locks = new ArrayList<>(devide);
    for (int i = 0; i < devide; i++) {
      this.locks.add(new ReentrantLock());
    }
    this.remotes = new ArrayList<>(devide);
    for (int i = 0; i < devide; i++) {
      this.remotes.add(new ConcurrentHashMap<>());
    }
  }

  private int getMod(InetSocketAddress remoteAddr) {
    int random = System.identityHashCode(remoteAddr);
    return Math.abs(random % devide);
  }

  /**
   * start service timeout.
   * @param callback callback for timeout
   * @param shutdown shutdown executor
   */
  void startServiceTimeout(BsCallback callback, BsShutdown shutdown) {
    int start = 1000 / devide;
    int offset = 1000 / devide;
    serviceTimeout.scheduleAtFixedRate(
        new Runnable() {

          @Override
          public void run() {
            // shutdown
            if (shutdown.inShutdown()) {
              for (int i = 0; i < devide; i++) {
                locks.get(i).lock();
                remotes.get(i).forEach((remoteAddr, remote) -> {
                  synchronized (remote) {
                    callback.shutdown(remote);
                  }
                });
                locks.get(i).unlock();
              }
              return;
            }

            // if remote was timeout, remote is force to timeout
            int no = serviceNo.getAndIncrement();
            if (serviceNo.get() >= devide) {
              serviceNo.set(0);
            }
            locks.get(no).lock();
            remotes.get(no).forEach((remoteAddr, remote) -> {
              synchronized (remote) {
                if (remote.isTimeout()) {
                  callback.timeout(remote);

                  if (remotes.get(no).remove(remoteAddr) != null) {
                    // TODO decrement
                    BsLogger.debug(() -> String.format("removed remote:%s", remote));
                  }
                }
              }
            });
            locks.get(no).unlock();
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

  BsRemote generate(InetSocketAddress remoteAddr, DatagramChannel sendChannel) {
    int mod = getMod(remoteAddr);
    if (!remotes.get(mod).containsKey(remoteAddr)) {
      locks.get(mod).lock();
      if (remotes.get(mod).putIfAbsent(remoteAddr, new BsRemote(remoteAddr, sendChannel)) == null) {
        // TODO increment
        BsLogger.debug(() -> String.format("created remote:%s", remotes.get(mod).get(remoteAddr)));
      }
      locks.get(mod).unlock();
    }
    return remotes.get(mod).get(remoteAddr);
  }

}
