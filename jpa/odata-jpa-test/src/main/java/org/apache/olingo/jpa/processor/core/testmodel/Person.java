package org.apache.olingo.jpa.processor.core.testmodel;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;

@Entity(name = "Person")
@DiscriminatorValue(value = "1")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::BusinessPartner\"")
public class Person extends BusinessPartner {

	@Column(name = "\"NameLine1\"")
	private String firstName;

	@Column(name = "\"NameLine2\"")
	private String lastName;

	@Convert(converter = DateConverter.class)
	@Column(name = "\"BirthDay\"")
	private LocalDate birthDay;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE, orphanRemoval = true)
	@JoinColumn(name = "\"ID\"", referencedColumnName = "\"PID\"", insertable = false, updatable = false, nullable = true)
	private PersonImage image1;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE, orphanRemoval = true)
	// Do not set 'referencedColumnName' in @@JoinColumn! We want to test auto
	// detect
	@JoinColumn(name = "\"ID\"", insertable = false, updatable = false, nullable = true)
	private PersonImage image2;

	@OneToOne(mappedBy = "personReferenceWithoutMappedAttribute")
	// Do not set a @JoinColum, we need a successful auto detection
	private PersonImage image3;

	/**
	 * Bound oData action.
	 */
	@EdmAction(name="ClearPersonsCustomStrings")
	public void clearCustomStrings() {
		setCustomString1(null);
		setCustomString2(null);
	}

	/**
	 * Bound oData action.
	 */
	@EdmAction(name="DoNothingAction1")
	public @NotNull BusinessPartner doNothing1(@EdmActionParameter(name="affectedPersons") final Collection<String> affectedPersons,
			@EdmActionParameter(name="minAny") final int minAny, @EdmActionParameter(name="maxAny") final Integer maxAny) {
		final Organization org = new Organization();
		org.setID("keyId...123");
		org.setName1("name 1");
		org.setCountry("DEU");
		org.setCustomString1("custom 1");
		org.setType("1");
		final PostalAddressData address = new PostalAddressData();
		address.setCityName("Berlin");
		address.setPOBox("1234567");
		org.setAddress(address);
		return org;
	}

	/**
	 * Bound oData action.
	 */
	@EdmAction(name="DoNothingAction2")
	public Collection<Person> doNothing2(@EdmActionParameter(name="paramAny") final String paramAny) {
		return Collections.emptyList();
	}

	/**
	 * Bound oData action.
	 */
	@EdmAction(name="SendBackTheInput")
	public String reflectBack(@EdmActionParameter(name="input") final String input) {
		return input;
	}

	/**
	 * Bound oData action.
	 */
	@EdmAction(name = "extractCountryCode")
	public String methodWithEntityParameter(@EdmActionParameter(name = "dummy") final int dummy,
			@EdmActionParameter(name = "country") final Country country) {
		return country.getCode();
	}
}
