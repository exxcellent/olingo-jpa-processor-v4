package org.apache.olingo.jpa.processor.core.testmodel;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
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

  @Column(name = "\"Type\"", length = 1, nullable = false, insertable = false, updatable = false)
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
  @Column(name = "\"CustomNum1\"", columnDefinition = "decimal", precision = 16, scale = 5)
  protected BigDecimal customNum1;

  /**
   * Declare with precision higher than possible in DB
   */
  @EdmIgnore
  @Column(name = "\"CustomNum2\"", columnDefinition = "decimal", precision = 34)
  protected BigDecimal customNum2;

  @Column(name = "\"Country\"", length = 4)
  private String country;

  // Hibernate has problems to support a scenario to join only one column from a
  // table with more columns as part of the id so we "invent" a dynamic filled
  // join table
  @OneToMany(fetch = FetchType.EAGER, orphanRemoval = false, cascade = { CascadeType.REFRESH })
  @JoinTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::BPADDJoinTable\"", joinColumns = {
      @JoinColumn(referencedColumnName = "\"ID\"", name = "\"BusinessPartnerID\"") }, inverseJoinColumns = {
          @JoinColumn(referencedColumnName = "\"CodePublisher\"", name = "\"CodePublisher\""),
          @JoinColumn(referencedColumnName = "\"CodeID\"", name = "\"CodeID\""),
          @JoinColumn(referencedColumnName = "\"DivisionCode\"", name = "\"DivisionCode\""),
          @JoinColumn(referencedColumnName = "\"LanguageISO\"", name = "\"LanguageISO\"") })
  private Collection<AdministrativeDivisionDescription> locations;

  // Embedded type should never be null
  @Embedded
  protected CommunicationData communicationData = new CommunicationData();

  // Embedded type should never be null
  @Embedded
  private PostalAddressData address = new PostalAddressData();

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "created.by", column = @Column(name = "\"CreatedBy\"", nullable=false) ),
    @AttributeOverride(name = "created.at", column = @Column(name = "\"CreatedAt\"", insertable = false, updatable = false)),
    @AttributeOverride(name = "updated.by", column = @Column(name = "\"UpdatedBy\"") ),
    @AttributeOverride(name = "updated.at", column = @Column(name = "\"UpdatedAt\"") )
  })
  private AdministrativeInformation administrativeInformation = new AdministrativeInformation();

  // BusinessPartnerRole is defined as 'read only' and must not be deleted
  @OneToMany(mappedBy = "businessPartner", fetch = FetchType.LAZY, orphanRemoval = false, cascade = {
      CascadeType.REMOVE })
  private Collection<BusinessPartnerRole> roles;

  @ElementCollection
  @CollectionTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::Phone\"", joinColumns = @JoinColumn(name = "\"PartnerID\""))
  @Column(name = "\"PhoneNumber\"", length = 128, insertable = false, updatable = false)
  private final Set<String> phoneNumbersAsString = new HashSet<>();

  /**
   * Additional mapping for an {@link ElementCollection @ElementCollection} using
   * {@link Embeddable @Embeddable}.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::Phone\"", joinColumns = @JoinColumn(name = "\"PartnerID\""))
  private final Set<Phone> phoneNumbers = new HashSet<>();

  public void addPhone(final Phone phone) {
    phoneNumbers.add(phone);
  }

  public Set<String> getPhoneNumbersAsString() {
    return phoneNumbersAsString;
  }

  public void setID(final String iD) {
    ID = iD;
  }

  public String getID() {
    return ID;
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

  public String getCountry() {
    return country;
  }

  public void setCountry(final String country) {
    this.country = country;
  }

  public void setCommunicationData(final CommunicationData communicationData) {
    this.communicationData = communicationData;
  }

  public CommunicationData getCommunicationData() {
    return communicationData;
  }

  public void setAddress(final PostalAddressData address) {
    this.address = address;
  }

  public PostalAddressData getAddress() {
    return address;
  }

  public void setAdministrativeInformation(final AdministrativeInformation administrativeInformation) {
    this.administrativeInformation = administrativeInformation;
  }

  public AdministrativeInformation getAdministrativeInformation() {
    return administrativeInformation;
  }

  public void setRoles(final Collection<BusinessPartnerRole> roles) {
    this.roles = roles;
  }

  /**
   * Bound oData action, called by UI5 demo client
   */
  @EdmAction
  public BusinessPartner modifyBusinessPartner(
      @EdmActionParameter(name = "changedRegion") final String changedRegion,
      @EdmActionParameter(name = "changedPostalCode") final String changedPostalCode,
      @EdmActionParameter(name = "changedCityName") final String changedCityName,
      @EdmActionParameter(name = "changedStreetName") final String changedStreetName,
      @EdmActionParameter(name = "changedHouseNumber") final String changedHouseNumber,
      @Inject final EntityManager em) {

    // Just a very simple example of a possible application-logic triggered by odata-action calls

    final PostalAddressData addressData = this.getAddress();
    if (Objects.nonNull(changedRegion)) {
      addressData.setRegion(changedRegion);
    }
    if (Objects.nonNull(changedPostalCode)) {
      if (Integer.parseInt(changedPostalCode) > 2000) {
        this.seteTag(2000L);
      }
      addressData.setPostalCode(changedPostalCode);
    }
    if (Objects.nonNull(changedCityName)) {
      addressData.setCityName(changedCityName);
    }
    if (Objects.nonNull(changedStreetName)) {
      addressData.setStreetName(changedStreetName);
    }
    if (Objects.nonNull(changedHouseNumber)) {
      addressData.setHouseNumber(changedHouseNumber);
    }
    return this;
  }
}
