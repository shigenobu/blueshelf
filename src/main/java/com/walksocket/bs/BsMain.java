package com.walksocket.bs;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class BsMain {

  public static void main(String args[]) throws BsExecutorServer.BsExecutorServerException, BsLocal.BsLocalException {

    BsLogger.setVerbose(true);

    // for server
    List<BsLocal> locals4server = Arrays.asList(
        new BsLocal("0.0.0.0", 8711),
        new BsLocal("0.0.0.0", 8712)
//        new BsLocal("0.0.0.0", 28711)
    );
    BsExecutorServer executorServer = new BsExecutorServer(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        BsLogger.debug(String.format("incoming server: %s (%s:%s)",
            new String(message, StandardCharsets.UTF_8),
            remote.getRemoteAddr().getHostString(),
            remote.getRemoteAddr().getPort()));
        try {
          remote.send("hello from server by incoming".getBytes(StandardCharsets.UTF_8));
        } catch (BsRemote.BsSendException e) {
          e.printStackTrace();
        }
      }
    }, locals4server);
    executorServer.start();

    // for client
    for (int port : Arrays.asList(8711/*, 18711, 28711*/)) {
      for (int i = 0; i < 2; i++) {
        int clientPort = port + 10000 + i;
        new Thread(() -> {
          BsLocal local4client = null;
          try {
            local4client = new BsLocal("0.0.0.0", clientPort);
          } catch (BsLocal.BsLocalException e) {
            e.printStackTrace();
          }
          BsRemote remote4client = new BsRemote("127.0.0.1", port, local4client.getSendChannel());
//        BsRemote remote4client = new BsRemote(local4client.getAddr(), local4client.getSendChannel());
          BsExecutorClient executorClient = new BsExecutorClient(new BsCallback() {
            @Override
            public void incoming(BsRemote remote, byte[] message) {
              BsLogger.debug(String.format("incoming client: %s (%s:%s)",
                  new String(message, StandardCharsets.UTF_8),
                  remote.getRemoteAddr().getHostString(),
                  remote.getRemoteAddr().getPort()));
              try {
                remote.send("hello from client by incoming".getBytes(StandardCharsets.UTF_8));
              } catch (BsRemote.BsSendException e) {
                e.printStackTrace();
              }
            }
          }, local4client, remote4client);
          try {
            executorClient.start();
          } catch (BsExecutorClient.BsExecutorClientException e) {
            e.printStackTrace();
          }
          try {
            remote4client.send(("hello from client 1 port " + clientPort).getBytes(StandardCharsets.UTF_8));
            remote4client.send(("hello from client 2 port " + clientPort).getBytes(StandardCharsets.UTF_8));
          } catch (BsRemote.BsSendException e) {
            e.printStackTrace();
          }
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          executorClient.shutdown();

        }).start();
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
