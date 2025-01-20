package org.apache.olingo.jpa.processor.transformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.impl.EntityCollection2ODataResponseContentTransformation;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent;
import org.apache.olingo.jpa.processor.transformation.impl.QueryEntityResult2EntityCollectionTransformation;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.junit.Test;

public class TransformationTest {

  private class DummyTransformation<X extends Object> implements Transformation<X, Object> {
    private final Class<X> inputClass;
    
    public DummyTransformation(Class<X> inputClass) {
      this.inputClass = inputClass;
    }
    
    @Override
    public @NotNull Class<X> getInputType() {
      return inputClass;
    }

    @Override
    public @NotNull Class<Object> getOutputType() {
      return Object.class;
    }

    @Override
    public Object transform(@NotNull X input) throws SerializerException {
      return null;
    }
  }

  @Test
  public void testTransformationToChain() {
    EntityCollection2ODataResponseContentTransformation t1 = new EntityCollection2ODataResponseContentTransformation();
    assertNotNull(t1.asChain());
    QueryEntityResult2EntityCollectionTransformation t2 = new QueryEntityResult2EntityCollectionTransformation();
    assertNotNull(t2.asChain());
  }

  @Test
  public void testInvalidTransformationChainStep() {
    @SuppressWarnings("unchecked")
    TransformationChain<Object, Object> tc =
        ((TransformationChain<Object, Object>) (TransformationChain<?, ?>) new EntityCollection2ODataResponseContentTransformation()
            .asChain());
    Transformation<Object, Object> tI = (Transformation<Object, Object>) new DummyTransformation<Object>(Object.class);
    assertThrows(AssertionError.class, () -> tc.addStep(tI));
  }

  @Test
  public void testFindTransformationChainStep() throws ODataJPAConversionException {
    TransformationChain<QueryEntityResult, Object> tc = new QueryEntityResult2EntityCollectionTransformation().asChain().addStep(new EntityCollection2ODataResponseContentTransformation()).addStep(new DummyTransformation<ODataResponseContent>(ODataResponseContent.class));
    assertNotNull(tc.createSubTransformation(EntityCollection.class, ODataResponseContent.class));
    assertNotNull(tc.createSubTransformation(ODataResponseContent.class, Object.class));
    assertEquals(2, tc.createSubTransformation(EntityCollection.class, Object.class).getSteps().size());
    assertEquals(3, tc.createSubTransformation(QueryEntityResult.class, Object.class).getSteps().size());
  }
  
}
