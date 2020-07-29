package org.apache.olingo.jpa.generator.api.client.generatorclassloader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LogWrapper {

  private Object log;

  public LogWrapper(final Object mavenLogger) {
    this.log = mavenLogger;
  }

  public void info(final String message) {
    log("info", message);
  }

  public void debug(final String message) {
    log("debug", message);
  }

  public void error(final String message) {
    log("error", message);
  }

  private void log(final String levelMethod, final String message) {
    if (log == null) {
      return;
    }
    try {
      final Method infoMethod = log.getClass().getMethod(levelMethod, CharSequence.class);
      infoMethod.invoke(log, message);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      log = null;
      e.printStackTrace();
    }
  }
}
