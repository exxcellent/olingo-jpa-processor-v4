package org.apache.olingo.jpa.processor.transformation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.DependencyInjector;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.util.TypedParameter;
import org.apache.olingo.jpa.processor.transformation.impl.QueryEntityResult2EntityCollectionTransformation;
import org.apache.olingo.jpa.processor.transformation.impl.QueryEntityResult2ODataResponseContentTransformation;
import org.apache.olingo.server.api.serializer.SerializerException;

/**
 * Helper class to build a proper serializer to convert processor results to requested response format.
 * @author Ralf Zozmann
 *
 */
public class TransformingFactory {

  private final Map<TransformationDeclaration<?, ?>, Class<? extends Transformation<?, ?>>> mapRegisteredTransformings =
      new HashMap<>();
  private final JPAODataRequestContext requestContext;

  public TransformingFactory(final JPAODataRequestContext context) {
    this.requestContext = context;
    registerBuiltinTransformings();
  }

  private void registerBuiltinTransformings() {
    // DB-Tuples -> OData-EntityCollection
    registerTransformation(QueryEntityResult2EntityCollectionTransformation.DEFAULT_DECLARATION,
        QueryEntityResult2EntityCollectionTransformation.class);

    // DB-Tuples -> OData-EntityCollection -> JSON/XML
    registerTransformation(QueryEntityResult2ODataResponseContentTransformation.DEFAULT_DECLARATION,
        QueryEntityResult2ODataResponseContentTransformation.class);
  }

  public <I, O> void registerTransformation(final TransformationDeclaration<I, O> declaration,
      final Class<? extends Transformation<I, O>> serializer) {
    if (serializer == null) {
      throw new IllegalArgumentException("serializer required");
    }
    if (declaration == null) {
      throw new IllegalArgumentException("descriptor required");
    }
    synchronized (mapRegisteredTransformings) {
      mapRegisteredTransformings.put(declaration, serializer);
    }
  }

  /**
   * @param transformationContext List of configuration values helping to find a matching implementation for the
   * requested {@link Transformation}. A transformation can be found only if a subset of transformation
   * {@link TypedParameter context information}'s plus all the {@link JPAODataRequestContext#getDependencyInjector()
   * request context} informations match all the {@link TransformationContextRequirement
   * transformation requirement}'s.
   * @return Never <code>null</code>
   * @throws SerializerException If no transformation can be found.
   */
  public <T extends Transformation<I, O>, I, O> T createTransformation(final Class<I> inputType,
      final Class<O> outputType,
      final TypedParameter... transformationContext)
          throws SerializerException {
    final Map<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> matchingTransformations =
        collectMatchingTransformations(inputType, outputType, transformationContext);
    if (matchingTransformations.isEmpty()) {
      throw new SerializerException(inputType.getName() + " -> " + outputType.getName()
      + " is not supported by this factory",
      SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
    // try to find a better matching (more specific) one...
    Map.Entry<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> bestMatchEntry = null;
    for (final Map.Entry<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> entry : matchingTransformations
        .entrySet()) {
      if (bestMatchEntry == null) {
        bestMatchEntry = entry;
        continue;
      } else if (!bestMatchEntry.getKey().hasPrecedenceOver(entry.getKey())) {
        bestMatchEntry = entry;
      }
    }
    @SuppressWarnings("null")
    final Class<? extends Transformation<I, O>> clazz = bestMatchEntry.getValue();
    return createTransformation(clazz, transformationContext);
  }

  @SuppressWarnings("unchecked")
  private <I, O> Map<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>>
  collectMatchingTransformations(final Class<I> inputType,
      final Class<O> outputType, final TypedParameter... transformationContext) {
    final Collection<TypedParameter> transformationContextValues = transformationContext!=null? Arrays.asList(transformationContext): Collections.emptyList();
    synchronized (mapRegisteredTransformings) {
      final Map<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> map = new HashMap<>();
      for (final Map.Entry<TransformationDeclaration<?, ?>, Class<? extends Transformation<?, ?>>> entry : mapRegisteredTransformings
          .entrySet()) {
        final TransformationDeclaration<?,?> declaration = entry.getKey();
        if (!declaration.isMatching(inputType, outputType, transformationContextValues, requestContext)) {
          continue;
        }
        map.put((TransformationDeclaration<I, O>) declaration, (Class<? extends Transformation<I, O>>) entry
            .getValue());
      }
      return map;
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Transformation<I, O>, I, O> T createTransformation(
      final Class<? extends Transformation<I, O>> classTransformation, final TypedParameter... transformationContext)
          throws SerializerException {
    try {
      final Transformation<I, O> instance = classTransformation.newInstance();
      final JPAODataRequestContext subContext = requestContext.createSubRequestContext();
      final DependencyInjector dpi = subContext.getDependencyInjector();
      dpi.registerDependencyMappings(transformationContext);
      dpi.injectDependencyValues(instance);
      return (T) instance;
    } catch (InstantiationException | IllegalAccessException | ODataException e) {
      throw new SerializerException("Could not create instance of builtin transformation", e,
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
  }
}
