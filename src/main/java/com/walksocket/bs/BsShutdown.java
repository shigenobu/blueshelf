package com.walksocket.bs;

import java.util.concurrent.atomic.AtomicBoolean;

public class BsShutdown implements Runnable {

  /**
   * in shutdown.
   */
  private final AtomicBoolean inShutdown = new AtomicBoolean(false);

  /**
   * in shutdown, custom executor.
   */
  private BsShutdownExecutor executor;

  /**
   * set custom executor.
   * @param executor custom executor.
   */
  void setExecutor(BsShutdownExecutor executor) {
    this.executor = executor;
  }

  /**
   * in shutdown.
   * @return if running shutdown, true
   */
  boolean inShutdown() {
    return inShutdown.get();
  }

  @Override
  public void run() {
    // start shutdown
    inShutdown.set(true);
    BsLogger.info("start shutdown handler");

    // sleep
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      BsLogger.error(e);
    }

    // execute
    if (executor != null) {
      executor.execute();
    }

    // end shutdown
    BsLogger.info("end shutdown handler");
  }
}
