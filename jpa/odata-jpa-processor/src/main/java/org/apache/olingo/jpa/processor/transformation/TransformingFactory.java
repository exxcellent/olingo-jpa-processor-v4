package org.apache.olingo.jpa.processor.transformation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.util.TypedParameter;
import org.apache.olingo.jpa.processor.transformation.impl.EntityCollection2ODataResponseContentTransformation;
import org.apache.olingo.jpa.processor.transformation.impl.QueryEntityResult2EntityCollectionTransformation;
import org.apache.olingo.server.api.serializer.SerializerException;

/**
 * Helper class to build a proper transformer to convert processor results to requested response format.
 * @author Ralf Zozmann
 *
 */
public class TransformingFactory {

  interface TransformationBuilder<I, O> {
    public Transformation<I, O> build(ModifiableJPAODataRequestContext subContext,
        TypedParameter... transformationContext) throws ReflectiveOperationException;
  }

  private final Map<TransformationDeclaration<?, ?>, TransformationBuilder<?, ?>> mapRegisteredTransformings =
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

    // OData-EntityCollection -> JSON/XML
    registerTransformation(EntityCollection2ODataResponseContentTransformation.DEFAULT_DECLARATION,
        EntityCollection2ODataResponseContentTransformation.class);

    // register chain to combine both
    registerTransformation(ChainTransformationBuilder.createDeclaration(
        QueryEntityResult2EntityCollectionTransformation.DEFAULT_DECLARATION,
        EntityCollection2ODataResponseContentTransformation.DEFAULT_DECLARATION), ChainTransformationBuilder.create(
            QueryEntityResult2EntityCollectionTransformation.class).append(
                EntityCollection2ODataResponseContentTransformation.class));
  }

  public <I, O> void registerTransformation(final TransformationDeclaration<I, O> declaration,
      final Class<? extends Transformation<I, O>> transformerClass) {
    if (transformerClass == null) {
      throw new IllegalArgumentException("transformer class required");
    }
    registerTransformation(declaration, new ClassTransformationBuilder<>(transformerClass));
  }

  private <I, O> void registerTransformation(final TransformationDeclaration<I, O> declaration,
      TransformationBuilder<I, O> builder) {
    if (builder == null) {
      throw new IllegalArgumentException("builder required");
    }
    if (declaration == null) {
      throw new IllegalArgumentException("descriptor required");
    }
    synchronized (mapRegisteredTransformings) {
      mapRegisteredTransformings.put(declaration, builder);
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
    final Map<TransformationDeclaration<I, O>, TransformationBuilder<?, ?>> matchingTransformations =
        collectMatchingTransformations(inputType, outputType, transformationContext);
    if (matchingTransformations.isEmpty()) {
      throw new SerializerException(inputType.getName() + " -> " + outputType.getName()
          + " is not supported by this factory",
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
    // try to find a better matching (more specific) one...
    Map.Entry<TransformationDeclaration<I, O>, TransformationBuilder<?, ?>> bestMatchEntry = null;
    for (final Map.Entry<TransformationDeclaration<I, O>, TransformationBuilder<?, ?>> entry : matchingTransformations
        .entrySet()) {
      if (bestMatchEntry == null) {
        bestMatchEntry = entry;
        continue;
      } else if (!bestMatchEntry.getKey().hasPrecedenceOver(entry.getKey())) {
        bestMatchEntry = entry;
      }
    }
    final TransformationBuilder<?, ?> builder = bestMatchEntry.getValue();
    return createTransformation(builder, transformationContext);
  }

  @SuppressWarnings("unchecked")
  private <I, O> Map<TransformationDeclaration<I, O>, TransformationBuilder<?, ?>>
      collectMatchingTransformations(final Class<I> inputType,
          final Class<O> outputType, final TypedParameter... transformationContext) {
    final Collection<TypedParameter> transformationContextValues = transformationContext != null ? Arrays.asList(
        transformationContext) : Collections.emptyList();
    synchronized (mapRegisteredTransformings) {
      final Map<TransformationDeclaration<I, O>, TransformationBuilder<?, ?>> map = new HashMap<>();
      for (final Map.Entry<TransformationDeclaration<?, ?>, TransformationBuilder<?, ?>> entry : mapRegisteredTransformings
          .entrySet()) {
        final TransformationDeclaration<?, ?> declaration = entry.getKey();
        if (!declaration.isMatching(inputType, outputType, transformationContextValues, requestContext)) {
          continue;
        }
        map.put((TransformationDeclaration<I, O>) declaration, entry.getValue());
      }
      return map;
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Transformation<I, O>, I, O> T createTransformation(
      final TransformationBuilder<?, ?> builder, final TypedParameter... transformationContext)
      throws SerializerException {
    try {
      final ModifiableJPAODataRequestContext subContext = requestContext.createSubRequestContext();
      return (T) builder.build(subContext, transformationContext);
    } catch (ReflectiveOperationException | ODataException e) {
      throw new SerializerException("Could not create instance of builtin transformation", e,
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
  }
}
