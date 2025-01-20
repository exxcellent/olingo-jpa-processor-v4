package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.chrono.IsoEra;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.geo.Geospatial.Dimension;
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.QueryRequestCustomizer;
import org.apache.olingo.jpa.processor.core.api.QueryResponseCustomizer;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.server.api.uri.UriResourceCount;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestCustomizer extends TestBase {

  @Test
  public void testEntityAndElementCollectionAndExpandBuilder() throws IOException, ODataException {

    final boolean[] wasCalled = new boolean[] { false };
    final QueryRequestCustomizer testCustomizer = new QueryRequestCustomizer() {

      @Override
      public void customizeQuery(final QueryCustomization context, final NavigationIfc queryScope) {
        if (!context.getStartTypeClass().equals(Person.class)) {
          return;
        }
        final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
        final Subquery<Integer> sq = context.createSubquery(Integer.class);
        final Root<Person> from = sq.from(Person.class);
        sq.select(cb.literal(Integer.valueOf(1)));
        sq.where(cb.equal(from.get("firstName"), context.getStartFrom().get("firstName")), cb.equal(from.get(
            "firstName"),
            "Max"));
        wasCalled[0] = true;
        context.withWhereClause(cb.exists(sq));
      }
    };

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").expand("*");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {
      @Override
      protected void modifyRequestContext(final ModifiableJPAODataRequestContext requestContext) {
        super.modifyRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(QueryRequestCustomizer.class, testCustomizer);
      }
    };
    helper.setRequestedResponseContentType(ContentType.JSON_FULL_METADATA.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode result = helper.getJsonObjectValues();
    assertTrue(wasCalled[0]);
    assertEquals(1, result.size());

  }

  @Test
  public void testCountBuilder() throws IOException, ODataException {

    final boolean[] wasCalled = new boolean[] { false };
    final QueryRequestCustomizer testCustomizer = new QueryRequestCustomizer() {

      @Override
      public void customizeQuery(final QueryCustomization context, final NavigationIfc queryScope) {
        if (queryScope.getUriResourceParts().size() != 2 || !(queryScope.getUriResourceParts().get(
            1) instanceof UriResourceCount)) {
          return;
        }
        final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
        final Subquery<Integer> sq = context.createSubquery(Integer.class);
        final Root<Person> from = sq.from(Person.class);
        sq.select(cb.literal(Integer.valueOf(1)));
        final Predicate or = cb.or(cb.equal(from.get("firstName"), "Max"), cb.equal(from.get("firstName"), "John"));
        sq.where(cb.equal(from.get("firstName"), context.getEndFrom().get("firstName")), or);
        wasCalled[0] = true;
        context.withWhereClause(cb.exists(sq));
      }
    };
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendCountSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {
      @Override
      protected void modifyRequestContext(final ModifiableJPAODataRequestContext requestContext) {
        super.modifyRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(QueryRequestCustomizer.class, testCustomizer);
      }
    };
    helper.setRequestedResponseContentType(ContentType.JSON_FULL_METADATA.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final long result = Long.valueOf(helper.getRawResult()).longValue();
    assertTrue(wasCalled[0]);
    assertEquals(2, result);

  }

  @Test
  public void testResponseCustomizer() throws IOException, ODataException {
    final String propertyNameToRemove = "AdministrativeInformation";
    final String propertyNameToAddPrimitive = "ResponseModifiedAddedPropertyPrimitive";
    final String propertyNameToAddComplex = "ResponseModifiedAddedPropertyComplex";
    final String propertyValueComplexBy = "blob";
    final String propertyNameToAddEnum = "ResponseModifiedAddedPropertyEnum";
    final String propertyNameToAddGeospatial = "ResponseModifiedAddedPropertyGeo";
    QueryResponseCustomizer testCustomizer = new QueryResponseCustomizer() {
      @Override
      public EntityCollection customizeResult(Edm edm, Class<?> entityType, EntityCollection result) {
        assertEquals(entityType, Organization.class);
        for(Entity e: result.getEntities()) {
          //modify every single organization by adding properties and removing a value for another one
          Property pRemove = e.getProperty(propertyNameToRemove);
          assertNotNull(pRemove);
          int sizeBefore = e.getProperties().size();
          e.getProperties().remove(pRemove);
          assertEquals(e.getProperties().size(), sizeBefore - 1);
          
          Property pNewComplex = new Property();
          pNewComplex.setName(propertyNameToAddComplex);
          pNewComplex.setType("org.apache.olingo.jpa.ChangeInformation");
          ComplexValue cNew =  new ComplexValue();
          Property pComplexNested = new Property();
          pComplexNested.setName("By"); //the property is not dynamic -> use existing property name, but type is already known
          pComplexNested.setValue(ValueType.PRIMITIVE, propertyValueComplexBy);
          cNew.getValue().add(pComplexNested);
          pNewComplex.setValue(ValueType.COLLECTION_COMPLEX, List.of(cNew));
          e.addProperty(pNewComplex);

          Property pNewPrimitive = new Property();
          pNewPrimitive.setName(propertyNameToAddPrimitive);
          pNewPrimitive.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName().getFullQualifiedNameAsString());
          pNewPrimitive.setValue(ValueType.COLLECTION_PRIMITIVE, List.of(Integer.valueOf(4), Integer.valueOf(2)));
          e.addProperty(pNewPrimitive);
                    
          Property pNewGeospatial = new Property();
          pNewGeospatial.setName(propertyNameToAddGeospatial);
          pNewGeospatial.setType(EdmPrimitiveTypeKind.Geometry.getFullQualifiedName().getFullQualifiedNameAsString());
          pNewGeospatial.setValue(ValueType.GEOSPATIAL, new org.apache.olingo.commons.api.edm.geo.Point(Dimension.GEOMETRY, SRID.valueOf("variable")));
          e.addProperty(pNewGeospatial);
          
          Property pNewEnum = new Property();
          pNewEnum.setName(propertyNameToAddEnum);
          pNewEnum.setType(edm.getEnumType(new FullQualifiedName(IsoEra.class.getName())).getFullQualifiedName().getFullQualifiedNameAsString());//simply the full qualified java class name
          pNewEnum.setValue(ValueType.ENUM, IsoEra.BCE.ordinal()); //enum values are always handled with ordinal (Int32 as underlying type)
          e.addProperty(pNewEnum);
        }
        return result;
      }
    };
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {
      @Override
      protected void modifyRequestContext(final ModifiableJPAODataRequestContext requestContext) {
        super.modifyRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(QueryResponseCustomizer.class, testCustomizer);
      }
    };
    helper.setRequestedResponseContentType(ContentType.JSON_FULL_METADATA.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode result = helper.getJsonObjectValues();
    for(int i=0;i<result.size();i++) {
      JsonNode org = result.get(i);
      assertTrue( org.get(propertyNameToRemove).isNull());//null node (present, but without value)
      assertNotNull( org.get(propertyNameToAddPrimitive));
      JsonNode nComplex = org.get(propertyNameToAddComplex); 
      assertNotNull(nComplex);
      assertTrue(nComplex.isArray());
      ObjectNode cCI = (ObjectNode) ((ArrayNode)nComplex).get(0);
      assertNotNull(cCI);
      assertEquals("#org.apache.olingo.jpa.ChangeInformation", cCI.get("@odata.type").asText());
      assertEquals(propertyValueComplexBy, cCI.get("By").asText());
      assertNotNull(org.get(propertyNameToAddEnum));
      assertNotNull(org.get(propertyNameToAddGeospatial));
    }
  }
}
