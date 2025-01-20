package org.apache.olingo.jpa.processor.transformation.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.TransformationChain;
import org.apache.olingo.server.api.serializer.SerializerException;

import javax.validation.constraints.NotNull;

/**
 * Helper class to combine two ore more transformations in a chained sequence.
 */
public final class TransformationSequence<Input, Output> implements TransformationChain<Input, Output> {

  private LinkedList<Transformation<Object, Object>> steps = new LinkedList<>();

  @SuppressWarnings({ "unchecked" })
  public TransformationSequence(@NotNull Transformation<Input, Output> first) {
    this(Collections.singletonList((Transformation<Object, Object>)first));
  }

  private TransformationSequence(List<Transformation<Object, Object>> buildFromSource) {
    steps.addAll(buildFromSource);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Class<Input> getInputType() {
    synchronized (steps) {
      return (Class<Input>) steps.getFirst().getInputType();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<Output> getOutputType() {
    synchronized (steps) {
      return (Class<Output>) steps.getLast().getOutputType();
    }
  }

  @Override
  public List<Transformation<?, ?>> getSteps() {
    return Collections.unmodifiableList(steps);
  }

  @Override
  public <X, Y> TransformationChain<X, Y> createSubTransformation(@NotNull Class<X> from, @NotNull Class<Y> to)
      throws ODataJPAConversionException {
    synchronized (steps) {
      int startI = findStartStepIndex(from);
      if (startI < 0) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM,
            "No transformation from '" + from.getName() + "' available in chain");
      }
      int endI = findEndStepIndex(to, startI);
      if (endI < 0) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM,
            "No transformation to '" + to.getName() + "' available in chain");
      }
      List<Transformation<Object, Object>> sublist = steps.subList(startI, endI+1);
      return new TransformationSequence<X,Y>(sublist);
    }
  }

  private <X> int findStartStepIndex(Class<X> from) {
    for (int i = 0; i < steps.size(); i++) {
      Transformation<?, ?> x = steps.get(i);
      if (x.getInputType() == from) {
        return i;
      }
    }
    return -1;
  }

  private <Y> int findEndStepIndex(Class<Y> to, int startIndex) {
    for (int i = startIndex; i < steps.size(); i++) {
      Transformation<?, ?> t = steps.get(i);
      if (t.getOutputType() == to) {
        return i;
      }
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <X> TransformationChain<Input, X> addStep(Transformation<Output, X> chainedElement) {
    synchronized (steps) {
      assert steps.getLast().getOutputType() == chainedElement.getInputType();
      assert chainedElement.getInputType() != chainedElement.getOutputType();
      steps.add((Transformation<Object, Object>) chainedElement);
      return (TransformationChain<Input, X>) this;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Output transform(Input input) throws SerializerException {
    synchronized (steps) {
      Object x = input;
      for (Transformation<Object, Object> t : steps) {
        x = t.transform(x);
      }
      return (Output) x;
    }
  }
}
