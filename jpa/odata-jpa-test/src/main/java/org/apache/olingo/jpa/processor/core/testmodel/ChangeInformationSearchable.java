package org.apache.olingo.jpa.processor.core.testmodel;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;

/**
 * Variant of {@link ChangeInformation} with attributes mark for $search
 *
 */
@Embeddable
public class ChangeInformationSearchable {

  // all values are controlled by annotations in parent classes
  @Column
  @EdmSearchable
  private String by;

  @Column
  private Timestamp at;

}
