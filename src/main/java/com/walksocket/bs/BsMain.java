package com.walksocket.bs;

import java.util.Arrays;
import java.util.List;

public class BsMain {

  public static void main(String args[]) throws BsExecutorServer.BsExecutorServerException {

    // for server
    List<BsLocal> locals4server = Arrays.asList(
        new BsLocal("0.0.0.0", 8711)
//        new BsLocal("0.0.0.0", 8712),
//        new BsLocal("0.0.0.0", 8713)
    );
    BsExecutorServer executorServer = new BsExecutorServer(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        BsLogger.debug(String.format("incoming server: %s", new String(message)));
        remote.send("hello from server by incoming".getBytes());
      }
    }, locals4server);
    executorServer.start();

    // for client
    for (int port : Arrays.asList(8711/*, 8712, 8713*/)) {
      new Thread(() -> {
        BsLocal local4client = new BsLocal("0.0.0.0", (port + 10000));
        BsRemote remote4client = new BsRemote("127.0.0.1", port, local4client.getSendChannel());
//        BsRemote remote4client = new BsRemote(local4client.getAddr(), local4client.getSendChannel());
        BsExecutorClient executorClient = new BsExecutorClient(new BsCallback() {
          @Override
          public void incoming(BsRemote remote, byte[] message) {
            BsLogger.debug(String.format("incoming client: %s", new String(message)));
            remote.send("hello from client by incoming".getBytes());
          }
        }, local4client, remote4client);
        executorClient.start();
        remote4client.send("hello from client".getBytes());
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        executorClient.shutdown();

      }).start();

    }

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    executorServer.shutdown();
  }
}
