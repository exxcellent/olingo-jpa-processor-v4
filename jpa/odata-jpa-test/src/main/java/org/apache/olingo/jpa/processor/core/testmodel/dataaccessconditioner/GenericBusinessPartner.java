package org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;
import org.apache.olingo.jpa.processor.core.testmodel.CommunicationData;

@Entity
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::BusinessPartner\"")
public class GenericBusinessPartner extends AbstractGenericBusinessPartner {

  @Id
  @Column(name = "\"ID\"")
  protected String ID;

  @Column(name = "\"Type\"", length = 1, nullable = false, insertable = false, updatable = false)
  protected String type;

  @EdmSearchable
  @Column(name = "\"Country\"", length = 4)
  private String country;

  // Embedded type should never be null
  @Embedded
  protected CommunicationData communicationData = new CommunicationData();

  @OneToOne(mappedBy = "businessPartnerPerson")
  private BusinessPartnerImage image;

}
