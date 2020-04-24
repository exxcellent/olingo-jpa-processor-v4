package org.apache.olingo.jpa.processor.core.testmodel.dto;

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
public class NestedStructureWithoutId {

  int levelCurrent = 1;
  int childDepth;
  NestedStructureWithoutId child = null;

  /**
   * Unbound oData action to produce nested structure of requested depth.
   */
  @SuppressWarnings("null")
  @EdmAction
  public static NestedStructureWithoutId createNestedStructure(@EdmActionParameter(
      name = "numberOfLevels") final int numberOfLevels) {
    if (numberOfLevels < 1) {
      throw new IllegalArgumentException("At least 1 level required");
    }
    NestedStructureWithoutId root = null;
    NestedStructureWithoutId current = null;
    NestedStructureWithoutId tmp;
    for (int i = 0; i < numberOfLevels; i++) {
      if (i == 0) {
        root = new NestedStructureWithoutId();
        root.levelCurrent = i + 1;
        root.childDepth = numberOfLevels;
        current = root;
      } else {
        tmp = new NestedStructureWithoutId();
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
      name = "structure") final NestedStructureWithoutId structure) {

    if (structure == null) {
      throw new IllegalStateException();
    }

    NestedStructureWithoutId current = structure;
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
