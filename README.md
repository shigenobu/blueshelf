# blueshelf - Java NIO Udp server & client 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.walksocket/blueshelf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.walksocket/blueshelf)
[![Java CI](https://github.com/shigenobu/blueshelf/actions/workflows/ci.yaml/badge.svg)](https://github.com/shigenobu/blueshelf/actions/workflows/ci.yaml)
[![codecov](https://codecov.io/gh/shigenobu/blueshelf/branch/develop/graph/badge.svg?token=IOVXB31JR7)](https://codecov.io/gh/shigenobu/blueshelf)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## maven

    <dependency>
      <groupId>com.walksocket</groupId>
      <artifactId>blueshelf</artifactId>
      <version>0.1.0</version>
    </dependency>

## how to use

### for server

    // start server
    BsLocal local4Server = new BsLocal("0.0.0.0", 8710);
    BsExecutorServer executor4Server = new BsExecutorServer(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        // receive message from client
        System.out.println(String.format("incoming server: %s (remote -> %s), port:%s",
            new String(message),
            remote,
            remote.getIncomingPort()));

        // get value
        int cnt = 0;
        Optional<Integer> opt = remote.getValue("cnt", Integer.class);
        if (opt.isPresent()) {
          cnt = opt.get();
        }
        remote.setValue("cnt", ++cnt);
        System.out.println("server cnt:" + cnt);

        if (cnt < 5) {
          // send message from server
          try {
            remote.send(("hi, I am server. Cnt is " + cnt).getBytes());
          } catch (BsRemote.BsSendException e) {
            e.printStackTrace();
          }
        } else {
          // escape
          remote.escape();
        }
      }
    }, local4Server);
    executor4Server.start();
    // wait for ...
    executor4Server.shutdown();

### for client

    // start client
    BsLocal local4Client = new BsLocal("0.0.0.0", 18710);
    BsRemote remote4Client = new BsRemote("127.0.0.1", 8710, local4Client.getLocalChannel());
    BsExecutorClient executor4Client = new BsExecutorClient(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        // receive message from server
        System.out.println(String.format("incoming client: %s (remote -> %s)",
            new String(message),
            remote));

        // get value
        int cnt = 0;
        Optional<Integer> opt = remote.getValue("cnt", Integer.class);
        if (opt.isPresent()) {
          cnt = opt.get();
        }
        remote.setValue("cnt", ++cnt);
        System.out.println("client cnt:" + cnt);

        if (cnt < 5) {
          // send message from client
          try {
            remote.send(("hi, I am client. Cnt is " + cnt).getBytes());
          } catch (BsRemote.BsSendException e) {
            e.printStackTrace();
          }
        } else {
          // escape
          remote.escape();
        }
      }
    }, local4Client, remote4Client);
    executor4Client.start();

    // send message from client
    try {
      remote4Client.send("hello from client.".getBytes());
    } catch (BsRemote.BsSendException e) {
      e.printStackTrace();
    }
    
    // wait for ...
    executor4Client.shutdown();
