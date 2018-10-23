# blueshelf - Java NIO Udp server & client 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.walksocket/blueshelf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.walksocket/blueshelf)
[![Javadoc](https://javadoc-badge.appspot.com/com.walksocket/blueshelf.svg?label=javadoc)](https://javadoc-badge.appspot.com/com.walksocket/blueshelf)
[![Build Status](https://travis-ci.org/shigenobu/blueshelf.svg?branch=master)](https://travis-ci.org/shigenobu/blueshelf)
[![Coverage Status](https://coveralls.io/repos/github/shigenobu/blueshelf/badge.svg?branch=master)](https://coveralls.io/github/shigenobu/blueshelf?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## how to use

### for server

    // start server
    BsLocal local4Server = new BsLocal("0.0.0.0", 8710);
    BsExecutorServer executor4Server = new BsExecutorServer(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        // receive message from client
        System.out.println(String.format("incoming server: %s (remote -> %s)",
            new String(message),
            remote.getRemoteHostAndPort()));

        // send message from server
        try {
          remote.send("hi, I am server.".getBytes());
        } catch (BsRemote.BsSendException e) {
          e.printStackTrace();
        }
      }
    }, local4Server);
    executor4Server.start();
    // wait for ...
    executor4Server.shutdown();

### for client

    // start client
    BsLocal local4Client = new BsLocal("0.0.0.0", 18710);
    BsRemote remote4Client = new BsRemote("127.0.0.1", 8710, local4Client.getSendChannel());
    BsExecutorClient executor4Client = new BsExecutorClient(new BsCallback() {
      @Override
      public void incoming(BsRemote remote, byte[] message) {
        // receive message from server
        System.out.println(String.format("incoming client: %s (remote -> %s)",
            new String(message),
            remote.getRemoteHostAndPort()));

        // send message from client
        try {
          remote.send("hi, I am client.".getBytes());
        } catch (BsRemote.BsSendException e) {
          e.printStackTrace();
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
