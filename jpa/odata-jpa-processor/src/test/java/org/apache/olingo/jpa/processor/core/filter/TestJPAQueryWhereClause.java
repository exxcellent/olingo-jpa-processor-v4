package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPAQueryWhereClause extends TestBase {

  //  @Override
  //  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
  //    // H2 has problems with java.time.LocalDate
  //    // HSLQDB has problem with substring queries
  //    // DERBY has problems with boolean as int queries (default of Eclipselink)
  //    return new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
  //        DataSourceHelper.DatabaseType.DERBY);
  //  }

  @Test
  public void testFilterLeftEqualsNullValue() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "AlternativeCode eq null");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertTrue(orgs.size() > 0);
  }

  @Test
  public void testFilterNullEqualsRightValue() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "null eq AlternativeCode");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertTrue(orgs.size() > 0);
  }

  @Test
  public void testFilterLeftNotEqualsNullValue() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "ParentDivisionCode ne null");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertTrue(orgs.size() > 0);
  }

  @Test
  public void testFilterNullNotEqualsRightValue() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "null ne ParentDivisionCode");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertTrue(orgs.size() > 0);
  }

  @Test
  public void testFilterOneEquals() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter("ID eq '3'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
    assertEquals("3", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneDescriptionEquals() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter("Country eq 'DEU'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
    assertEquals("10", orgs.get(0).get("ID").asText());
  }

  @Disabled("LocationName currently not available")
  @Test
  public void testFilterOneDescriptionEqualsFieldNotSelected() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "LocationName eq 'Deutschland'").select("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
    assertEquals("10", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneEqualsTwoProperties() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "DivisionCode eq CountryCode");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterOneEqualsInvert() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter("'3' eq ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
    assertEquals("3", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneNotEqual() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter("ID ne '3'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(9, orgs.size());
  }

  @Test
  public void testFilterOneGreaterEqualsString() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter("ID ge '5'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(5, orgs.size()); // '10' is smaller than '5' when comparing strings!
  }

  @Test
  public void testFilterOneLowerThanTwoProperties() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "DivisionCode lt CountryCode");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(228, orgs.size());
  }

  @Test
  public void testFilterOneGreaterThanString() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "ID gt '5'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(4, orgs.size()); // '10' is smaller than '5' when comparing strings!
  }

  @Test
  public void testFilterOneLowerThanString() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "ID lt '5'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(5, orgs.size());
  }

  @Test
  public void testFilterOneLowerEqualsString() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "ID le '5'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(6, orgs.size());
  }

  @Test
  public void testFilterOneGreaterEqualsNumber() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Area ge 119330610");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterOneAndEquals() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "CodePublisher eq 'Eurostat' and CodeID eq 'NUTS2'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(11, orgs.size());
  }

  @Test
  public void testFilterOneOrEquals() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "ID eq '5' or ID eq '10'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterOneNotLower() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "not (Area lt 50000000)");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(24, orgs.size());
  }

  @Test
  public void testFilterTwoAndEquals() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "CodePublisher eq 'Eurostat' and CodeID eq 'NUTS2' and DivisionCode eq 'BE25'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
    assertEquals("BEL", orgs.get(0).get("CountryCode").asText());
  }

  @Test
  public void testFilterAndOrEqualsParenthesis() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "CodePublisher eq 'Eurostat' and (DivisionCode eq 'BE25' or  DivisionCode eq 'BE24')&$orderby=DivisionCode desc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
    assertEquals("BE25", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAndOrEqualsNoParenthesis() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "CodePublisher eq 'Eurostat' and DivisionCode eq 'BE25' or  CodeID eq '3166-1'&$orderby=DivisionCode desc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(5, orgs.size());
    assertEquals("USA", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAddGreater() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Area add 7000000 ge 50000000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(31, orgs.size());
  }

  @Test
  public void testFilterSubGreater() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Area sub 7000000 ge 60000000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(15, orgs.size());
  }

  @Test
  public void testFilterDivGreater() throws IOException, ODataException {

    assumeTrue(getJPAProvider() != JPAProvider.Hibernate, "Hibernate cannot compare a Short (from 6000) as Number");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Area gt 0 and Area div Population ge 6000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(9, orgs.size());
  }

  @Test
  public void testFilterMulGreater() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Area mul Population gt 0");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(64, orgs.size());
  }

  @Test
  public void testFilterMod() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Area gt 0 and Area mod 3578335 eq 0");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterLength() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "length(Name) eq 10");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(11, orgs.size());
  }

  @Test
  public void testFilterNow() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").filter(
        "AdministrativeInformation/Created/At lt now()");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterContains() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "contains(CodeID,'166')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(110, orgs.size());
  }

  @Test
  public void testFilterEndswith() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "endswith(CodeID,'166-1')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterStartswith() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "startswith(DivisionCode,'DE-')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(16, orgs.size());
  }

  @Test
  public void testFilterIndexOf() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "indexof(DivisionCode,'3') eq 4");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(7, orgs.size());
  }

  @Test
  public void testFilterSubstringStartIndex() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and length(Name) gt 6 and substring(Name,6) eq 'Dakota'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterSubstringStartEndIndex() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and substring(Name,0,5) eq 'North'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterSubstringLengthCalculated() throws IOException, ODataException {
    // substring(CompanyName, 1 add 4, 2 mul 3)
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and substring(Name,0,1 add 4) eq 'North'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterSubstringStartCalculated() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and substring(Name, cast(2 mul 3, Edm.Int32)) eq 'Dakota'"); // North/South Dakota
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterToLower() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and tolower(Name) eq 'brandenburg'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterToUpper() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and toupper(Name) eq 'HESSEN'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterToUpperInvers() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "toupper('nuts1') eq CodeID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(3, orgs.size());
  }

  @Test
  public void testFilterTrim() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and trim(Name) eq 'Sachsen'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterConcat() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").filter(
        "concat(concat(LastName,','),FirstName) eq 'Mustermann,Max'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterBoolean1() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("CountryEntitySet").filter(
        "contains(Code,'H') and startswith(Name, 'S') and not endswith(Name, 'xyz')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertTrue(orgs.size() > 0);
  }

  @Test
  public void testFilterBoolean2() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("CountryEntitySet").filter(
        "length(Code) gt 1 and startswith( substring(Name,0,3), 'S')").top(3);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertTrue(orgs.size() > 0);
  }

  @Test
  public void testNavigationFilter() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities").filter(
        "targets/any(d:contains(d/Name, 'rel'))");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getJsonObjectValues();
    assertTrue(relSources.size() == 2);
    assertTrue(relSources.get(0).get("targets") == null);// AsIs
    assertTrue(relSources.get(0).get("Targets") == null);// upperCamelCase (not valid)
  }

  @Test
  public void testNavigationFilterElementCollectionWithoutNestedElements() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("BusinessPartners").select("ID", "Country")
        .filter("PhoneNumbers/any(d:d/internationalAreaCode eq '+42')").top(4);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getJsonObjectValues();
    assertTrue(relSources.size() == 1);
    assertEquals("USA", relSources.get(0).get("Country").asText());
  }

  @Test
  public void testNavigationFilterElementCollectionForCompleteBO() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("BusinessPartners").filter(
        "PhoneNumbers/any(d:d/internationalAreaCode eq '+42')").top(4);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getJsonObjectValues();
    assertTrue(relSources.size() == 1);
    assertTrue(relSources.get(0).withArray("PhoneNumbersAsString").size() == 2);
    assertTrue(relSources.get(0).withArray("PhoneNumbers").size() == 2);
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAny() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "Roles/any(d:d/RoleCategory eq 'A')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(3, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAnyMultiParameter() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID").filter(
        "Roles/any(d:d/RoleCategory eq 'A' and d/BusinessPartnerID eq '1')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterLambdaAllOperatorAfterBooleanOperatorExpression() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "Name1 ne 'foo' and Roles/all(d:d/RoleCategory eq 'A')").select("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  /**
   * Services MUST support case-insensitive operator names
   *
   * @see https://docs.oasis-open.org/odata/odata/v4.01/odata-v4.01-part2-url-conventions.html#_Toc26179939
   */
  @Disabled("With Olingo 4.7.0 operators are case sensitive (must be lower case)")
  @Test
  public void testCaseInsensitiveFilterOperators() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "Name1 ne 'foo' AND Name1 ne 'bar'").select("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAnyNoRestriction() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").filter(
        "Roles/any()");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToOneValue() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Parent/CodeID eq 'NUTS1'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(11, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToOneValueAndEquals() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Parent/CodeID eq 'NUTS1' and DivisionCode eq 'BE34'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyToOneValueTwoHops() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "Parent/Parent/CodeID eq 'NUTS1' and DivisionCode eq 'BE212'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyToOneValueViaComplexType() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "AdministrativeInformation/Created/By eq '99'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(8, orgs.size());
  };

  @Test
  public void testEmptyFilterResultNavigationPropertyToOneValueViaComplexType() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "AdministrativeInformation/Created/By eq 'NonExistingUserId'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode values = helper.getJsonObjectValues();

    assertEquals(0, values.size());
  };

  @Disabled("RegionName currently not available in PostalAdress")
  @Test
  public void testFilterNavigationPropertyDescriptionViaComplexTypeWOSubselectSelectId() throws IOException,
  ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "Address/RegionName eq 'Kalifornien'").select("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(3, orgs.size());
  };

  @Test
  public void testFilterSubstringStartEndIndexToLower() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").filter(
        "Language eq 'de' and tolower(substring(Name,0,5)) eq 'north'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterContainsOnIntegerCast() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "contains(cast(AIntegerYear, Edm.String), '90')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(1, entities.size());
  }

  @Test
  public void testFilterContainsOnNavigationPathManyToOne() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "contains(Parent/CountryCode, 'EU')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertTrue(entities.size() > 10);
  }

  @Test
  public void testFilterContainsOnNavigationPathOneToOne() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("BusinessPartners").filter(
        "contains(Creator/LastName, 'lle')"); // find Müller
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(1, entities.size());
  }

  @Test
  public void testFilterCastFailingForInteger2Binary() throws IOException, ODataException {
    // cast should fail with our current implementation
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "cast(AIntegerYear, Edm.Binary) gt 90");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void testFilterCaseInsensitive() throws IOException, ODataException {

    // find 'EuroSTat'
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "contains(toupper(CodePublisher), 'ST')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(122, entities.size());
  }

  @Test
  public void testFilterTimestamp2SqlTimestampWithContainsAndCast() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "contains(cast(ATimestamp1SqlTimestamp, Edm.String), '09:21:00')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(1, entities.size());
  }

  @Test
  public void testFilterBooleanAttribute() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "ABoolean eq true");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(1, entities.size());
  }

  @Test
  public void testFilterNavigationThroughJoinTable() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID", "Country")
        .filter("Locations/any(d:d/Name eq 'Texas')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterWithWildcardEscape() throws IOException, ODataException {
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate cannot handle the not insertable property 'aDouble' while inserting, so we ignore test");

    // create entities with reserved characters
    final URIBuilder uriBuilderCreate = TestBase.newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities");

    final StringBuffer requestBody1 = new StringBuffer("{");
    requestBody1.append("\"AUrl\": \"http://my_underscore_test\"").append(", ");
    requestBody1.append("\"AIntBoolean\": true");
    requestBody1.append("}");
    final ServerCallSimulator servercallCreate1 = new ServerCallSimulator(persistenceAdapter, uriBuilderCreate,
        requestBody1.toString(), HttpMethod.POST);
    servercallCreate1.execute(HttpStatusCode.CREATED.getStatusCode());

    final StringBuffer requestBody2 = new StringBuffer("{");
    requestBody2.append("\"AUrl\": \"http://my*underscore_variation1\"").append(", ");
    requestBody2.append("\"AIntBoolean\": true");
    requestBody2.append("}");
    final ServerCallSimulator servercallCreate2 = new ServerCallSimulator(persistenceAdapter, uriBuilderCreate,
        requestBody2.toString(), HttpMethod.POST);
    servercallCreate2.execute(HttpStatusCode.CREATED.getStatusCode());

    final StringBuffer requestBody3 = new StringBuffer("{");
    requestBody3.append("\"AUrl\": \"http://my*?nderscore_variation2\"").append(", ");
    requestBody3.append("\"AIntBoolean\": true");
    requestBody3.append("}");
    final ServerCallSimulator servercallCreate3 = new ServerCallSimulator(persistenceAdapter, uriBuilderCreate,
        requestBody3.toString(), HttpMethod.POST);
    servercallCreate3.execute(HttpStatusCode.CREATED.getStatusCode());

    // query entity '_' having 1 valid result
    ArrayNode entities;
    final URIBuilder uriBuilderQuery1 = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "contains(AUrl, 'my_underscore')");
    final ServerCallSimulator servercallQuery1 = new ServerCallSimulator(persistenceAdapter, uriBuilderQuery1);
    servercallQuery1.execute(HttpStatusCode.OK.getStatusCode());
    entities = servercallQuery1.getJsonObjectValues();
    assertEquals(1, entities.size(), "With escaping only one result expected");
    assertTrue(entities.get(0).get("AUrl").asText().contains("my_underscore"));

    // query entity '?' having 0 valid result
    final URIBuilder uriBuilderQuery2 = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "contains(AUrl, 'my_?underscore')");
    final ServerCallSimulator servercallQuery2 = new ServerCallSimulator(persistenceAdapter, uriBuilderQuery2);
    servercallQuery2.execute(HttpStatusCode.OK.getStatusCode());
    entities = servercallQuery2.getJsonObjectValues();
    assertEquals(0, entities.size(), "With escaping only one result expected");

    // query entity '*' having 0 valid result
    final URIBuilder uriBuilderQuery3 = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "contains(AUrl, 'my*')");
    final ServerCallSimulator servercallQuery3 = new ServerCallSimulator(persistenceAdapter, uriBuilderQuery3);
    servercallQuery3.execute(HttpStatusCode.OK.getStatusCode());
    entities = servercallQuery3.getJsonObjectValues();
    assertEquals(2, entities.size(), "With escaping only one result expected");
  }

}
