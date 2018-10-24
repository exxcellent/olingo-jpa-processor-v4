package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPAQueryWhereClause extends TestBase {

	@Test
	public void testFilterOneEquals() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID eq '3'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
		assertEquals("3", orgs.get(0).get("ID").asText());
	}

	@Test
	public void testFilterOneDescriptionEquals() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=Country eq 'DEU'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
		assertEquals("10", orgs.get(0).get("ID").asText());
	}

	@Ignore("LocationName currently not available")
	@Test
	public void testFilterOneDescriptionEqualsFieldNotSelected() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=LocationName eq 'Deutschland'&$select=ID");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
		assertEquals("10", orgs.get(0).get("ID").asText());
	}

	@Test
	public void testFilterOneEqualsTwoProperties() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=DivisionCode eq CountryCode");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(4, orgs.size());
	}

	@Test
	public void testFilterOneEqualsInvert() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter='3' eq ID");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
		assertEquals("3", orgs.get(0).get("ID").asText());
	}

	@Test
	public void testFilterOneNotEqual() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID ne '3'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(9, orgs.size());
	}

	@Test
	public void testFilterOneGreaterEqualsString() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID ge '5'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(5, orgs.size()); // '10' is smaller than '5' when comparing strings!
	}

	@Test
	public void testFilterOneLowerThanTwoProperties() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=DivisionCode lt CountryCode");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(228, orgs.size());
	}

	@Test
	public void testFilterOneGreaterThanString() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID gt '5'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(4, orgs.size()); // '10' is smaller than '5' when comparing strings!
	}

	@Test
	public void testFilterOneLowerThanString() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID lt '5'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(5, orgs.size());
	}

	@Test
	public void testFilterOneLowerEqualsString() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID le '5'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(6, orgs.size());
	}

	@Test
	public void testFilterOneGreaterEqualsNumber() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Area ge 119330610");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(4, orgs.size());
	}

	@Test
	public void testFilterOneAndEquals() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and CodeID eq 'NUTS2'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(11, orgs.size());
	}

	@Test
	public void testFilterOneOrEquals() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=ID eq '5' or ID eq '10'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Test
	public void testFilterOneNotLower() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=not (Area lt 50000000)");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(24, orgs.size());
	}

	@Test
	public void testFilterTwoAndEquals() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and CodeID eq 'NUTS2' and DivisionCode eq 'BE25'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
		assertEquals("BEL", orgs.get(0).get("CountryCode").asText());
	}

	@Test
	public void testFilterAndOrEqualsParenthesis() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and (DivisionCode eq 'BE25' or  DivisionCode eq 'BE24')&$orderby=DivisionCode desc");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
		assertEquals("BE25", orgs.get(0).get("DivisionCode").asText());
	}

	@Test
	public void testFilterAndOrEqualsNoParenthesis() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and DivisionCode eq 'BE25' or  CodeID eq '3166-1'&$orderby=DivisionCode desc");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(5, orgs.size());
		assertEquals("USA", orgs.get(0).get("DivisionCode").asText());
	}

	@Test
	public void testFilterAddGreater() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Area add 7000000 ge 50000000");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(31, orgs.size());
	}

	@Test
	public void testFilterSubGreater() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Area sub 7000000 ge 60000000");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(15, orgs.size());
	}

	@Test
	public void testFilterDivGreater() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Area gt 0 and Area div Population ge 6000");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(9, orgs.size());
	}

	@Test
	public void testFilterMulGreater() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Area mul Population gt 0");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(64, orgs.size());
	}

	@Test
	public void testFilterMod() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Area gt 0 and Area mod 3578335 eq 0");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterLength() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=length(Name) eq 10");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(11, orgs.size());
	}

	@Test
	public void testFilterNow() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Persons?$filter=AdministrativeInformation/Created/At lt now()");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(3, orgs.size());
	}

	@Test
	public void testFilterContains() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=contains(CodeID,'166')");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(110, orgs.size());
	}

	@Test
	public void testFilterEndswith() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=endswith(CodeID,'166-1')");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(4, orgs.size());
	}

	@Test
	public void testFilterStartswith() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=startswith(DivisionCode,'DE-')");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(16, orgs.size());
	}

	@Test
	public void testFilterIndexOf() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=indexof(DivisionCode,'3') eq 4");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(7, orgs.size());
	}

	@Ignore("TODO")
	@Test
	public void testFilterSubstringStartIndex() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,6) eq 'Dakota'");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Test
	public void testFilterSubstringStartEndIndex() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,0,5) eq 'North'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Test
	public void testFilterSubstringLengthCalculated() throws IOException, ODataException {
		// substring(CompanyName, 1 add 4, 2 mul 3)
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,0,1 add 4) eq 'North'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Ignore // Usage of mult currently creates parser error: The types 'Edm.Double' and
	// '[Int64, Int32, Int16, Byte,
	// SByte]' are not compatible.
	@Test
	public void testFilterSubstringStartCalculated() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,2 mul 3) eq 'Dakota'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Test
	public void testFilterToLower() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and tolower(Name) eq 'brandenburg'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterToUpper() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and toupper(Name) eq 'HESSEN'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Ignore("TODO")
	@Test
	public void testFilterToUpperInvers() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=toupper('nuts1') eq CodeID");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterTrim() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and trim(Name) eq 'Sachsen'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterConcat() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Persons?$filter=concat(concat(LastName,','),FirstName) eq 'Mustermann,Max'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterBoolean1() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Countries?$filter=contains(Code,'H') and startswith(Name, 'S') and not endswith(Name, 'xyz')");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertTrue(orgs.size() > 0);
	}

	@Test
	public void testFilterBoolean2() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Countries?$filter=length(Code) gt 1 and startswith( substring(Name,0,3), 'S')&$top=3");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertTrue(orgs.size() > 0);
	}

	@Test
	public void testFilterNavigationPropertyToManyValueAny() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=Roles/any(d:d/RoleCategory eq 'A')");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(3, orgs.size());
	}

	@Test
	public void testFilterNavigationPropertyToManyValueAnyMultiParameter() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$select=ID&$filter=Roles/any(d:d/RoleCategory eq 'A' and d/BusinessPartnerID eq '1')");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterNavigationPropertyToManyValueAnyNoRestriction() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Persons?$filter=Roles/any()");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Test
	public void testFilterNavigationPropertyToManyValueAll() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$select=ID&$filter=Roles/all(d:d/RoleCategory eq 'A')");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Test
	public void testFilterCountNavigationProperty() throws IOException, ODataException {
		//https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301
		//Example 43: return all Categories with less than 10 products
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$select=ID&$filter=Roles/$count eq 2");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

	@Ignore("Currently no deeper navigation available ending with a collection")
	@Test
	public void testFilterCountNavigationPropertyMultipleHops() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$select=ID&$filter=AdministrativeInformation/Created/User/Roles/$count ge 2");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(8, orgs.size());
	}

	@Test
	public void testFilterNavigationPropertyToOneValue() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Parent/CodeID eq 'NUTS1'");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(11, orgs.size());
	}

	@Test
	public void testFilterNavigationPropertyToOneValueAndEquals() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Parent/CodeID eq 'NUTS1' and DivisionCode eq 'BE34'");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	};

	@Test
	public void testFilterNavigationPropertyToOneValueTwoHops() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions?$filter=Parent/Parent/CodeID eq 'NUTS1' and DivisionCode eq 'BE212'");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	};

	@Test
	public void testFilterNavigationPropertyToOneValueViaComplexType() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=AdministrativeInformation/Created/By eq '99'");
		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(8, orgs.size());
	};

	@Test
	public void testEmptyFilterResultNavigationPropertyToOneValueViaComplexType() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=AdministrativeInformation/Created/By eq 'NonExistingUserId'");
		helper.assertStatus(200);
		final ArrayNode values = helper.getValues();

		assertEquals(0, values.size());
	};

	@Ignore("RegionName currently not available in PostalAdress")
	@Test
	public void testFilterNavigationPropertyDescriptionViaComplexTypeWOSubselectSelectAll() throws IOException,
	ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=Address/RegionName eq 'Kalifornien'");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(3, orgs.size());
	};

	@Ignore("RegionName currently not available in PostalAdress")
	@Test
	public void testFilterNavigationPropertyDescriptionViaComplexTypeWOSubselectSelectId() throws IOException,
	ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=Address/RegionName eq 'Kalifornien'&$select=ID");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(3, orgs.size());
	};

	@Ignore("TODO")
	@Test
	public void testFilterNavigationPropertyDescriptionToOneValueViaComplexTypeWSubselect1() throws IOException,
	ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=AdministrativeInformation/Created/User/LocationName eq 'Schweiz'");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	};

	@Ignore("TODO")
	@Test
	public void testFilterNavigationPropertyDescriptionToOneValueViaComplexTypeWSubselect2() throws IOException,
	ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$filter=AdministrativeInformation/Created/User/LocationName eq 'Schweiz'&$select=ID");

		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	};

	@Test
	public void testFilterSubstringStartEndIndexToLower() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$filter=Language eq 'de' and tolower(substring(Name,0,5)) eq 'north'");

		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(2, orgs.size());
	}

	@Test
	public void testNavigationOneToOne1() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('99')/Image1");
		helper.assertStatus(200);
		assertNotNull(helper.getValue());
	}

	@Test
	public void testNavigationOneToOne2() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('99')/Image2");
		helper.assertStatus(200);
		assertNotNull(helper.getValue());
	}

	@Test
	public void testNavigationOneToOneWithoutMappedAttribute() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"PersonImages('99')/PersonReferenceWithoutMappedAttribute");
		helper.assertStatus(200);
		assertNotNull(helper.getValue());
	}
}
