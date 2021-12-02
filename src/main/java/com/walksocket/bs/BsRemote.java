package com.walksocket.bs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * udp remote configuration.
 * @author shigenobu
 * @version 0.0.6
 *
 */
public class BsRemote {

  /**
   * remote id.
   */
  private String rid;

  /**
   * sending remote address.
   */
  private InetSocketAddress remoteAddr;

  /**
   * local channel.
   */
  private BsLocalChannel localChannel;

  /**
   * life timestamp milliseconds.
   */
  private long lifeTimestampMilliseconds;

  /**
   * idle milliseconds.
   */
  private int idleMilliSeconds = 10000;

  /**
   * active flag.
   * <pre>
   *   timeout or shutdown callback was invoked, set false.
   * </pre>
   */
  private boolean active = true;

  /**
   * values.
   */
  private Map<String, Object> values;

  /**
   * newest.
   */
  private boolean newest = true;

  /**
   * constructor.
   * @param remoteHost remote host
   * @param remotePort remote port
   * @param localChannel local channel
   */
  public BsRemote(String remoteHost, int remotePort, BsLocalChannel localChannel) {
    this(new InetSocketAddress(remoteHost, remotePort), localChannel);
  }

  /**
   * constructor.
   * @param remoteAddr remote address
   * @param localChannel local channel
   */
  public BsRemote(InetSocketAddress remoteAddr, BsLocalChannel localChannel) {
    this.rid = UUID.randomUUID().toString();
    this.remoteAddr = remoteAddr;
    this.localChannel = localChannel;
    this.lifeTimestampMilliseconds = BsDate.timestampMilliseconds() + idleMilliSeconds;
  }

  /**
   * is timeout.
   * @return if timeout, true
   */
  boolean isTimeout() {
    return !newest && BsDate.timestampMilliseconds() > lifeTimestampMilliseconds;
  }

  /**
   * update timeout.
   */
  void updateTimeout() {
    newest = false;
    this.lifeTimestampMilliseconds = BsDate.timestampMilliseconds() + idleMilliSeconds;
  }

  /**
   * get idle milliseconds.
   * @return idle milliseconds
   */
  public int getIdleMilliSeconds() {
    return idleMilliSeconds;
  }

  /**
   * set active.
   * @param active active flag
   */
  void setActive(boolean active) {
    this.active = active;
  }

  /**
   * is active.
   * @return active
   */
  boolean isActive() {
    return active;
  }

  /**
   * set idle milliseconds.
   * @param idleMilliSeconds idle milliseconds
   */
  public void setIdleMilliSeconds(int idleMilliSeconds) {
    this.idleMilliSeconds = idleMilliSeconds;
    updateTimeout();
  }

  /**
   * send message.
   * @param bytes message
   * @throws BsSendException send exception
   */
  public void send(byte[] bytes) throws BsSendException {
    // if escaped, disallow send
    if (lifeTimestampMilliseconds == 0) {
      throw new BsSendException(String.format("remote(%s) is already escaped."));
    }

    // if not active, disallow send
    if (!active) {
      throw new BsSendException(String.format("remote(%s) is not active."));
    }

    try {
      localChannel.getChannel().send(ByteBuffer.wrap(bytes), remoteAddr);
    } catch (IOException e) {
      BsLogger.error(e);
      throw new BsSendException(e);
    }
  }

  /**
   * escape.
   */
  public void escape() {
    // lifetime is force to set 0
    lifeTimestampMilliseconds = 0;
    BsLogger.debug(() -> String.format("escape %s", this));
  }

  /**
   * get remote address.
   * @return remote address
   */
  public InetSocketAddress getRemoteAddr() {
    return remoteAddr;
  }

  /**
   * get remote host and port.
   * @return host and port
   */
  public String getRemoteHostAndPort() {
    return String.format("%s:%s", remoteAddr.getHostString(), remoteAddr.getPort());
  }

  /**
   * get incoming port.
   * @return incoming port
   */
  public int getIncomingPort() {
    return localChannel.getLocalAddr().getPort();
  }

  /**
   * set value.
   * @param name your name
   * @param value your valut
   */
  public void setValue(String name, Object value) {
    if (values == null) {
      values = new HashMap<>();
    }
    values.put(name, value);
  }

  /**
   * get value.
   * @param <T> your type
   * @param name your name
   * @param cls your class
   * @return optional(your value or null)
   */
  public <T> Optional<T> getValue(String name, Class<T> cls) {
    if (values == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(cls.cast(values.get(name)));
  }

  /**
   * clear value.
   * @param name your name.
   */
  public void clearValue(String name) {
    if (values == null) {
      return;
    }
    values.remove(name);
  }

  @Override
  public String toString() {
    return String.format(
        "rid:%s, remoteAddr:%s, channel:%s",
        rid,
        remoteAddr,
        localChannel.getChannel());
  }

  /**
   * send exception.
   * @author shigenobu
   *
   */
  public class BsSendException extends Exception {

    /**
     * version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * @param e error
     */
    private BsSendException(Throwable e) {
      super(e);
    }

    /**
     * constructor.
     * @param message message
     */
    private BsSendException(String message) {
      super(message);
    }
  }
}
