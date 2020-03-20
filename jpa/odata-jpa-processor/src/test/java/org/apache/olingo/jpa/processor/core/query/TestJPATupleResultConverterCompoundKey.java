package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TupleDouble;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Test;

public class TestJPATupleResultConverterCompoundKey extends TestBase {

  @Test
  public void checkConvertsOneResultsTwoKeys() throws ODataApplicationException, ODataJPAModelException {
    // .../BusinessPartnerRoles(BusinessPartnerID='3',RoleCategory='C')
    final List<Tuple> tupleResult = new ArrayList<Tuple>();

    final HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("BusinessPartnerID", new String("3"));
    result.put("RoleCategory", new String("C"));
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult queryResult = new QueryEntityResult(tupleResult,
        helper.getJPAEntityType("BusinessPartnerRoles"));
    final DatabaseQueryResult2ODataEntityConverter cut = new DatabaseQueryResult2ODataEntityConverter(
        helper.getServiceDocument(),
        odata.createUriHelper(),
        serviceMetaData);

    final EntityCollection act = cut.convertDBTuple2OData(queryResult);
    assertEquals(1, act.getEntities().size());
    assertEquals("3", act.getEntities().get(0).getProperty("BusinessPartnerID").getValue().toString());
    assertEquals("C", act.getEntities().get(0).getProperty("RoleCategory").getValue().toString());

    assertEquals("BusinessPartnerRoles(BusinessPartnerID='3',RoleCategory='C')",
        act.getEntities().get(0).getId().getPath());
  }

  @Test // EmbeddedIds are resolved to elementary key properties
  public void checkConvertsOneResultsEmbeddedKey() throws ODataApplicationException, ODataJPAModelException {
    // .../AdministrativeDivisionDescriptions(CodePublisher='ISO', CodeID='3166-1', DivisionCode='DEU',Language='en')
    final List<Tuple> tupleResult = new ArrayList<Tuple>();

    final HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("CodePublisher", new String("ISO"));
    result.put("CodeID", new String("3166-1"));
    result.put("DivisionCode", new String("DEU"));
    result.put("Language", new String("en"));
    tupleResult.add(new TupleDouble(result));

    final QueryEntityResult queryResult = new QueryEntityResult(tupleResult,
        helper.getJPAEntityType("AdministrativeDivisionDescriptions"));
    final DatabaseQueryResult2ODataEntityConverter cut = new DatabaseQueryResult2ODataEntityConverter(
        helper.getServiceDocument(), odata.createUriHelper(), serviceMetaData);


    final EntityCollection act = cut.convertDBTuple2OData(queryResult);
    assertEquals(1, act.getEntities().size());
    assertEquals("ISO", act.getEntities().get(0).getProperty("CodePublisher").getValue().toString());
    assertEquals("en", act.getEntities().get(0).getProperty("Language").getValue().toString());

    assertEquals(
        "AdministrativeDivisionDescriptions(DivisionCode='DEU',CodeID='3166-1',CodePublisher='ISO',Language='en')",
        act.getEntities().get(0).getId().toASCIIString());
  }

}
