package org.apache.olingo.jpa.processor.conversion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.util.ContentTypeComparable;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.junit.Test;

public class TransformationDescriptionTest {

  @Test
  public void testDescriptorSameMustMatch() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration.Builder<>(
            QueryEntityResult.class, EntityCollection.class).addConstraint(
                RepresentationType.COLLECTION_ENTITY).build();
    final TransformationRequest<QueryEntityResult, EntityCollection> d2 = new TransformationRequest<>(
        QueryEntityResult.class, EntityCollection.class,
        RepresentationType.COLLECTION_ENTITY);
    assertTrue(declaration.isMatching(d2));
  }

  @Test
  public void testDescriptorInputSubclassMustMatch() throws IOException, ODataException {
    final TransformationDeclaration<Integer, EntityCollection> declaration =
        new TransformationDeclaration.Builder<>(
            Integer.class, EntityCollection.class).build();
    final TransformationRequest<Number, EntityCollection> d2 = new TransformationRequest<>(
        Number.class, EntityCollection.class);
    assertTrue(declaration.isMatching(d2));
  }

  @Test
  public void testDescriptorOutputSubclassMustMatch() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, Integer> declaration =
        new TransformationDeclaration.Builder<>(
            QueryEntityResult.class, Integer.class).build();
    final TransformationRequest<QueryEntityResult, Number> d2 = new TransformationRequest<>(
        QueryEntityResult.class, Number.class);
    assertTrue(declaration.isMatching(d2));
  }

  @Test
  public void testDescriptorMoreSpecialized() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration.Builder<>(
            QueryEntityResult.class, EntityCollection.class).addConstraintAlternatives(
                RepresentationType.COLLECTION_ENTITY).build();

    final TransformationRequest<QueryEntityResult, EntityCollection> d2 =
        new TransformationRequest<>(
            QueryEntityResult.class, EntityCollection.class,
            RepresentationType.COLLECTION_ENTITY, new ContentTypeComparable(ContentType.APPLICATION_ATOM_XML));
    assertFalse(declaration.isMatching(d2));
  }

  @Test
  public void testDescriptorAlternatives() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration.Builder<>(
            QueryEntityResult.class, EntityCollection.class).addConstraintAlternatives(
                RepresentationType.COLLECTION_ENTITY, RepresentationType.COLLECTION_COMPLEX).addConstraint(
                    new ContentTypeComparable(ContentType.APPLICATION_ATOM_XML)).build();

    final TransformationRequest<QueryEntityResult, EntityCollection> d1 =
        new TransformationRequest<>(
            QueryEntityResult.class, EntityCollection.class,
            RepresentationType.COLLECTION_COMPLEX, new ContentTypeComparable(ContentType.APPLICATION_ATOM_XML));
    assertTrue(declaration.isMatching(d1));

    final TransformationRequest<QueryEntityResult, EntityCollection> d2 =
        new TransformationRequest<>(
            QueryEntityResult.class, EntityCollection.class,
            RepresentationType.COLLECTION_PRIMITIVE, new ContentTypeComparable(ContentType.APPLICATION_ATOM_XML));
    assertFalse(declaration.isMatching(d2));
  }

}
