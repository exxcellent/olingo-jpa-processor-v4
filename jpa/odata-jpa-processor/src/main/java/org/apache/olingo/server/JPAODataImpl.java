package org.apache.olingo.server;

import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.IConstants;
import org.apache.olingo.commons.api.constants.Constantsv00;
import org.apache.olingo.commons.api.constants.Constantsv01;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.core.ODataImpl;
import org.apache.olingo.server.core.deserializer.json.JPAODataJsonDeserializer;
import org.apache.olingo.server.core.serializer.json.JPAODataJsonSerializer;

/**
 * Custom {@link OData} implementation having custom (de)serializers.
 * @author Ralf Zozmann
 *
 */
public class JPAODataImpl extends ODataImpl {

  @Override
  public ODataSerializer createSerializer(final ContentType contentType) throws SerializerException {
    return createSerializer(contentType, Collections.singletonList("4.00"));
  }

  @Override
  public ODataSerializer createSerializer(final ContentType contentType, final List<String> versions)
      throws SerializerException {
    if (contentType == null) {
      return super.createSerializer(contentType, versions);
    }
    if (contentType.isCompatible(ContentType.APPLICATION_JSON)) {
      final String metadata = contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA);
      if (metadata == null
          || ContentType.VALUE_ODATA_METADATA_MINIMAL.equalsIgnoreCase(metadata)
          || ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(metadata)
          || ContentType.VALUE_ODATA_METADATA_FULL.equalsIgnoreCase(metadata)) {
        IConstants constants = new Constantsv00();
        if (determineMaxVersion(versions) > 4.0) {
          constants = new Constantsv01();
        }
        return new JPAODataJsonSerializer(contentType, constants);
      }
    }
    return super.createSerializer(contentType, versions);
  }

  private float determineMaxVersion(final List<String> versions) {
    if (versions == null || versions.isEmpty()) {
      return 4.00f;
    }
    final Float versionValue[] = new Float[versions.size()];
    int i = 0;
    Float max = new Float(0);
    for (final String version : versions) {
      final Float ver = Float.valueOf(version);
      versionValue[i++] = ver;
      max = max.floatValue() > ver.floatValue() ? max : ver;
    }
    return max.floatValue();
  }

  @Override
  public ODataDeserializer createDeserializer(final ContentType contentType) throws DeserializerException {
    return createDeserializer(contentType, Collections.singletonList("4.00"));
  }

  @Override
  public ODataDeserializer createDeserializer(final ContentType contentType, final List<String> versions)
      throws DeserializerException {
    return createDeserializer(contentType, null, versions);
  }

  @Override
  public ODataDeserializer createDeserializer(final ContentType contentType, final ServiceMetadata metadata)
      throws DeserializerException {
    return createDeserializer(contentType, metadata, Collections.singletonList("4.00"));
  }

  @Override
  public ODataDeserializer createDeserializer(final ContentType contentType, final ServiceMetadata metadata,
      final List<String> versions)
          throws DeserializerException {
    if (contentType != null && contentType.isCompatible(ContentType.JSON)) {
      IConstants constants = new Constantsv00();
      if (determineMaxVersion(versions) > 4.0) {
        constants = new Constantsv01();
      }
      return new JPAODataJsonDeserializer(contentType, metadata, constants);
    }
    return super.createDeserializer(contentType, metadata, versions);
  }
}
