package org.apache.olingo.jpa.processor.transformation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.util.TypedParameter;
import org.apache.olingo.jpa.processor.transformation.TransformingFactory.TransformationBuilder;
import org.apache.olingo.jpa.processor.transformation.impl.TransformationSequence;

/**
 * Helper class to manage building of chained transformations.
 */
class ChainTransformationBuilder<I, O> implements TransformationBuilder<I, O> {
  
  private final LinkedList<ClassTransformationBuilder<?, ?>> builders = new LinkedList<>();
  
  private ChainTransformationBuilder(Class<? extends Transformation<I, O>> first) {
    this.builders.add(new ClassTransformationBuilder<>(first));
  }
  
  static <I, O> ChainTransformationBuilder<I, O> create(Class<? extends Transformation<I, O>> first) {
    return new ChainTransformationBuilder<>(first);
  }
  
  @SuppressWarnings("unchecked")
  <X> ChainTransformationBuilder<I, X> append(Class<? extends Transformation<O, X>> next) {
    this.builders.add(new ClassTransformationBuilder<>(next));
    return (ChainTransformationBuilder<I, X>) this;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Transformation<I, O> build(ModifiableJPAODataRequestContext subContext, TypedParameter... transformationContext) throws ReflectiveOperationException {
    ClassTransformationBuilder<Object, Object> builder = (ClassTransformationBuilder<Object, Object>) builders.get(0);
    Transformation<Object, Object> t = builder.build(subContext, transformationContext);
    TransformationSequence<Object, Object> chain = new TransformationSequence<Object, Object>(t);
    
    for(int i=1;i<builders.size();i++) {
      builder = (ClassTransformationBuilder<Object, Object>) builders.get(i);
      t = builder.build(subContext, transformationContext);
      chain = (TransformationSequence<Object, Object>) chain.addStep(t);
    }
    return (Transformation<I, O>) chain;
  }
  
  /**
   * Helper function to produce a new {@link TransformationDeclaration} from requirements of all given.
   */
  @SuppressWarnings("unchecked")
  static <I, O> TransformationDeclaration<I, O> createDeclaration(TransformationDeclaration<?, ?>... declarations) {
    if(declarations.length == 1) {
      return (TransformationDeclaration<I, O>) declarations[0];
    }
    Class<I> classI = (Class<I>) declarations[0].getInputType();
    Class<O> classO = (Class<O>) declarations[declarations.length-1].getOutputType();
    List<TransformationContextRequirement> requirements = new LinkedList<>();
    for(TransformationDeclaration<?, ?> decl: declarations) {
      for(TransformationContextRequirement r: decl.getRequirements()) {
        TransformationContextRequirement alreadyMerged = requirements.stream().filter(x -> x.getType() == r.getType()).findFirst().orElse(null);
        if(alreadyMerged == null) {
          //type not yet seen, simply add requirement
          requirements.add(r);
        } else {
          //merge alternatives to existing TransformationContextRequirement
          Collection<Object> moreValues = r.getAlternatives();
          if(moreValues != null) {
            List<Object> newAlternatives = new LinkedList<>();
            if(alreadyMerged.getAlternatives() != null) {
              //compare lists
              newAlternatives.addAll(alreadyMerged.getAlternatives());
              for(Object v: moreValues) {
                if(!newAlternatives.contains(v)) {
                  newAlternatives.add(v);
                }
              }
            } else {
              //copy into new list
              newAlternatives.addAll(moreValues);
            }
            //replace existing requirement by new one
            requirements.remove(alreadyMerged);
            requirements.add(new TransformationContextRequirement((Class<Object>)r.getType(), newAlternatives.toArray()));
          }
        }
      }
    }
    return new TransformationDeclaration<I, O>( classI, classO, requirements.toArray(new TransformationContextRequirement[0]) );
  }
}
