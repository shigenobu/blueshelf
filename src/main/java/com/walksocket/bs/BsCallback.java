package com.walksocket.bs;

public interface BsCallback {


  void incoming(BsRemote remote, byte[] message);

}
