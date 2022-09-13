package org.apache.olingo.jpa.processor.core.testmodel;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 * Variant of {@link AdministrativeInformation} having nested embeddables with attributes for $search.
 *
 */
@Embeddable
public class AdministrativeInformationSearchable {

  @Embedded
  private final ChangeInformationSearchable created = new ChangeInformationSearchable();

  @Embedded
  private final ChangeInformationSearchable updated = new ChangeInformationSearchable();

}
