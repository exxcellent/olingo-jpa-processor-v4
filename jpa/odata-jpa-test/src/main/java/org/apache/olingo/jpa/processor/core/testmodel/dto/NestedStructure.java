package org.apache.olingo.jpa.processor.core.testmodel.dto;

import javax.persistence.Id;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;

/**
 * Test POJO to realize a OData entity without JPA persistence.
 *
 * @author Ralf Zozmann
 *
 */
@ODataDTO(attributeNaming = NamingStrategy.AsIs,
edmEntitySetName = "NestedStructures")
public class NestedStructure {

  private static long idSequence = 1;
  @Id
  private final long id;
  int levelCurrent = 1;
  int childDepth;
  NestedStructure child = null;

  public NestedStructure() {
    this.id = idSequence++;
  }

  /**
   * Unbound oData action to produce nested structure of requested depth.
   */
  @SuppressWarnings("null")
  @EdmAction
  public static NestedStructure createNestedStructure(@EdmActionParameter(
      name = "numberOfLevels") final int numberOfLevels) {
    if (numberOfLevels < 1) {
      throw new IllegalArgumentException("At least 1 level required");
    }
    NestedStructure root = null;
    NestedStructure current = null;
    NestedStructure tmp;
    for (int i = 0; i < numberOfLevels; i++) {
      if (i == 0) {
        root = new NestedStructure();
        root.levelCurrent = i + 1;
        root.childDepth = numberOfLevels;
        current = root;
      } else {
        tmp = new NestedStructure();
        tmp.levelCurrent = i + 1;
        tmp.childDepth = numberOfLevels - (i + 1);
        current.child = tmp;
        current = tmp;
      }
    }
    return root;
  }

  @EdmAction
  public static void validateNestedStructure(@EdmActionParameter(
      name = "structure") final NestedStructure structure) {

    if (structure == null) {
      throw new IllegalStateException();
    }

    NestedStructure current = structure;
    for (int i = 0; i < structure.childDepth; i++) {
      if (current == null) {
        throw new IllegalStateException();
      }
      if (current.childDepth == 0 && current.child != null) {
        throw new IllegalStateException();
      }
      if (current.childDepth > 0 && current.child == null) {
        throw new IllegalStateException();
      }
      current = current.child;
    }
  }
}
