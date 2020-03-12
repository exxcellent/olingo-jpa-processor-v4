package org.apache.olingo.jpa.processor.transformation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.util.TypedParameter;

/**
 * An instance of this class describes the declaration of an transformation.
 */
public final class TransformationDeclaration<Input, Output> {

  private final List<TransformationContextRequirement> requirements =
      new LinkedList<TransformationContextRequirement>();
  private final Class<Input> inputType;
  private final Class<Output> outputType;

  /**
   *
   * @param outputType The output type of {@link Transformation}.
   * @param requirements Optional list of constraints for transformation usage.
   */
  public TransformationDeclaration(final Class<Input> inputType, final Class<Output> outputType,
      final TransformationContextRequirement... requirements) {
    this.inputType = inputType;
    this.outputType = outputType;
    final Set<Class<?>> types = new HashSet<>();
    if (requirements != null) {
      for (final TransformationContextRequirement r : requirements) {
        if (types.contains(r.getType())) {
          throw new IllegalArgumentException("Duplicate type detected: " + r.getType().getSimpleName());
        }
        types.add(r.getType());
      }
      this.requirements.addAll(Arrays.asList(requirements));
    }
  }

  public Class<Input> getInputType() {
    return inputType;
  }

  public Class<Output> getOutputType() {
    return outputType;
  }

  int getNumberOfRequirements() {
    return requirements.size();
  }

  private final boolean hasMatchingTypes(final Class<?> inputTypeOther, final Class<?> outputTypeOther) {
    if (!inputTypeOther.isAssignableFrom(this.getInputType())) {
      return false;
    }
    if (!outputTypeOther.isAssignableFrom(this.getOutputType())) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return TRUE if this descriptor is matching at least all the input/out/constraints or having more constraints than
   * the requested descriptor (means: this descriptor is more specialized).
   */
  boolean isMatching(final Class<?> inputTypeRequested,
      final Class<?> outputTypeRequested, final Collection<TypedParameter> transformationContextValues,
      final JPAODataRequestContext requestContext) {
    if (!hasMatchingTypes(inputTypeRequested, outputTypeRequested)) {
      return false;
    }
    for (final TransformationContextRequirement r : requirements) {
      // every requirement must be fulfilled...
      final TypedParameter tp = selectParameter(r.getType(), transformationContextValues);
      // ...via transformation context (configuration) values
      if (tp != null) {
        if (isMatchingTransformationContextValue(r, tp)) {
          continue;
        }
        // transformation context overrides request context, so an existing parameter must match or fail (no second
        // chance via request context)
        return false;
      }
      // ...or at least via global/request context
      if (!isMatchingRequestContextValue(r, requestContext)) {
        return false;
      }
    }
    return true;
  }

  private boolean isMatchingRequestContextValue(final TransformationContextRequirement requirement,
      final JPAODataRequestContext requestContext) {
    if (requirement.getAlternatives() == null) {
      return true;
    }
    final Object config = requestContext.getDependencyInjector().getDependencyValue(requirement.getType());
    if (config == null) {
      // no wildcard == no null value
      return false;
    }
    for (final Object alternative : requirement.getAlternatives()) {
      if (isSame(alternative, config)) {
        return true;
      }
    }
    return false;
  }

  private boolean isMatchingTransformationContextValue(final TransformationContextRequirement requirement,
      final TypedParameter typedParameter) {
    if (requirement.getAlternatives() == null) {
      return true;
    }
    if (typedParameter.getValue() == null) {
      // no wildcard == no null value
      return false;
    }
    for (final Object alternative : requirement.getAlternatives()) {
      if (isSame(alternative, typedParameter
          .getValue())) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private boolean isSame(final Object a, final Object b) {
    if (Comparable.class.isInstance(a)) {
      return (Comparable.class.cast(a).compareTo(b) == 0);
    }
    return Objects.equals(a, b);
  }

  private TypedParameter selectParameter(final Class<?> type,
      final Collection<TypedParameter> transformationContextValues) {
    for (final TypedParameter tp : transformationContextValues) {
      if (type == tp.getType()) {
        return tp;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "TransformationDeclaration [" + (requirements != null ? "constraints=" + requirements + ", " : "")
        + (getInputType() != null ? "getInputType()=" + getInputType() + ", " : "") + (getOutputType() != null
        ? "getOutputType()=" + getOutputType() : "") + "]";
  }



}
