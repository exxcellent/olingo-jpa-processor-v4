package org.apache.olingo.jpa.processor.core.testmodel;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.processor.core.testmodel.converter.jpa.JPADateConverter;
import org.apache.olingo.jpa.processor.core.testmodel.otherpackage.TestEnum;

@Entity(name = "Person")
@DiscriminatorValue(value = "1")
public class Person extends BusinessPartner {

  /**
   * This logger is declared as field in the entity to check handling of
   * unsupported field types (ignored by O/R-Mapper).
   */
  @Transient
  private final Logger log = Logger.getLogger(Person.class.getName());

  /**
   * Must be ignored by O/R mapper and Olingo-processor
   */
  @Transient
  private final Serializable ignoredSerializableField = new Serializable() {
    private static final long serialVersionUID = 1L;
  };

  @Column(name = "\"NameLine1\"")
  private String firstName;

  @Column(name = "\"NameLine2\"")
  private String lastName;

  @Convert(converter = JPADateConverter.class)
  @Column(name = "\"BirthDay\"", columnDefinition = "date")
  private LocalDate birthDay;

  @OneToOne(mappedBy = "owningPerson", targetEntity = PersonImage.class, cascade = CascadeType.ALL)
  private BPImageIfc image1;

  @OneToOne(mappedBy = "personReferenceWithoutMappedAttribute")
  private PersonImage image2;

  // This collection (, not Set!) is defined to be used as always empty, so we should use it
  // with persons having no Phone entry
  @ElementCollection
  @CollectionTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::Phone\"", joinColumns = @JoinColumn(name = "\"PartnerID\"", referencedColumnName = "\"ID\"", updatable = false, insertable = false))
  private final Collection<Phone> partnerTelephoneConnections = new LinkedList<>();

  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = false, cascade = { CascadeType.REFRESH })
  @JoinTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::OrganizationMember\"", joinColumns = { @JoinColumn(
      referencedColumnName = "\"ID\"", name = "\"memberID\"") }, inverseJoinColumns = { @JoinColumn(
          referencedColumnName = "\"ID\"", name = "\"organizationID\"") })
  private Collection<Organization> memberOfOrganizations;

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
    log.log(Level.INFO, "doNothing2() was called");
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

  /**
   * Bound oData action.
   */
  @EdmAction(name = "sendBackEnumParameter")
  public String methodWithEnumParameter(@EdmActionParameter(name = "value") final TestEnum param) {
    return param.name();
  }

}
