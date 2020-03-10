package org.apache.olingo.jpa.processor.conversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.jpa.processor.api.DependencyInjector;
import org.apache.olingo.jpa.processor.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.conversion.impl.QueryEntityResult2EntityCollectionTransformation;
import org.apache.olingo.jpa.processor.conversion.impl.QueryEntityResult2SerializeResultTransformation;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.util.DependencyMapping;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;

/**
 * Helper class to build a proper serializer to convert processor results to requested response format.
 * @author Ralf Zozmann
 *
 */
public class TransformingFactory {

  private final Map<TransformationDeclaration<?, ?>, Class<? extends Transformation<?, ?>>> mapRegisteredTransformings =
      new HashMap<>();
  private final JPAODataSessionContextAccess context;

  public TransformingFactory(final JPAODataSessionContextAccess context) {
    this.context = context;
    registerBuiltinTransformings();
  }

  private void registerBuiltinTransformings() {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> tD1 = new TransformationDeclaration.Builder<>(
        QueryEntityResult.class, EntityCollection.class).build();
    registerSerializer(tD1, QueryEntityResult2EntityCollectionTransformation.class);

    final TransformationDeclaration<QueryEntityResult, SerializerResult> tD2 = new TransformationDeclaration.Builder<>(
        QueryEntityResult.class, SerializerResult.class).addConstraintAlternatives(RepresentationType.COLLECTION_ENTITY)
        .build();
    registerSerializer(tD2, QueryEntityResult2SerializeResultTransformation.class);
  }

  public <I, O> void registerSerializer(final TransformationDeclaration<I, O> declaration,
      final Class<? extends Transformation<I, O>> serializer) {
    if (serializer == null) {
      throw new IllegalArgumentException("serializer required");
    }
    if (declaration == null) {
      throw new IllegalArgumentException("descriptor required");
    }
    synchronized (mapRegisteredTransformings) {
      // check ambiguous declarations
      for (final TransformationDeclaration<?, ?> existing : mapRegisteredTransformings.keySet()) {
        if (existing.isMatching(declaration)) {
          throw new IllegalArgumentException(
              "Transformation descriptor will match other already registered transformation: " + existing.toString());
        }
      }
      // ok
      mapRegisteredTransformings.put(declaration, serializer);
    }
  }

  /**
   * @param dependencies Optional list of configuration values to inject into created {@link Transformation}.
   * @return Never <code>null</code>
   * @throws SerializerException If no transformer can be found.
   */
  public <T extends Transformation<I, O>, I, O> T createTransformation(final TransformationRequest<I, O> descriptor,
      final DependencyMapping... dependencies)
          throws SerializerException {
    final Map<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> matchingTransformations =
        collectMatchingTransformations(descriptor);
    if (matchingTransformations.isEmpty()) {
      throw new SerializerException(descriptor.toString() + " is not supported by this factory",
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
    // try to find a better matching (more specific) one...
    Map.Entry<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> bestMatchEntry = null;
    for (final Map.Entry<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> entry : matchingTransformations
        .entrySet()) {
      if (bestMatchEntry == null) {
        bestMatchEntry = entry;
        continue;
      } else if (bestMatchEntry.getKey().getNumberOfConstraints() < entry.getKey().getNumberOfConstraints()) {
        bestMatchEntry = entry;
      }
    }
    @SuppressWarnings("null")
    final Class<? extends Transformation<I, O>> clazz = bestMatchEntry.getValue();
    return createTransformation(clazz, dependencies);
  }

  @SuppressWarnings("unchecked")
  private <I, O> Map<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>>
  collectMatchingTransformations(final TransformationRequest<I, O> descriptor) {
    synchronized (mapRegisteredTransformings) {
      final Map<TransformationDeclaration<I, O>, Class<? extends Transformation<I, O>>> map = new HashMap<>();
      for (final Map.Entry<TransformationDeclaration<?, ?>, Class<? extends Transformation<?, ?>>> entry : mapRegisteredTransformings
          .entrySet()) {
        if (!entry.getKey().isMatching(descriptor)) {
          continue;
        }
        map.put((TransformationDeclaration<I, O>) entry.getKey(), (Class<? extends Transformation<I, O>>) entry
            .getValue());
      }
      return map;
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Transformation<I, O>, I, O> T createTransformation(
      final Class<? extends Transformation<I, O>> classTransformation, final DependencyMapping... dependencies)
          throws SerializerException {
    try {
      final Transformation<I, O> instance = classTransformation.newInstance();
      final DependencyInjector dpi = context.getDependencyInjector();
      dpi.registerDependencyMappings(dependencies);
      dpi.injectFields(instance);
      return (T) instance;
    } catch (InstantiationException | IllegalAccessException | ODataApplicationException e) {
      throw new SerializerException("Could not create instance of builtin transformation", e,
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
  }
}
