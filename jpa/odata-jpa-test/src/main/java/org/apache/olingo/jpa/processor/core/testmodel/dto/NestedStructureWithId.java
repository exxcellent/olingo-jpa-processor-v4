package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;

/**
 * Test POJO to realize a OData entity for shared usage to test merging/relationship handling on actions.
 *
 * @author Ralf Zozmann
 *
 */
@ODataDTO(attributeNaming = NamingStrategy.AsIs,
edmEntitySetName = "NestedStructuresWithId")
public class NestedStructureWithId {

  @Id
  String name;
  List<NestedStructureWithId> children = new LinkedList<>();

  private NestedStructureWithId setName(final String name) {
    this.name = name;
    return this;
  }

  /**
   * Unbound oData action to produce nested structure.
   */
  @EdmAction
  public static NestedStructureWithId createNestedStructureWithShared() {
    final NestedStructureWithId root = new NestedStructureWithId().setName("root");
    final NestedStructureWithId child11 = new NestedStructureWithId().setName("child 1.1");
    root.children.add(child11);
    final NestedStructureWithId child12 = new NestedStructureWithId().setName("child 1.2");
    root.children.add(child12);
    final NestedStructureWithId leafShared = new NestedStructureWithId().setName("shared leaf");
    child11.children.add(leafShared);
    child12.children.add(leafShared);
    return root;
  }

  /**
   * Useless method to trigger conversions of data: OData -> JPA -> OData
   */
  @EdmAction
  public static NestedStructureWithId giveBackNestedStructure(@EdmActionParameter(
      name = "structure") final NestedStructureWithId root) {
    root.name = root.name + " server side modified";
    assert root.children.get(0).children.get(0) == root.children.get(1).children.get(0);
    return root;
  }
}
