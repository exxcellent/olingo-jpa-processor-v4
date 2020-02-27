package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;
import org.junit.Before;
import org.junit.Test;

public class TestJPAInstanceResultConverter extends TestBase {
  private JPAInstanceResultConverter cut;
  private List<Object> jpaQueryResult;

  @Before
  public void setup() throws ODataException {
    final UriHelper uriHelper = odata.createUriHelper();
    final EdmEntitySet edmEntitySet = serviceMetaData.getEdm().getEntityContainer().getEntitySet(
        "AdministrativeDivisions");

    jpaQueryResult = new ArrayList<Object>();

    cut = new JPAInstanceResultConverter(uriHelper, helper.getEdmProvider().getServiceDocument(), jpaQueryResult,
        edmEntitySet,
        AdministrativeDivision.class);
  }

  @Test
  public void checkConvertsEmptyResult() throws ODataApplicationException, SerializerException, URISyntaxException {
    assertNotNull(cut.getResult());
  }

  @Test
  public void checkConvertsOneResult() throws ODataApplicationException, SerializerException, URISyntaxException {
    final AdministrativeDivision division = firstResult();

    jpaQueryResult.add(division);

    final EntityCollection act = cut.getResult();
    assertEquals(1, act.getEntities().size());
  }

  @Test
  public void checkConvertsTwoResult() throws ODataApplicationException, SerializerException, URISyntaxException {

    jpaQueryResult.add(firstResult());
    jpaQueryResult.add(secondResult());
    final EntityCollection act = cut.getResult();
    assertEquals(2, act.getEntities().size());
  }

  @Test
  public void checkConvertsOneResultOneElement() throws ODataApplicationException, SerializerException,
  URISyntaxException {
    final AdministrativeDivision division = firstResult();

    jpaQueryResult.add(division);

    final EntityCollection act = cut.getResult();
    assertEquals(1, act.getEntities().size());
    assertEquals("BE21", act.getEntities().get(0).getProperty("DivisionCode").getValue().toString());

  }

  @Test
  public void checkConvertsOneResultMultiElement() throws ODataApplicationException, SerializerException,
  URISyntaxException {
    final AdministrativeDivision division = firstResult();

    jpaQueryResult.add(division);

    final EntityCollection act = cut.getResult();
    assertEquals(1, act.getEntities().size());
    assertEquals("BE21", act.getEntities().get(0).getProperty("DivisionCode").getValue().toString());
    assertEquals("BE2", act.getEntities().get(0).getProperty("ParentDivisionCode").getValue().toString());
    assertEquals("0", act.getEntities().get(0).getProperty("Population").getValue().toString());
  }

  AdministrativeDivision firstResult() {
    final AdministrativeDivision division = new AdministrativeDivision();

    division.setCodePublisher("Eurostat");
    division.setCodeID("NUTS2");
    division.setDivisionCode("BE21");
    division.setCountryCode("BEL");
    division.setParentCodeID("NUTS1");
    division.setParentDivisionCode("BE2");
    division.setAlternativeCode("");
    division.setArea(0);
    division.setPopulation(0);
    return division;
  }

  private Object secondResult() {
    final AdministrativeDivision division = new AdministrativeDivision();

    division.setCodePublisher("Eurostat");
    division.setCodeID("NUTS2");
    division.setDivisionCode("BE22");
    division.setCountryCode("BEL");
    division.setParentCodeID("NUTS1");
    division.setParentDivisionCode("BE2");
    division.setAlternativeCode("");
    division.setArea(0);
    division.setPopulation(0);
    return division;
  }
}
