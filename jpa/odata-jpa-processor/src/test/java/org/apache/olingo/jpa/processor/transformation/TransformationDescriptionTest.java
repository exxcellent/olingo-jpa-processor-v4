package org.apache.olingo.jpa.processor.transformation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.jpa.processor.core.util.TypedParameter;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.junit.Test;

public class TransformationDescriptionTest {

  private final JPAODataRequestContext requestContext = mock(JPAODataRequestContext.class);

  @Test
  public void testDeclarationAndRequestSameMustMatch() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration<>(
            QueryEntityResult.class, EntityCollection.class, new TransformationContextRequirement(
                RepresentationType.class, RepresentationType.COLLECTION_ENTITY));
    assertTrue(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Arrays.asList(
        new TypedParameter(RepresentationType.class, RepresentationType.COLLECTION_ENTITY)), requestContext));
  }

  @Test
  public void testRequestInputSubclassMustMatch() throws IOException, ODataException {
    final TransformationDeclaration<Integer, EntityCollection> declaration =
        new TransformationDeclaration<>(
            Integer.class, EntityCollection.class);
    assertTrue(declaration.isMatching(Number.class, EntityCollection.class, Collections.emptyList(), requestContext));
  }

  @Test
  public void testRequestOutputSubclassMustMatch() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, Integer> declaration =
        new TransformationDeclaration<>(
            QueryEntityResult.class, Integer.class);
    assertTrue(declaration.isMatching(QueryEntityResult.class, Number.class, Collections.emptyList(), requestContext));
  }

  @Test
  public void testDescriptorMoreSpecialized() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration<>(
            QueryEntityResult.class, EntityCollection.class, new TransformationContextRequirement(
                RepresentationType.class, RepresentationType.COLLECTION_ENTITY));

    assertFalse(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Arrays.asList(
        new TypedParameter(RepresentationType.class, RepresentationType.COLLECTION_COMPLEX), new TypedParameter(
            ContentType.class, ContentType.APPLICATION_ATOM_XML)), requestContext));
    assertTrue(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Arrays.asList(
        new TypedParameter(RepresentationType.class, RepresentationType.COLLECTION_ENTITY), new TypedParameter(
            ContentType.class, ContentType.APPLICATION_ATOM_XML)), requestContext));
  }

  @Test
  public void testDeclarationAlternatives() throws IOException, ODataException {
    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration<>(
            QueryEntityResult.class, EntityCollection.class, new TransformationContextRequirement(
                RepresentationType.class, RepresentationType.COLLECTION_ENTITY, RepresentationType.COLLECTION_COMPLEX),
            new TransformationContextRequirement(ContentType.class, ContentType.APPLICATION_ATOM_XML));

    assertTrue(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Arrays.asList(
        new TypedParameter(RepresentationType.class, RepresentationType.COLLECTION_COMPLEX), new TypedParameter(
            ContentType.class, ContentType.APPLICATION_ATOM_XML)), requestContext));

    assertFalse(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Arrays.asList(
        new TypedParameter(RepresentationType.class, RepresentationType.COLLECTION_PRIMITIVE), new TypedParameter(
            ContentType.class, ContentType.APPLICATION_ATOM_XML)), requestContext));
  }

  @Test
  public void testRequestContextComparable() throws IOException, ODataException {
    final DependencyInjectorImpl di = new DependencyInjectorImpl();
    di.registerDependencyMapping(ChronoField.class, ChronoField.ALIGNED_WEEK_OF_YEAR);
    when(requestContext.getDependencyInjector()).thenReturn(di);

    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration<>(
            QueryEntityResult.class, EntityCollection.class,
            new TransformationContextRequirement(ChronoField.class, ChronoField.ALIGNED_WEEK_OF_YEAR));

    assertTrue(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Collections.emptyList(),
        requestContext));
  }

  @Test
  public void testRequestContextObject() throws IOException, ODataException {
    // take any not comparable object
    final LocalDate2UtilCalendarODataAttributeConverter test = new LocalDate2UtilCalendarODataAttributeConverter();
    assert !Comparable.class.isInstance(test);

    final DependencyInjectorImpl di = new DependencyInjectorImpl();
    when(requestContext.getDependencyInjector()).thenReturn(di);

    final TransformationDeclaration<QueryEntityResult, EntityCollection> declaration =
        new TransformationDeclaration<>(
            QueryEntityResult.class, EntityCollection.class,
            new TransformationContextRequirement(LocalDate2UtilCalendarODataAttributeConverter.class, test));

    // before context prepared
    assertFalse(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Collections.emptyList(),
        requestContext));

    di.registerDependencyMapping(LocalDate2UtilCalendarODataAttributeConverter.class, test);
    // after context prepared
    assertTrue(declaration.isMatching(QueryEntityResult.class, EntityCollection.class, Collections.emptyList(),
        requestContext));
  }

}
