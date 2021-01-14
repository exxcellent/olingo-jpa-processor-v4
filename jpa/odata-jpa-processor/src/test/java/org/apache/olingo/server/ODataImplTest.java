package org.apache.olingo.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.core.deserializer.json.JPAODataJsonDeserializer;
import org.apache.olingo.server.core.serializer.json.JPAODataJsonSerializer;
import org.junit.Test;

public class ODataImplTest {

  @Test
  public void testBuiltinFactory() throws IOException, ODataException {
    // default implementation
    assertTrue(ODataFactory.createCustomODataInstance().getClass() == JPAODataImpl.class);
  }

  @Test(expected = SerializerException.class)
  public void testSerializerNoContentType() throws IOException, ODataException {
    final OData odata = ODataFactory.createCustomODataInstance();
    odata.createSerializer(null);
  }

  @Test
  public void testSerializer() throws IOException, ODataException {
    final OData odata = ODataFactory.createCustomODataInstance();
    assertTrue(odata.createSerializer(ContentType.JSON_FULL_METADATA).getClass() == JPAODataJsonSerializer.class);
    assertTrue(odata.createSerializer(ContentType.JSON).getClass() == JPAODataJsonSerializer.class);
    assertFalse(odata.createSerializer(ContentType.APPLICATION_XML).getClass() == JPAODataJsonSerializer.class);
  }

  @Test
  public void testDeserializer() throws IOException, ODataException {
    final OData odata = ODataFactory.createCustomODataInstance();
    assertTrue(odata.createDeserializer(ContentType.JSON_FULL_METADATA).getClass() == JPAODataJsonDeserializer.class);
    assertTrue(odata.createDeserializer(ContentType.JSON).getClass() == JPAODataJsonDeserializer.class);
    assertFalse(odata.createDeserializer(ContentType.APPLICATION_XML).getClass() == JPAODataJsonDeserializer.class);
  }
}
