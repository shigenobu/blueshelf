package com.walksocket.bs;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class BsMainTest {

  enum TestMessageType {
    FROM_CLIENT,
    FROM_SERVER,
  }

  class TestMessage {
    TestMessageType type;
    String say;

    TestMessage(TestMessageType type, String say) {
      this.type = type;
      this.say = say;
    }

    @Override
    public String toString() {
      return String.format("type:%s, say:%s", type, say);
    }
  }

  byte[] serialize(TestMessage message) {
    int typeNo = message.type.ordinal();
    byte[] sayData = message.say.getBytes(StandardCharsets.UTF_8);

    byte[] data = new byte[4 + sayData.length];
    for (int i = 0; i < 4; i++) {
      data[i] = (byte) ((typeNo >> i * 8) & 0xFF);
    }
    for (int i = 4; i < data.length; i++) {
      data[i] = sayData[i - 4];
    }
    return data;
  }

  TestMessage unserialize(byte[] data) {
    int typeNo = 0;
    for (int i = 0; i < 4; i++) {
      typeNo += (data[i] & 0xFF) << i * 8;
    }

    byte[] sayData = new byte[data.length - 4];
    for (int i = 4; i < data.length; i++) {
      sayData[i - 4] = data[i];
    }
    String say = new String(sayData, StandardCharsets.UTF_8);

    TestMessageType type;
    if (typeNo == TestMessageType.FROM_CLIENT.ordinal()) {
      type = TestMessageType.FROM_CLIENT;
    } else {
      type = TestMessageType.FROM_SERVER;
    }

    return new TestMessage(type, say);
  }

  @BeforeClass
  public static void beforeClass() {
    BsLogger.setVerbose(true);
    BsDate.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
  }

//  @Test
//  public void testSerialize() {
//    byte[] data = serialize(new TestMessage(TestMessageType.FROM_CLIENT, "hi, I am client."));
//    BsLogger.debug(unserialize(data));
//  }

//  @Test
//  public void testExample() throws
//      BsLocal.BsLocalException, BsExecutorClient.BsExecutorClientException, BsExecutorServer.BsExecutorServerException {
//    // start server
//    BsLocal local4Server = new BsLocal("0.0.0.0", 8710);
//    BsExecutorServer executor4Server = new BsExecutorServer(new BsCallback() {
//      @Override
//      public void incoming(BsRemote remote, byte[] message) {
//        // receive message from client
//        System.out.println(String.format("incoming server: %s (remote -> %s)",
//            new String(message),
//            remote.getRemoteHostAndPort()));
//
//        // send message from server
//        try {
//          remote.send("hi, I am server.".getBytes());
//        } catch (BsRemote.BsSendException e) {
//          e.printStackTrace();
//        }
//      }
//    }, local4Server);
//    executor4Server.start();
//
//    // start client
//    BsLocal local4Client = new BsLocal("0.0.0.0", 18710);
//    BsRemote remote4Client = new BsRemote("127.0.0.1", 8710, local4Client.getSendChannel());
//    BsExecutorClient executor4Client = new BsExecutorClient(new BsCallback() {
//      @Override
//      public void incoming(BsRemote remote, byte[] message) {
//        // receive message from server
//        System.out.println(String.format("incoming client: %s (remote -> %s)",
//            new String(message),
//            remote.getRemoteHostAndPort()));
//
//        // send message from client
//        try {
//          remote.send("hi, I am client.".getBytes());
//        } catch (BsRemote.BsSendException e) {
//          e.printStackTrace();
//        }
//      }
//    }, local4Client, remote4Client);
//    executor4Client.start();
//
//    // send message from client
//    try {
//      remote4Client.send("hello from client.".getBytes());
//    } catch (BsRemote.BsSendException e) {
//      e.printStackTrace();
//    }
//
//    // sleep
//    try {
//      Thread.sleep(500);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//
//    // shutdown client
//    executor4Client.shutdown();
//
//    // shutdown server
//    executor4Server.shutdown();
//  }

  @Test
  public void testClientSingleAndServerSingle()
      throws BsLocal.BsLocalException, BsExecutorServer.BsExecutorServerException, BsExecutorClient.BsExecutorClientException {
    // port
    int port4Server = 8710;
    int port4Client = 18710;

    // start server
    BsLocal local4Server = new BsLocal("0.0.0.0", port4Server);
    BsExecutorServer executor4Server = new BsExecutorServer(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        // receive message from client
        BsLogger.debug(String.format("incoming server: %s (remote -> %s)",
            unserialize(message),
            remote.getRemoteHostAndPort()));

        // send message from server
        try {
          remote.send(serialize(new TestMessage(TestMessageType.FROM_SERVER, "hi, I am server.")));
        } catch (BsRemote.BsSendException e) {
          e.printStackTrace();
        }
      }
    }, local4Server);
    executor4Server.start();

    // start client
    BsLocal local4Client = new BsLocal("0.0.0.0", port4Client);
    BsRemote remote4Client = new BsRemote("127.0.0.1", port4Server, local4Client.getSendChannel());
    BsExecutorClient executor4Client = new BsExecutorClient(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        // receive message from server
        BsLogger.debug(String.format("incoming client: %s (remote -> %s)",
            unserialize(message),
            remote.getRemoteHostAndPort()));

        // send message from client
        try {
          remote.send(serialize(new TestMessage(TestMessageType.FROM_CLIENT, "hi, I am client.")));
        } catch (BsRemote.BsSendException e) {
          e.printStackTrace();
        }
      }
    }, local4Client, remote4Client);
    executor4Client.start();

    // send message from client
    try {
      remote4Client.send(serialize(new TestMessage(TestMessageType.FROM_CLIENT, "hello from client.")));
    } catch (BsRemote.BsSendException e) {
      e.printStackTrace();
    }

    // sleep
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // shutdown client
    executor4Client.shutdown();

    // shutdown server
    executor4Server.shutdown();
  }

  @Test
  public void testClientSingleAndServerMulti() throws BsLocal.BsLocalException, BsExecutorServer.BsExecutorServerException {
    // for server
    List<BsLocal> locals4server = Arrays.asList(
        new BsLocal("0.0.0.0", 8711),
        new BsLocal("0.0.0.0", 8712),
        new BsLocal("0.0.0.0", 8713)
    );
    BsExecutorServer executorServer = new BsExecutorServer(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        System.out.println("incoming server :" + new String(message));
        try {
          remote.send("hello from server by incoming".getBytes(StandardCharsets.UTF_8));
        } catch (BsRemote.BsSendException e) {
          e.printStackTrace();
        }
      }
    }, locals4server);
    executorServer.start();

    // for client
    for (int port : Arrays.asList(8711, 8712, 8713)) {
      for (int i = 0; i < 2; i++) {
        int clientPort = port + 10000 + i * 1000;
        new Thread(() -> {
          BsLocal local4client = null;
          try {
            local4client = new BsLocal("0.0.0.0", clientPort);
          } catch (BsLocal.BsLocalException e) {
            e.printStackTrace();
          }
          BsRemote remote4client = new BsRemote("127.0.0.1", port, local4client.getSendChannel());
          BsExecutorClient executorClient = new BsExecutorClient(new BsCallback() {
            @Override
            public void incoming(BsRemote remote, byte[] message) {
              System.out.println("incoming client :" + new String(message));
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
            Thread.sleep(2000);
          } catch (BsRemote.BsSendException e) {
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          executorClient.shutdown();

        }).start();
      }

    }

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    executorServer.shutdown();
  }

}