package org.apache.olingo.jpa.processor.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.olingo.jpa.processor.core.api.JPAServiceDebugger;
import org.apache.olingo.server.api.debug.RuntimeMeasurement;

public class JPACoreDebugger implements JPAServiceDebugger {
  private final List<RuntimeMeasurement> runtimeInformation = new ArrayList<RuntimeMeasurement>();

  @Override
  public int startRuntimeMeasurement(final String className, final String methodName) {
    final int handleId = runtimeInformation.size();

    final RuntimeMeasurement measurement = new RuntimeMeasurement();
    measurement.setTimeStarted(System.nanoTime());
    measurement.setClassName(className);
    measurement.setMethodName(methodName);

    runtimeInformation.add(measurement);

    return handleId;
  }

  @Override
  public void stopRuntimeMeasurement(final int handle) {
    if (handle < runtimeInformation.size()) {
      final RuntimeMeasurement runtimeMeasurement = runtimeInformation.get(handle);
      if (runtimeMeasurement != null) {
        runtimeMeasurement.setTimeStopped(System.nanoTime());
      }
    }
  }

  @Override
  public Collection<? extends RuntimeMeasurement> getRuntimeInformation() {
    return runtimeInformation;
  }

}
