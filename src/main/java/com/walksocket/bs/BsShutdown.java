package com.walksocket.bs;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * shutdown handler.
 * @author shigenobu
 * @version 0.0.2
 *
 */
public class BsShutdown implements Runnable {

  /**
   * in shutdown.
   */
  private final AtomicBoolean inShutdown = new AtomicBoolean(false);

  /**
   * in shutdown, custom executor.
   */
  private BsShutdownExecutor shutdownExecutor;

  /**
   * set custom executor.
   * @param shutdownExecutor custom executor.
   */
  void setExecutor(BsShutdownExecutor shutdownExecutor) {
    this.shutdownExecutor = shutdownExecutor;
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
    if (shutdownExecutor != null) {
      shutdownExecutor.execute();
    }

    // end shutdown
    BsLogger.info("end shutdown handler");
  }
}
