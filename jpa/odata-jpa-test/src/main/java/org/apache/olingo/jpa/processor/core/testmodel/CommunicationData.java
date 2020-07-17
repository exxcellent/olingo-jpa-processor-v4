package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;

@Embeddable
public class CommunicationData {
  @Column(name = "\"Telecom.Phone\"")
  private String landlinePhoneNumber;

  @Column(name = "\"Telecom.Mobile\"")
  private String mobilePhoneNumber;

  @Column(name = "\"Telecom.Fax\"")
  private String fax;

  @EdmIgnore("Ignore for test purposes")
  @Column(name = "\"Telecom.Email\"")
  private String email;

  public String getEmail() {
    return email;
  }

  public String getFax() {
    return fax;
  }

  public String getLandlinePhoneNumber() {
    return landlinePhoneNumber;
  }

  public String getMobilePhoneNumber() {
    return mobilePhoneNumber;
  }

  public void setMobilePhoneNumber(final String mobilePhoneNumber) {
    this.mobilePhoneNumber = mobilePhoneNumber;
  }

  public void setLandlinePhoneNumber(final String landlinePhoneNumber) {
    this.landlinePhoneNumber = landlinePhoneNumber;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public void setFax(final String fax) {
    this.fax = fax;
  }
}
