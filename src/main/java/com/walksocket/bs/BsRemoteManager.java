package com.walksocket.bs;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * remote manager for server.
 * @author shigenobu
 * @version 0.0.5
 *
 */
class BsRemoteManager {

  /**
   * remote timeout check devide number.
   */
  private int devide;

  /**
   * callback.
   */
  private BsCallback callback;

  /**
   * shutdown handler.
   */
  private BsShutdown shutdown;

  /**
   * remote locks.
   */
  private final List<ReentrantLock> locks;

  /**
   * remotes.
   */
  private final List<ConcurrentHashMap<InetSocketAddress, BsRemote>> remotes;

  /**
   * service timeout.
   */
  private final ScheduledExecutorService serviceTimeout = Executors.newSingleThreadScheduledExecutor();

  /**
   * service no.
   */
  private final AtomicInteger serviceNo = new AtomicInteger(0);

  /**
   * remote count.
   */
  private final AtomicLong remoteCount = new AtomicLong(0);

  /**
   * constructor.
   * @param devide remote timeout check devide number
   * @param callback callback for timeout
   * @param shutdown shutdown executor
   */
  BsRemoteManager(int devide, BsCallback callback, BsShutdown shutdown) {
    this.devide = devide;
    this.callback = callback;
    this.shutdown = shutdown;
    this.locks = new ArrayList<>(devide);
    for (int i = 0; i < devide; i++) {
      this.locks.add(new ReentrantLock());
    }
    this.remotes = new ArrayList<>(devide);
    for (int i = 0; i < devide; i++) {
      this.remotes.add(new ConcurrentHashMap<>());
    }
  }

  /**
   * get mod.
   * @param remoteAddr remote addr
   * @return mod
   */
  private int getMod(InetSocketAddress remoteAddr) {
    int random = System.identityHashCode(remoteAddr);
    return Math.abs(random % devide);
  }

  /**
   * start service timeout.
   */
  void startServiceTimeout() {
    int start = 1000 / devide;
    int offset = 1000 / devide;
    serviceTimeout.scheduleAtFixedRate(
        new Runnable() {

          @Override
          public void run() {
            // shutdown
            if (shutdown.inShutdown()) {
              for (int i = 0; i < devide; i++) {
                final int no = i;
                locks.get(no).lock();
                remotes.get(no).forEach((remoteAddr, remote) -> {
                  synchronized (remote) {
                    // if active, invoke shutdown.
                    if (remote.isActive()) {
                      remote.setActive(false);
                      callback.shutdown(remote);
                    }
                    if (remotes.get(no).remove(remoteAddr) != null) {
                      // decrement
                      remoteCount.decrementAndGet();
                      BsLogger.debug(() -> String.format("By force shutdown, removed remote:%s", remote));
                    }
                  }
                });
                locks.get(no).unlock();
              }
              return;
            }

            // timeout
            int no = serviceNo.getAndIncrement();
            if (serviceNo.get() >= devide) {
              serviceNo.set(0);
            }
            locks.get(no).lock();
            remotes.get(no).forEach((remoteAddr, remote) -> {
              synchronized (remote) {
                // if already timeout and active, invoke timeout.
                if (remote.isTimeout() && remote.isActive()) {
                  remote.setActive(false);
                  callback.timeout(remote);

                  if (remotes.get(no).remove(remoteAddr) != null) {
                    // decrement
                    remoteCount.decrementAndGet();
                    BsLogger.debug(() -> String.format("By timeout, removed remote:%s", remote));
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
      for (int i = 0; i < devide; i++) {
        final int no = i;
        locks.get(no).lock();
        remotes.get(no).forEach((remoteAddr, remote) -> {
          synchronized (remote) {
            // if active, invoke shutdown.
            if (remote.isActive()) {
              remote.setActive(false);
              callback.shutdown(remote);
            }
            if (remotes.get(no).remove(remoteAddr) != null) {
              // decrement
              remoteCount.decrementAndGet();
              BsLogger.debug(() -> String.format("By normal shutdown, removed remote:%s", remote));
            }
          }
        });
        locks.get(no).unlock();
      }
      serviceTimeout.shutdown();
    }
  }

  /**
   * register remote for client.
   * @param remote remote
   */
  void register(BsRemote remote) {
    InetSocketAddress remoteAddr = remote.getRemoteAddr();
    int mod = getMod(remoteAddr);
    if (!remotes.get(mod).containsKey(remoteAddr)) {
      locks.get(mod).lock();
      if (remotes.get(mod).putIfAbsent(remoteAddr, remote) == null) {
        BsLogger.debug(() -> String.format("register remote:%s", remotes.get(mod).get(remoteAddr)));
      }
      locks.get(mod).unlock();
    }
  }

  /**
   * generate remote.
   * @param remoteAddr remote addr
   * @param localChannel local channel
   * @return remote
   */
  BsRemote generate(InetSocketAddress remoteAddr, BsLocalChannel localChannel) {
    int mod = getMod(remoteAddr);
    if (!remotes.get(mod).containsKey(remoteAddr)) {
      locks.get(mod).lock();
      if (remotes.get(mod).putIfAbsent(remoteAddr, new BsRemote(remoteAddr, localChannel)) == null) {
        // increment
        remoteCount.incrementAndGet();
        BsLogger.debug(() -> String.format("generate remote:%s", remotes.get(mod).get(remoteAddr)));
      }
      locks.get(mod).unlock();
    }
    return remotes.get(mod).get(remoteAddr);
  }

  /**
   * get remote count.
   * @return remote count
   */
  long getRemoteCount() {
    return remoteCount.get();
  }
}
