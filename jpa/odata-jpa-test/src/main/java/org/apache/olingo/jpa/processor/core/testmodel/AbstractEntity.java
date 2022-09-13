package org.apache.olingo.jpa.processor.core.testmodel;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;

/**
 * Super class without own persistence.
 *
 * @author Ralf Zozmann
 *
 */
@MappedSuperclass
public abstract class AbstractEntity {
  @Id
  @Column(name = "\"ID\"", updatable = false, nullable = false, unique = true, columnDefinition = "bigint")
  @GeneratedValue(generator = "IdSequence", strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "IdSequence", sequenceName = "OLINGO.IDSEQUENCE", allocationSize = 1)
  private Integer ID;

  public Integer getID() {
    return ID;
  }

  /**
   * For special purposes only
   */
  protected void setID(final Integer iD) {
    ID = iD;
  }

  @EdmAction(name = "actionInMappedSuperclass")
  public void actionInMappedSuperclass() {
    // do nothing
  }

}
