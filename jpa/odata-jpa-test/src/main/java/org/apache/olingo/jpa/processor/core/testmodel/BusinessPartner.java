package org.apache.olingo.jpa.processor.core.testmodel;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunctionParameter;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunctions;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;;

@Inheritance
@DiscriminatorColumn(name = "\"Type\"")
@Entity(name = "BusinessPartner")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::BusinessPartner\"")
@EdmFunctions({
	@EdmFunction(
			name = "CountRoles",
			functionName = "COUNT_ROLES",
			returnType = @EdmFunction.ReturnType(isCollection = true),
			parameter = { @EdmFunctionParameter(name = "Amount", parameterName = "a", type = Integer.class),
			}),

	@EdmFunction(
			name = "max",
			functionName = "MAX",
			isBound = false,
			returnType = @EdmFunction.ReturnType(type = BigDecimal.class, isCollection = false),
			parameter = { @EdmFunctionParameter(name = "Path", parameterName = "path", type = String.class),
			}),

	@EdmFunction(
			name = "IsPrime",
			functionName = "IS_PRIME",
			isBound = false,
			hasFunctionImport = true,
			returnType = @EdmFunction.ReturnType(type = Boolean.class, isNullable = false),
			parameter = { @EdmFunctionParameter(name = "Number", type = BigDecimal.class, precision = 32, scale = 0) }),

})
public abstract class BusinessPartner {
	@Id
	@Column(name = "\"ID\"")
	protected String ID;

	@Version
	@Column(name = "\"ETag\"", nullable = false)
	protected long eTag;

	@Column(name = "\"Type\"", length = 1, nullable = false)
	protected String type;

	@Column(name = "\"CreatedAt\"", precision = 3)
	private Timestamp creationDateTime;

	@EdmIgnore
	@Column(name = "\"CustomString1\"")
	protected String customString1;
	@EdmIgnore
	@Column(name = "\"CustomString2\"")
	protected String customString2;
	@EdmIgnore
	@Column(name = "\"CustomNum1\"", precision = 16, scale = 5)
	protected BigDecimal customNum1;
	@EdmIgnore
	@Column(name = "\"CustomNum2\"", precision = 34)
	protected BigDecimal customNum2;

	@Column(name = "\"Country\"", length = 4)
	private String country;

	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = false)
	@JoinColumn(name = "\"DivisionCode\"", referencedColumnName = "\"Address.Region\"", nullable = false, insertable = false)
	private Collection<AdministrativeDivisionDescription> locations;

	@Embedded
	protected CommunicationData communicationData = new CommunicationData();

	@Embedded
	private PostalAddressData address = new PostalAddressData();

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "created.by", column = @Column(name = "\"CreatedBy\"", nullable=false) ),
		@AttributeOverride(name = "created.at", column = @Column(name = "\"CreatedAt\"", insertable=false) ),
		@AttributeOverride(name = "updated.by", column = @Column(name = "\"UpdatedBy\"") ),
		@AttributeOverride(name = "updated.at", column = @Column(name = "\"UpdatedAt\"") )
	})
	private AdministrativeInformation administrativeInformation = new AdministrativeInformation();

	// BusinessPartnerRole is defined as 'read only' and must not be deleted
	@OneToMany(mappedBy = "businessPartner", fetch = FetchType.LAZY, orphanRemoval = false)
	private Collection<BusinessPartnerRole> roles;

	@ElementCollection
	@CollectionTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::Phone\"", joinColumns = @JoinColumn(name = "\"PartnerID\""))
	@Column(name = "\"PhoneNumber\"", length = 128, insertable = false)
	private final Set<String> phoneNumbersAsString = new HashSet<>();

	/**
	 * Additional mapping for an {@link ElementCollection @ElementCollection} using
	 * {@link Embeddable @Embeddable}.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::Phone\"", joinColumns = @JoinColumn(name = "\"PartnerID\""))
	private final Set<Phone> phoneNumbers = new HashSet<>();

	public void setID(final String iD) {
		ID = iD;
	}

	public void seteTag(final long eTag) {
		this.eTag = eTag;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setCreationDateTime(final Timestamp creationDateTime) {
		this.creationDateTime = creationDateTime;
	}

	public void setCustomString1(final String customString1) {
		this.customString1 = customString1;
	}

	public void setCustomString2(final String customString2) {
		this.customString2 = customString2;
	}

	public void setCustomNum1(final BigDecimal customNum1) {
		this.customNum1 = customNum1;
	}

	public void setCustomNum2(final BigDecimal customNum2) {
		this.customNum2 = customNum2;
	}

	public void setCountry(final String country) {
		this.country = country;
	}

	public void setLocationNames(final Collection<AdministrativeDivisionDescription> locationName) {
		this.locations = locationName;
	}

	public void setCommunicationData(final CommunicationData communicationData) {
		this.communicationData = communicationData;
	}

	public void setAddress(final PostalAddressData address) {
		this.address = address;
	}

	public void setAdministrativeInformation(final AdministrativeInformation administrativeInformation) {
		this.administrativeInformation = administrativeInformation;
	}

	public void setRoles(final Collection<BusinessPartnerRole> roles) {
		this.roles = roles;
	}
}
