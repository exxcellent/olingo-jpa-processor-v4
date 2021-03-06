package org.apache.olingo.server.core.serializer.json;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.olingo.commons.api.IConstants;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.ODataFactory;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.core.serializer.utils.ExpandSelectHelper;

import com.fasterxml.jackson.core.JsonGenerator;

public class JPAODataJsonSerializer extends ODataJsonSerializer {

  public JPAODataJsonSerializer(final ContentType contentType, final IConstants constants) {
    super(contentType, constants);
  }

  // called for single complex value
  @Override
  protected void writeProperties(final ServiceMetadata metadata, final EdmStructuredType type,
      final List<Property> properties,
      final SelectOption select, final JsonGenerator json, final Linked linked, final ExpandOption expand)
          throws IOException, SerializerException, DecoderException {
    super.writeProperties(metadata, type, properties, select, json, linked, expand);
    final Set<List<String>> expandedPaths = ExpandSelectHelper.getExpandedItemsPath(expand);
    writeOpenTypeProperties(metadata, type, properties, null, json, expandedPaths, linked, expand);
  }

  // called for complex value collection
  @Override
  protected void writeComplexValue(final ServiceMetadata metadata, final EdmComplexType type,
      final List<Property> properties,
      final Set<List<String>> selectedPaths, final JsonGenerator json, final Set<List<String>> expandedPaths,
      final Linked linked,
      final ExpandOption expand, final String complexPropName) throws IOException, SerializerException,
  DecoderException {
    super.writeComplexValue(metadata, type, properties, selectedPaths, json, expandedPaths, linked, expand,
        complexPropName);
    writeOpenTypeProperties(metadata, type, properties, selectedPaths, json, expandedPaths, linked, expand);
  }

  private void writeOpenTypeProperties(final ServiceMetadata metadata, final EdmStructuredType type,
      final List<Property> properties, final Set<List<String>> selectedPaths, final JsonGenerator json,
      final Set<List<String>> expandedPaths, final Linked linked, final ExpandOption expand) throws IOException,
  SerializerException, DecoderException {
    if (!type.isOpenType()) {
      return;
    }
    // write dynamic properties of open type
    for (final Property property : properties) {
      // properties defined in type are not dynamic
      if (type.getProperty(property.getName()) != null) {
        continue;
      }
      if (selectedPaths == null || ExpandSelectHelper.isSelected(selectedPaths, property.getName())) {
        final EdmProperty edmProperty = ODataFactory.createDynamicEdmProperty(type, property.getName());
        writeProperty(metadata, edmProperty, property,
            selectedPaths == null ? null : ExpandSelectHelper.getReducedSelectedPaths(selectedPaths, property
                .getName()), json, expandedPaths, linked, expand);
      }
    }
  }
}
