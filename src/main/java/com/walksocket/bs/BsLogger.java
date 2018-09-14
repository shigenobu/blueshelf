package com.walksocket.bs;

import java.util.function.Supplier;

public class BsLogger {

  private static boolean verbose = true;

  static void debug(Object message) {
    out("D", message);
  }

  static void debug(Supplier<Object> message) {
    if (!verbose) {
      return;
    }
    out("D", message.get());
  }

  static void error(Object message) {
    out("E", message);
  }

  private static void out(String level, Object message) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append(BsDate.now());
    builder.append("]");
    builder.append("[");
    builder.append("BS");
    builder.append("-");
    builder.append(level);
    builder.append("]");
    builder.append("[");
    builder.append(String.format("%010d", Thread.currentThread().getId()));
    builder.append("]");
    builder.append(message.toString());
    if (message instanceof Throwable) {
      StackTraceElement[] stacks = ((Throwable) message).getStackTrace();
      for (StackTraceElement stack : stacks) {
        builder.append("\n");
        builder.append("(C:" + stack.getClassName() + ")");
        builder.append("(F:" + stack.getFileName() + ")");
        builder.append("(L:" + stack.getLineNumber() + ")");
        builder.append("(M:" + stack.getMethodName() + ")");
      }
    }
    System.out.println(builder.toString());
  }
}
