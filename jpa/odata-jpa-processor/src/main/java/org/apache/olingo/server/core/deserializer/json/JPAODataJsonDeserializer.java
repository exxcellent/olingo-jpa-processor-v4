package org.apache.olingo.server.core.deserializer.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.olingo.commons.api.IConstants;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.ODataFactory;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * This class is a complete copy of {@link ODataJsonDeserializer}, because all methods are private and cannot be reused.
 * Modified are:
 * <ul>
 * <li>{@link #readComplexNode(String, EdmType, boolean, JsonNode)} to read dynamic properties of an open type.</li>
 * </ul>
 *
 */
public class JPAODataJsonDeserializer extends ODataJsonDeserializer implements ODataDeserializer {

  public JPAODataJsonDeserializer(final ContentType contentType, final ServiceMetadata serviceMetadata,
      final IConstants constants) {
    super(contentType, serviceMetadata, constants);
  }

  @Override
  protected void readComplexNodeExtension(EdmType type, ComplexValue result, JsonNode jsonNode) throws DeserializerException {
    super.readComplexNodeExtension(type, result, jsonNode);
    readDynamicProperties(type, result, jsonNode);
  }
  
  /**
   * INFO: new code part
   * Custom method to handle dynamic properties.
   */
  protected void readDynamicProperties(final EdmType type, final ComplexValue result, final JsonNode jsonNode)
      throws DeserializerException {
    if (result == null) {
      return;
    }
    final EdmComplexType openComplexType = (EdmComplexType) type;
    if (!openComplexType.isOpenType()) {
      return;
    }
    // consume all still existing properties in jsonNode
    final List<String> toRemove = new ArrayList<>();
    for (final Entry<String, JsonNode> entry : jsonNode.properties()) {
      final String propertyName = entry.getKey();
      final EdmProperty edmProperty = ODataFactory.createDynamicEdmProperty(openComplexType, propertyName);
      final Property property = consumePropertyNode(edmProperty.getName(), edmProperty.getType(),
          edmProperty.isCollection(),
          edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(),
          edmProperty.isUnicode(), edmProperty.getMapping(),
          entry.getValue());
      result.getValue().add(property);
      toRemove.add(propertyName);
    }
    ((ObjectNode) jsonNode).remove(toRemove);

  }


}
