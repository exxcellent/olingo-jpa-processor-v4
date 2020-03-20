package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TupleDouble;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriHelper;
import org.junit.Before;
import org.junit.Test;

public class TestJPATupleResultConverter extends TestBase {

  private static final int NO_POSTAL_ADDRESS_FIELDS = 7; // TODO 8
  private static final int NO_ADMIN_INFO_FIELDS = 2;
  private DatabaseQueryResult2ODataEntityConverter cut;
  private UriHelper uriHelper;
  private TestHelper helper;

  @Before
  public void setup() throws ODataException {
    /* final UriHelper */ uriHelper = odata.createUriHelper();
    cut = new DatabaseQueryResult2ODataEntityConverter(helper.getServiceDocument(), uriHelper, serviceMetaData);
  }

  @Test
  public void checkConvertsEmptyResult() throws Exception {
    final QueryEntityResult jpaQueryResult = new QueryEntityResult(Collections.emptyList(), helper.getJPAEntityType(
        "Organizations"));
    assertNotNull(cut.convertDBTuple2OData(jpaQueryResult));
  }

  @Test
  public void checkConvertsOneResultOneElement() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    final HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("ID", new String("1"));
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(1, act.getEntities().size());
    assertEquals("1", act.getEntities().get(0).getProperty("ID").getValue().toString());
  }

  @Test
  public void checkConvertsOneResultOneKey() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    final HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("ID", new String("1"));
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(1, act.getEntities().size());
    assertEquals("Organizations" + "('1')", act.getEntities().get(0).getId().getPath());
  }

  @Test
  public void checkConvertsTwoResultsOneElement() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    HashMap<String, Object> result;

    result = new HashMap<String, Object>();
    result.put("ID", new String("1"));
    tupleResult.add(new TupleDouble(result));

    result = new HashMap<String, Object>();
    result.put("ID", new String("5"));
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(2, act.getEntities().size());
    assertEquals("1", act.getEntities().get(0).getProperty("ID").getValue().toString());
    assertEquals("5", act.getEntities().get(1).getProperty("ID").getValue().toString());
  }

  @Test
  public void checkConvertsOneResultsTwoElements() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    final HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("ID", new String("1"));
    result.put("Name1", new String("Willi"));
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(1, act.getEntities().size());
    assertEquals("1", act.getEntities().get(0).getProperty("ID").getValue().toString());
    assertEquals("Willi", act.getEntities().get(0).getProperty("Name1").getValue().toString());
  }

  @Test
  public void checkConvertsOneResultsOneComplexElement() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    final HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("ID", "1");
    result.put("Address/CityName", "Test City");
    result.put("Address/Country", "GB");
    result.put("Address/PostalCode", "ZE1 3AA");
    result.put("Address/StreetName", "Test Road");
    result.put("Address/HouseNumber", "123");
    result.put("Address/POBox", "155");
    result.put("Address/Region", "GB-12");
    result.put("Address/CountryName", "Willi");
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(1, act.getEntities().size());

    assertEquals(ValueType.COMPLEX, act.getEntities().get(0).getProperty("Address").getValueType());
    final ComplexValue value = (ComplexValue) act.getEntities().get(0).getProperty("Address").getValue();
    assertEquals(NO_POSTAL_ADDRESS_FIELDS, value.getValue().size());
  }

  @Test
  public void checkConvertsOneResultsOneNestedComplexElement() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    HashMap<String, Object> result;

    // AdministrativeInformation adminInfo = new AdministrativeInformation();
    // adminInfo.setCreated(new ChangeInformation("Joe Doe", Timestamp.valueOf("2016-01-22 12:25:23")));
    // adminInfo.setUpdated(new ChangeInformation("Joe Doe", Timestamp.valueOf("2016-01-24 14:29:45")));
    result = new HashMap<String, Object>();
    result.put("ID", "1");
    result.put("AdministrativeInformation/Created/By", "Joe Doe");
    result.put("AdministrativeInformation/Created/At", "2016-01-22 12:25:23");
    result.put("AdministrativeInformation/Updated/By", "Joe Doe");
    result.put("AdministrativeInformation/Updated/At", "2016-01-24 14:29:45");
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(1, act.getEntities().size());
    // Check first level
    assertEquals(ValueType.COMPLEX, act.getEntities().get(0).getProperty("AdministrativeInformation").getValueType());
    final ComplexValue value = (ComplexValue) act.getEntities().get(0).getProperty("AdministrativeInformation").getValue();
    assertEquals(NO_ADMIN_INFO_FIELDS, value.getValue().size());
    // Check second level
    assertEquals(ValueType.COMPLEX, value.getValue().get(0).getValueType());
  }

  @Test
  public void checkConvertsOneResultsOneElementOfComplexElement() throws Exception {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    final HashMap<String, Object> entityResult = new HashMap<String, Object>();
    entityResult.put("ID", "1");
    entityResult.put("Address/Region", new String("CA"));
    tupleResult.add(new TupleDouble(entityResult));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "Organizations"));
    final EntityCollection act = cut.convertDBTuple2OData(jpaQueryResult);
    assertEquals(1, act.getEntities().size());
    assertEquals("CA", ((ComplexValue) act.getEntities().get(0).getProperty("Address").getValue()).getValue().get(0)
        .getValue().toString());
  }

  @Test
  public void checkConvertMediaStreamStaticMime() throws ODataJPAModelException, NumberFormatException,
  ODataApplicationException {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();

    //    uriHelper.setKeyPredicates(keyPredicates, "PID");
    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "PersonImages"));
    final DatabaseQueryResult2ODataEntityConverter converter = new DatabaseQueryResult2ODataEntityConverter(
        helper.getServiceDocument(), uriHelper, serviceMetaData);

    final HashMap<String, Object> entityResult = new HashMap<String, Object>();
    entityResult.put("PID", "1");
    final byte[] image = { -119, 10 };
    entityResult.put("Image", image);
    tupleResult.add(new TupleDouble(entityResult));

    final EntityCollection act = converter.convertDBTuple2OData(jpaQueryResult);
    assertEquals("image/png", act.getEntities().get(0).getMediaContentType());
  }

  @Test
  public void checkConvertMediaStreamDynamicMime() throws ODataJPAModelException, NumberFormatException,
  ODataApplicationException {
    final List<Tuple> tupleResult = new ArrayList<Tuple>();
    final HashMap<String, Object> entityResult = new HashMap<String, Object>();
    entityResult.put("ID", "9");
    final byte[] image = { -119, 10 };
    entityResult.put("Image", image);
    entityResult.put("MimeType", "image/svg+xml");
    tupleResult.add(new TupleDouble(entityResult));

    final QueryEntityResult jpaQueryResult = new QueryEntityResult(tupleResult, helper.getJPAEntityType(
        "OrganizationImages"));
    final DatabaseQueryResult2ODataEntityConverter converter = new DatabaseQueryResult2ODataEntityConverter(
        helper.getServiceDocument(), uriHelper, serviceMetaData);

    final EntityCollection act = converter.convertDBTuple2OData(jpaQueryResult);
    assertEquals("image/svg+xml", act.getEntities().get(0).getMediaContentType());
    assertEquals(2, act.getEntities().get(0).getProperties().size());
  }
}
