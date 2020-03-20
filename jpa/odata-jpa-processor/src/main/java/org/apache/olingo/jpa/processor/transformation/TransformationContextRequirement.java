package org.apache.olingo.jpa.processor.transformation;

import java.util.Arrays;
import java.util.Collection;

/**
 * Defines a concrete requirement for transformation processing. A transformation will be used only if all requirements
 * are fulfilled by callers context.
 *
 * @author Ralf Zozmann
 *
 */
public final class TransformationContextRequirement {

  private final Class<?> type;
  private final Collection<Object> alternativeValues;

  /**
   *
   * @param type The type of requirement.
   * @param alternatives The allowed alternative values for the type. A <code>null</code> value works as wildcard:
   * allowing any value. Alternative values should implement {@link Comparable} interface to avoid object comparison.
   */
  @SafeVarargs
  public <T> TransformationContextRequirement(final Class<T> type, final T... alternatives) {
    this.type = type;
    if (alternatives == null || alternatives.length == 0) {
      alternativeValues = null;
    } else {
      for (final Object a : alternatives) {
        if (a == null) {
          throw new IllegalArgumentException("Alternative must not be null");
        }
      }
      this.alternativeValues = Arrays.asList(alternatives);
    }
  }

  public Class<?> getType() {
    return type;
  }

  /**
   *
   * @return The collection of alternative values or <code>null</code> if placeholder for any value of {@link #getType()
   * type}.
   */
  public Collection<Object> getAlternatives() {
    return alternativeValues;
  }
}
