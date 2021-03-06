package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;

@Embeddable
@ODataComplexType(attributeNaming = NamingStrategy.AsIs)
public class Phone {

  /**
   * International area code of complete phone number: +49/1234/55667788-0 ->
   * '+49'
   */
  @Size(max = 32)
  @Column(name = "\"InternationalAreaCode\"", length = 32)
  private String internationalAreaCode;

  /**
   * Main part of complete phone number: +49/1234/55667788-0 -> '1234/55667788-0'
   */
  @Column(name = "\"PhoneNumber\"", length = 128, nullable = false)
  private String phoneNumber;

  public void setInternationalAreaCode(final String internationalAreaCode) {
    this.internationalAreaCode = internationalAreaCode;
  }

  public void setPhoneNumber(final String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
}
