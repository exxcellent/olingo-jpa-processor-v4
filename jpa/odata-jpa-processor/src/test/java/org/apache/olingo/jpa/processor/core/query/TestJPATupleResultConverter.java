package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryEntityResult;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.util.ServiceMetadataDouble;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestHelper;
import org.apache.olingo.jpa.processor.core.util.TupleDouble;
import org.apache.olingo.jpa.processor.core.util.UriHelperDouble;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Before;
import org.junit.Test;

public class TestJPATupleResultConverter extends TestBase {
	public static final int NO_POSTAL_ADDRESS_FIELDS = 7; // TODO 8
	public static final int NO_ADMIN_INFO_FIELDS = 2;
	private JPATuple2EntityConverter cut;
	private List<Tuple> tupleResult;
	private UriHelperDouble uriHelper;
	private Map<String, String> keyPredicates;
	private JPAQueryEntityResult jpaQueryResult;

	@Before
	public void setup() throws ODataException {
		helper = new TestHelper(persistenceAdapter.getMetamodel(), Constant.PUNIT_NAME);
		tupleResult = new ArrayList<Tuple>();
		final HashMap<String, List<Tuple>> result = new HashMap<String, List<Tuple>>(1);
		result.put(JPAQueryEntityResult.ROOT_RESULT, tupleResult);
		uriHelper = new UriHelperDouble();
		keyPredicates = new HashMap<String, String>();
		uriHelper.setKeyPredicates(keyPredicates, "ID");
		jpaQueryResult = new JPAQueryEntityResult(result, Long.valueOf(0), helper.getJPAEntityType("Organizations"));
		cut = new JPATuple2EntityConverter(
				helper.sd,
				jpaQueryResult.getEntityType(),
				uriHelper,
				new ServiceMetadataDouble(nameBuilder, "Organization"));
	}

	@Test
	public void checkConvertsEmptyResult() throws ODataApplicationException {
		assertNotNull(cut.convertQueryResult(jpaQueryResult));
	}

	@Test
	public void checkConvertsOneResultOneElement() throws ODataApplicationException {
		final HashMap<String, Object> result = new HashMap<String, Object>();

		result.put("ID", new String("1"));
		tupleResult.add(new TupleDouble(result));

		keyPredicates.put("1", "Organizations('1')");

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(1, act.getEntities().size());
		assertEquals("1", act.getEntities().get(0).getProperty("ID").getValue().toString());
	}

	@Test
	public void checkConvertsOneResultOneKey() throws ODataApplicationException {
		final HashMap<String, Object> result = new HashMap<String, Object>();
		keyPredicates.put("1", "'1'");

		result.put("ID", new String("1"));
		tupleResult.add(new TupleDouble(result));

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(1, act.getEntities().size());
		assertEquals("Organizations" + "('1')", act.getEntities().get(0).getId().getPath());
	}

	@Test
	public void checkConvertsTwoResultsOneElement() throws ODataApplicationException {
		HashMap<String, Object> result;

		result = new HashMap<String, Object>();
		result.put("ID", new String("1"));
		tupleResult.add(new TupleDouble(result));

		result = new HashMap<String, Object>();
		result.put("ID", new String("5"));
		tupleResult.add(new TupleDouble(result));

		keyPredicates.put("1", "Organizations('1')");
		keyPredicates.put("5", "Organizations('5')");

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(2, act.getEntities().size());
		assertEquals("1", act.getEntities().get(0).getProperty("ID").getValue().toString());
		assertEquals("5", act.getEntities().get(1).getProperty("ID").getValue().toString());
	}

	@Test
	public void checkConvertsOneResultsTwoElements() throws ODataApplicationException {
		final HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("ID", new String("1"));
		result.put("Name1", new String("Willi"));
		tupleResult.add(new TupleDouble(result));

		keyPredicates.put("1", "Organizations('1')");

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(1, act.getEntities().size());
		assertEquals("1", act.getEntities().get(0).getProperty("ID").getValue().toString());
		assertEquals("Willi", act.getEntities().get(0).getProperty("Name1").getValue().toString());
	}

	@Test
	public void checkConvertsOneResultsOneComplexElement() throws ODataApplicationException {
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

		keyPredicates.put("1", "Organizations('1')");

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(1, act.getEntities().size());

		assertEquals(ValueType.COMPLEX, act.getEntities().get(0).getProperty("Address").getValueType());
		final ComplexValue value = (ComplexValue) act.getEntities().get(0).getProperty("Address").getValue();
		assertEquals(NO_POSTAL_ADDRESS_FIELDS, value.getValue().size());
	}

	@Test
	public void checkConvertsOneResultsOneNestedComplexElement() throws ODataApplicationException {
		HashMap<String, Object> result;

		//    AdministrativeInformation adminInfo = new AdministrativeInformation();
		//    adminInfo.setCreated(new ChangeInformation("Joe Doe", Timestamp.valueOf("2016-01-22 12:25:23")));
		//    adminInfo.setUpdated(new ChangeInformation("Joe Doe", Timestamp.valueOf("2016-01-24 14:29:45")));
		result = new HashMap<String, Object>();
		result.put("ID", "1");
		result.put("AdministrativeInformation/Created/By", "Joe Doe");
		result.put("AdministrativeInformation/Created/At", "2016-01-22 12:25:23");
		result.put("AdministrativeInformation/Updated/By", "Joe Doe");
		result.put("AdministrativeInformation/Updated/At", "2016-01-24 14:29:45");
		tupleResult.add(new TupleDouble(result));

		keyPredicates.put("1", "Organizations('1')");

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(1, act.getEntities().size());
		// Check first level
		assertEquals(ValueType.COMPLEX, act.getEntities().get(0).getProperty("AdministrativeInformation").getValueType());
		final ComplexValue value = (ComplexValue) act.getEntities().get(0).getProperty("AdministrativeInformation").getValue();
		assertEquals(NO_ADMIN_INFO_FIELDS, value.getValue().size());
		// Check second level
		assertEquals(ValueType.COMPLEX, value.getValue().get(0).getValueType());
	}

	@Test
	public void checkConvertsOneResultsOneElementOfComplexElement() throws ODataApplicationException {
		final HashMap<String, Object> entityResult = new HashMap<String, Object>();
		entityResult.put("ID", "1");
		entityResult.put("Address/Region", new String("CA"));
		tupleResult.add(new TupleDouble(entityResult));

		keyPredicates.put("1", "Organizations('1')");

		final EntityCollection act = cut.convertQueryResult(jpaQueryResult);
		assertEquals(1, act.getEntities().size());
		assertEquals("CA", ((ComplexValue) act.getEntities().get(0).getProperty("Address").getValue()).getValue().get(0)
				.getValue().toString());
	}

	@Test
	public void checkConvertMediaStreamStaticMime() throws ODataJPAModelException, NumberFormatException,
	ODataApplicationException {

		final HashMap<String, List<Tuple>> result = new HashMap<String, List<Tuple>>(1);
		result.put("root", tupleResult);
		uriHelper.setKeyPredicates(keyPredicates, "PID");
		jpaQueryResult = new JPAQueryEntityResult(result, Long.valueOf(1), helper.getJPAEntityType("PersonImages"));
		final JPATuple2EntityConverter converter = new JPATuple2EntityConverter(
				helper.sd,
				jpaQueryResult.getEntityType(),
				uriHelper,
				new ServiceMetadataDouble(nameBuilder, "PersonImages"));

		final HashMap<String, Object> entityResult = new HashMap<String, Object>();
		entityResult.put("PID", "1");
		final byte[] image = { -119, 10 };
		entityResult.put("Image", image);
		tupleResult.add(new TupleDouble(entityResult));

		final EntityCollection act = converter.convertQueryResult(jpaQueryResult);
		assertEquals("image/png", act.getEntities().get(0).getMediaContentType());
	}

	@Test
	public void checkConvertMediaStreamDynamicMime() throws ODataJPAModelException, NumberFormatException,
	ODataApplicationException {

		final HashMap<String, List<Tuple>> result = new HashMap<String, List<Tuple>>(1);
		result.put("root", tupleResult);
		jpaQueryResult = new JPAQueryEntityResult(result, Long.valueOf(1), helper.getJPAEntityType("OrganizationImages"));
		final JPATuple2EntityConverter converter = new JPATuple2EntityConverter(
				helper.sd,
				jpaQueryResult.getEntityType(),
				uriHelper,
				new ServiceMetadataDouble(nameBuilder, "OrganizationImages"));

		final HashMap<String, Object> entityResult = new HashMap<String, Object>();
		entityResult.put("ID", "9");
		final byte[] image = { -119, 10 };
		entityResult.put("Image", image);
		entityResult.put("MimeType", "image/svg+xml");
		tupleResult.add(new TupleDouble(entityResult));

		final EntityCollection act = converter.convertQueryResult(jpaQueryResult);
		assertEquals("image/svg+xml", act.getEntities().get(0).getMediaContentType());
		assertEquals(2, act.getEntities().get(0).getProperties().size());
	}
}
