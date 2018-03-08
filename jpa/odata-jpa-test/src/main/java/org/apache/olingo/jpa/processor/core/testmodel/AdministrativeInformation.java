package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class AdministrativeInformation {

  @Embedded
  private ChangeInformation created = new ChangeInformation();
  
  @Embedded
  private ChangeInformation updated = new ChangeInformation();

  public ChangeInformation getCreated() {
    return created;
  }

  public ChangeInformation getUpdated() {
    return updated;
  }

  public void setCreated(ChangeInformation created) {
    this.created = created;
  }

  public void setUpdated(ChangeInformation updated) {
    this.updated = updated;
  }

}
