package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.JPAODataContextAccessDouble;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.util.EdmEntitySetDouble;
import org.apache.olingo.jpa.processor.core.util.EdmEntityTypeDouble;
import org.apache.olingo.jpa.processor.core.util.EdmPropertyDouble;
import org.apache.olingo.jpa.processor.core.util.ExpandItemDouble;
import org.apache.olingo.jpa.processor.core.util.ExpandOptionDouble;
import org.apache.olingo.jpa.processor.core.util.SelectOptionDouble;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.UriInfoDouble;
import org.apache.olingo.jpa.processor.core.util.UriResourceNavigationDouble;
import org.apache.olingo.jpa.processor.core.util.UriResourcePropertyDouble;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.TestDataConstants;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.core.uri.UriResourceComplexPropertyImpl;
import org.apache.olingo.server.core.uri.UriResourceEntitySetImpl;
import org.apache.olingo.server.core.uri.UriResourceValueImpl;
import org.junit.Before;
import org.junit.Test;

public class TestJPAQuerySelectClause extends TestBase {

  private JPAODataRequestContext context;

  @Before
  public void setup() throws ODataException {
    context = new JPAODataContextAccessDouble(
        new JPAEdmProvider(Constant.PUNIT_NAME, persistenceAdapter.getMetamodel()),
        persistenceAdapter);
  }

  @Test
  public void checkSelectAll() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("BusinessPartners"),
        persistenceAdapter
        .createEntityManager(), null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut
        .buildSelectionPathList(
            new UriInfoDouble(new SelectOptionDouble("*"))).determineAllPaths());
    final JPAEntityType jpaEntityType = helper.getJPAEntityType("BusinessPartners");
    assertEquals(jpaEntityType.getPathList().size(), selectClause.size());

  }

  @Test
  public void checkSelectAllWithSelectionNull() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("BusinessPartners"),
        persistenceAdapter
        .createEntityManager(), null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(new UriInfoDouble(
        null)).determineAllPaths());

    final JPAEntityType jpaEntityType = helper.getJPAEntityType("BusinessPartners");
    assertEquals(jpaEntityType.getPathList().size(), selectClause.size());
  }

  @Test
  public void checkSelectExpandViaIgnoredProperties() throws ODataApplicationException, ODataJPAModelException {
    // Organizations('3')/Address?$expand=AdministrativeDivision
    final List<ExpandItem> expItems = new ArrayList<ExpandItem>();
    final EdmEntityType startEntity = new EdmEntityTypeDouble(nameBuilder, "Organization");
    final EdmEntityType targetEntity = new EdmEntityTypeDouble(nameBuilder, "AdministrativeDivision");

    final ExpandOption expOps = new ExpandOptionDouble("AdministrativeDivision", expItems);
    expItems.add(new ExpandItemDouble(targetEntity));
    final List<UriResource> startResources = new ArrayList<UriResource>();
    final UriInfoDouble uriInfo = new UriInfoDouble(null);
    uriInfo.setExpandOpts(expOps);
    uriInfo.setUriResources(startResources);

    startResources.add(new UriResourceNavigationDouble(startEntity));
    startResources.add(new UriResourcePropertyDouble(new EdmPropertyDouble("Address")));

    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("BusinessPartners"),
        persistenceAdapter
            .createEntityManager(), null);
    final List<Selection<?>> selectClause = cut
        .createSelectClause(cut.buildSelectionPathList(uriInfo).determineAllPaths());

    assertContains(selectClause, "Address/StreetName");
    assertContainsNot(selectClause, "Address/RegionCodeID");// ignored
  }

  @Test
  public void checkSelectOnePropertyCreatedAt() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("BusinessPartners"),
        persistenceAdapter
            .createEntityManager(), null);
    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("CreationDateTime"))).determineAllPaths());
    assertEquals(2, selectClause.size());
    assertContains(selectClause, "CreationDateTime");
    assertContains(selectClause, "ID");
  }

  @Test
  public void checkSelectOnePropertyID() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("BusinessPartners"),
        persistenceAdapter
            .createEntityManager(), null);
    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("ID"))).determineAllPaths());
    assertEquals(1, selectClause.size());
    assertContains(selectClause, "ID");
  }

  @Test
  public void checkSelectOnePropertyPartKey() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo(
        "AdministrativeDivisionDescriptions"),
        persistenceAdapter.createEntityManager(), null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble((new SelectOptionDouble("CodePublisher")))).determineAllPaths());
    assertEquals(4, selectClause.size());
    assertContains(selectClause, "CodePublisher");
    assertContains(selectClause, "CodeID");
    assertContains(selectClause, "DivisionCode");
    assertContains(selectClause, "Language");
  }

  @Test
  public void checkSelectPropertyTypeCreatedAt() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("BusinessPartners"),
        persistenceAdapter
            .createEntityManager(), null);
    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("Type,CreationDateTime"))).determineAllPaths());

    assertEquals(3, selectClause.size());
    assertContains(selectClause, "CreationDateTime");
    assertContains(selectClause, "Type");
    assertContains(selectClause, "ID");
  }

  @Test
  public void checkSelectSupertypePropertyTypeName2() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(), null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("Type,Name2"))).determineAllPaths());
    assertContains(selectClause, "Name2");
    assertContains(selectClause, "Type");
    assertContains(selectClause, "ID");
    assertEquals(3, selectClause.size());
  }

  @Test
  public void checkSelectCompleteComplexType() throws ODataApplicationException, ODataJPAModelException {
    // Organizations$select=Address
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(),
        null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("Address"))).determineAllPaths());
    assertContains(selectClause, "Address/Region");
    assertContains(selectClause, "ID");
    assertEquals(TestDataConstants.NO_ATTRIBUTES_POSTAL_ADDRESS + 1, selectClause.size());
  }

  @Test
  public void checkSelectCompleteNestedComplexTypeLowLevel() throws ODataApplicationException, ODataJPAModelException {
    // Organizations$select=Address
    final EntityQueryBuilder cut = new EntityQueryBuilder(context,
        createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(), null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("AdministrativeInformation/Created"))).determineAllPaths());
    assertEquals(3, selectClause.size());
    assertContains(selectClause, "AdministrativeInformation/Created/By");
    assertContains(selectClause, "AdministrativeInformation/Created/At");
    assertContains(selectClause, "ID");
  }

  @Test
  public void checkSelectCompleteNestedComplexTypeHighLevel() throws ODataApplicationException, ODataJPAModelException {
    // Organizations$select=Address
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(),
        null);

    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("AdministrativeInformation"))).determineAllPaths());
    assertEquals(5, selectClause.size());
    assertContains(selectClause, "AdministrativeInformation/Created/By");
    assertContains(selectClause, "AdministrativeInformation/Created/At");
    assertContains(selectClause, "AdministrativeInformation/Updated/By");
    assertContains(selectClause, "AdministrativeInformation/Updated/At");
    assertContains(selectClause, "ID");
  }

  @Test
  public void checkSelectElementOfComplexType() throws ODataApplicationException, ODataJPAModelException {
    // Organizations$select=Address/Country
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(),
        null);

    // SELECT c.address.geocode FROM Company c WHERE c.name = 'Random House'
    final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
        new UriInfoDouble(new SelectOptionDouble("Address/Country"))).determineAllPaths());
    assertContains(selectClause, "Address/Country");
    assertContains(selectClause, "ID");
    assertEquals(2, selectClause.size());
  }

  @Test(expected = ODataJPAQueryException.class)
  public void checkInvalidSelectedAttribute() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(),
        null);

    cut.buildSelectionPathList(new UriInfoDouble(new SelectOptionDouble("Address/CountryName")));
  }

  @Test
  public void checkSelectStreamValueStatic() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("PersonImages"), persistenceAdapter
        .createEntityManager(),
        null);

    final UriInfoDouble uriInfo = new UriInfoDouble(new SelectOptionDouble("Address"));// TODO useless?
    final List<UriResource> uriResources = new ArrayList<UriResource>();
    uriInfo.setUriResources(uriResources);
    uriResources.add(new UriResourceEntitySetImpl(new EdmEntitySetDouble(nameBuilder, "PersonImages")));
    uriResources.add(new UriResourceValueImpl());

    final List<Selection<?>> selectClause = cut
        .createSelectClause(cut.buildSelectionPathList(uriInfo).determineAllPaths());
    assertNotNull(selectClause);
    assertContains(selectClause, "Image");
    assertContains(selectClause, "PID");
  }

  @Test
  public void checkSelectStreamValueDynamic() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context,
        createTestUriInfo("OrganizationImages"),
        persistenceAdapter
        .createEntityManager(),
        null);

    final UriInfoDouble uriInfo = new UriInfoDouble(new SelectOptionDouble("Address"));// TODO useless?
    final List<UriResource> uriResources = new ArrayList<UriResource>();
    uriInfo.setUriResources(uriResources);
    uriResources.add(new UriResourceEntitySetImpl(new EdmEntitySetDouble(nameBuilder, "OrganizationImages")));
    uriResources.add(new UriResourceValueImpl());

    final List<Selection<?>> selectClause = cut
        .createSelectClause(cut.buildSelectionPathList(uriInfo).determineAllPaths());
    assertNotNull(selectClause);
    assertContains(selectClause, "Image");
    assertContains(selectClause, "MimeType");
    assertContains(selectClause, "ID");
  }

  @Test
  public void checkSelectPropertyValue() throws ODataApplicationException, ODataJPAModelException {
    final EntityQueryBuilder cut = new EntityQueryBuilder(context, createTestUriInfo("Organizations"),
        persistenceAdapter.createEntityManager(),
        null);

    final UriInfoDouble uriInfo = new UriInfoDouble(new SelectOptionDouble("Address"));// TODO useless?
    final List<UriResource> uriResources = new ArrayList<UriResource>();
    uriInfo.setUriResources(uriResources);
    // BusinessPartnerImages('99')/AdministrativeInformation/Created/By/$value
    uriResources.add(new UriResourceEntitySetImpl(new EdmEntitySetDouble(nameBuilder, "Organizations")));
    uriResources.add(new UriResourceComplexPropertyImpl(new EdmPropertyDouble("AdministrativeInformation")));
    uriResources.add(new UriResourceComplexPropertyImpl(new EdmPropertyDouble("Created")));
    uriResources.add(new UriResourcePropertyDouble(new EdmPropertyDouble("By")));
    uriResources.add(new UriResourceValueImpl());

    final List<Selection<?>> selectClause = cut
        .createSelectClause(cut.buildSelectionPathList(uriInfo).determineAllPaths());
    assertNotNull(selectClause);
    assertContains(selectClause, "AdministrativeInformation/Created/By");
    assertContains(selectClause, "ID");
  }

  private void assertContains(final List<Selection<?>> selectClause, final String alias) {
    for (final Selection<?> selection : selectClause) {
      if (selection.getAlias().equals(alias)) {
        return;
      }
    }
    fail(alias + " not found");
  }

  private void assertContainsNot(final List<Selection<?>> selectClause, final String alias) {
    for (final Selection<?> selection : selectClause) {
      if (selection.getAlias().equals(alias)) {
        fail(alias + " found");
      }
    }
  }

}
