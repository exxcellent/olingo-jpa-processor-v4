package org.apache.olingo.jpa.processor.transformation;

import java.lang.reflect.InvocationTargetException;

import org.apache.olingo.jpa.processor.ModifiableDependencyInjector;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.util.TypedParameter;
import org.apache.olingo.jpa.processor.transformation.TransformingFactory.TransformationBuilder;
import org.apache.olingo.server.api.ODataApplicationException;

class ClassTransformationBuilder<I, O> implements TransformationBuilder<I, O> {
  
  private final Class<? extends Transformation<I, O>> classTransformation;
  
  ClassTransformationBuilder(Class<? extends Transformation<I, O>> classTransformation) {
    this.classTransformation = classTransformation;
  }
  
  @Override
  public Transformation<I, O> build(ModifiableJPAODataRequestContext subContext, TypedParameter... transformationContext) throws ReflectiveOperationException {
    try {
      Transformation<I, O> instance = classTransformation.getConstructor().newInstance();
      final ModifiableDependencyInjector dpi = subContext.getDependencyInjector();
      dpi.registerDependencyMappings(transformationContext);
      dpi.injectDependencyValues(instance);
      return instance;
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ODataApplicationException e) {
      throw new ReflectiveOperationException(e);
    }
  }
}
