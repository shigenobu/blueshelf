package com.walksocket.bs;

import java.util.Arrays;
import java.util.List;

public class BsMain {

  public static void main(String args[]) throws BsExecutorServer.BsExecutorServerException {

    // for server
    List<BsLocal> locals4server = Arrays.asList(
        new BsLocal("0.0.0.0", 8711)
//        new BsLocal("0.0.0.0", 18711),
//        new BsLocal("0.0.0.0", 28711)
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
    for (int port : Arrays.asList(8711/*, 18711, 28711*/)) {
      for (int i = 0; i < 3; i++) {
        int clientPort = port + 10000 + i;
        new Thread(() -> {
          BsLocal local4client = new BsLocal("0.0.0.0", clientPort);
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
          remote4client.send(("hello from client 1 port " + clientPort).getBytes());
          remote4client.send(("hello from client 2 port " + clientPort).getBytes());
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          executorClient.shutdown();

        }).start();

        BsLogger.debug(String.format("client port: %s", clientPort));
      }

    }

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    executorServer.shutdown();
  }
}
