package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.chrono.IsoEra;
import java.util.Arrays;

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
import org.apache.olingo.jpa.processor.core.testmodel.dto.RecordAsComplexType;
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
    
    persistenceAdapter.registerDTOComplexType(RecordAsComplexType.class);
    
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
          
          Property pAdditionalRecordComplex = new Property();
          pAdditionalRecordComplex.setName(propertyNameToAddComplex);
          pAdditionalRecordComplex.setType(RecordAsComplexType.class.getName());
          ComplexValue cvRecord =  new ComplexValue();
          Property pRecordNestedBy = new Property();
          pRecordNestedBy.setName("By"); //the property is not dynamic -> use existing property name, type is already known
          pRecordNestedBy.setValue(ValueType.PRIMITIVE, propertyValueComplexBy);
          cvRecord.getValue().add(pRecordNestedBy);
          Property pRecordNestedParameters = new Property();
          pRecordNestedParameters.setName("Parameters"); //the property is not dynamic -> but is open type
          ComplexValue cvNestedParameters =  new ComplexValue();
          pRecordNestedParameters.setValue(ValueType.COMPLEX, cvNestedParameters);
          cvRecord.getValue().add(pRecordNestedParameters);
          //simulate the map entries
          for(int i=0;i<3;i++) {
            Property pComplexNestedMapEntry = new Property();
            pComplexNestedMapEntry.setName("K"+Integer.toString(i));
            pComplexNestedMapEntry.setValue(ValueType.PRIMITIVE, "V"+Integer.toString(i));
            cvNestedParameters.getValue().add(pComplexNestedMapEntry);
          }          
          pAdditionalRecordComplex.setValue(ValueType.COMPLEX, cvRecord);
          e.addProperty(pAdditionalRecordComplex);

          Property pNewPrimitive = new Property();
          pNewPrimitive.setName(propertyNameToAddPrimitive);
          pNewPrimitive.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName().getFullQualifiedNameAsString());
          pNewPrimitive.setValue(ValueType.COLLECTION_PRIMITIVE, Arrays.asList(Integer.valueOf(4), Integer.valueOf(2)));
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
      assertNotNull(org.get(propertyNameToAddEnum));
      assertNotNull(org.get(propertyNameToAddGeospatial));      
      JsonNode nComplexRecord = org.get(propertyNameToAddComplex); 
      assertNotNull(nComplexRecord);
      assertTrue(!nComplexRecord.isArray());
      assertTrue(nComplexRecord.isObject());
      assertEquals("#" + RecordAsComplexType.class.getName(), nComplexRecord.get("@odata.type").asText());
      assertEquals(propertyValueComplexBy, nComplexRecord.get("By").asText());
      ObjectNode pParameters = (ObjectNode) nComplexRecord.get("Parameters");
      assertEquals("V1", pParameters.get("K1").asText());
    }
  }
}
