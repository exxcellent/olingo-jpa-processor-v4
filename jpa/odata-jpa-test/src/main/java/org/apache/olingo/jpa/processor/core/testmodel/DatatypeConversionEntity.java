package org.apache.olingo.jpa.processor.core.testmodel;

import java.math.BigDecimal;
import java.net.URL;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConverter;
import org.apache.olingo.jpa.processor.core.testmodel.converter.jpa.JPAUrlConverter;
import org.apache.olingo.jpa.processor.core.testmodel.converter.odata.EdmUrlConverter;
import org.apache.olingo.jpa.processor.core.testmodel.otherpackage.TestEnum;

/**
 * The ID is mapped in super class.
 *
 * @author Ralf Zozmann
 *
 */
@Entity(name = "DatatypeConversionEntity")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::DatatypeConversionEntity\"")
public class DatatypeConversionEntity extends AbstractEntity {

	@Column(name = "\"ADate1\"")
	@Temporal(TemporalType.DATE)
	private java.util.Date aDate1;

	@Column(name = "\"ADate1\"", updatable = false, insertable = false)
	private java.sql.Date aDate11;

	@Column(name = "\"ADate2\"")
	private java.time.LocalDate aDate2;

	@Column(name = "\"ADate3\"")
	@Temporal(TemporalType.DATE)
	private java.util.Calendar aDate3;

	// @Column(name = "\"ATimestamp1\"")
	// private java.time.LocalDateTime aTimestamp1;

	// @Column(name = "\"ATimestamp2\"")
	// private java.time.LocalDateTime aTimestamp2;

	@Column(name = "\"AUrlString\"")
	@Convert(converter = JPAUrlConverter.class)
	@EdmAttributeConverter(EdmUrlConverter.class)
	private URL aUrl;

	@Column(name = "\"ADecimal\"", precision = 16, scale = 5)
	private BigDecimal aDecimal;

	// @Column(name = "\"AYear\"")
	// private java.time.Year aYear;

	@Column(name = "\"AStringMappedEnum\"")
	@Enumerated(EnumType.STRING)
	private IsoEra aStringMappedEnum;

	@Column(name = "\"AOrdinalMappedEnum\"")
	@Enumerated(EnumType.ORDINAL)
	private ChronoUnit aOrdinalMappedEnum;

	@Enumerated(javax.persistence.EnumType.STRING)
	@Column(name = "\"AOtherPackageEnum\"")
	private TestEnum aEnumFromOtherPackage;

	// do not define a JPA converter here, we want to test the autoapply!
	@Column(name = "\"UUID\"")
	private UUID uuid;

	@Column(name = "\"ABoolean\"")
	private boolean aBoolean;

	@EdmAction()
	public static boolean unboundActionCheckAllValueSettings(
			@EdmActionParameter(name = "jpaEnity") final DatatypeConversionEntity jpaEnity) {
		if (jpaEnity == null) {
			throw new IllegalStateException("Entity not given");
		}
		if (jpaEnity.aDate1 == null) {
			throw new IllegalStateException("aDate1 not set");
		}
		if (jpaEnity.aDate11 == null) {
			throw new IllegalStateException("aDate11 not set");
		}
		if (jpaEnity.aUrl == null) {
			throw new IllegalStateException("aUrl not set");
		}
		if (jpaEnity.aDecimal == null) {
			throw new IllegalStateException("aDecimal not set");
		}
		if (jpaEnity.aStringMappedEnum == null) {
			throw new IllegalStateException("aStringMappedEnum not set");
		}
		if (jpaEnity.aOrdinalMappedEnum == null) {
			throw new IllegalStateException("aOrdinalMappedEnum not set");
		}
		if (jpaEnity.aEnumFromOtherPackage == null) {
			throw new IllegalStateException("aEnumFromOtherPackage not set");
		}
		if (jpaEnity.uuid == null) {
			throw new IllegalStateException("uuid not set");
		}
		if (!jpaEnity.aBoolean) {
			throw new IllegalStateException("aBoolean not set");
		}
		return true;
	}
}
