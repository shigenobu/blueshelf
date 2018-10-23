# blueshelf - Java NIO Udp server & client 

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
