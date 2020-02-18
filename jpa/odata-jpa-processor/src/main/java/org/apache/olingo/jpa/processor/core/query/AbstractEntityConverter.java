package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;

public abstract class AbstractEntityConverter extends AbstractConverter {

  public static final String ACCESS_MODIFIER_GET = "get";
  public static final String ACCESS_MODIFIER_SET = "set";
  public static final String ACCESS_MODIFIER_IS = "is";

  protected final static Logger LOG = Logger.getLogger(AbstractEntityConverter.class.getName());

  protected static enum KeyPredicateStrategy {
    /**
     * Allow missing key fields, the id will not be unique and may have side effects to serialization.
     */
    ALLOW_NULL,
    /**
     * Throw exception for empty id's.
     */
    FORCE_EXISTING,
    /**
     * Generate an volatile id on demand if missing.
     */
    AUTOGENERATE_MISSING;
  }

  private final IntermediateServiceDocument sd;
  private final ServiceMetadata serviceMetadata;
  private final UriHelper uriHelper;

  public AbstractEntityConverter(final UriHelper uriHelper, final IntermediateServiceDocument sd,
      final ServiceMetadata serviceMetadata) {
    super();

    this.uriHelper = uriHelper;
    this.sd = sd;
    this.serviceMetadata = serviceMetadata;
  }

  protected final UriHelper getUriHelper() {
    return uriHelper;
  }

  protected final IntermediateServiceDocument getIntermediateServiceDocument() {
    return sd;
  }

  /**
   *
   * @param keyStrategy Define strategy to work with entities without key attribute or key values.
   * @return The id URI or <code>null</code>
   */
  protected final URI createId(final Entity odataEntity, final JPAEntityType jpaEntityType,
      final KeyPredicateStrategy keyStrategy)
          throws ODataJPAModelException {

    final EdmEntityType edmType = serviceMetadata.getEdm()
        .getEntityType(jpaEntityType.getExternalFQN());
    try {
      // TODO Clarify host-name and port as part of ID see
      // http://docs.oasis-open.org/odata/odata-atom-format/v4.0/cs02/odata-atom-format-v4.0-cs02.html#_Toc372792702

      final String setName = sd.getEntitySet(jpaEntityType).getExternalName();
      final StringBuffer uriString = new StringBuffer(setName);
      final String idPart = uriHelper.buildKeyPredicate(edmType, odataEntity);
      uriString.append("(");
      if (idPart != null && !idPart.isEmpty()) {
        uriString.append(idPart);
      } else {
        switch (keyStrategy) {
        case FORCE_EXISTING:
          throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM, "Entity has no key, but is required: "
              + odataEntity.toString());
        case AUTOGENERATE_MISSING:
          LOG.log(Level.FINER, "Found entity without key attributes, will create generated id: " + odataEntity
              .toString());
          uriString.append("generated-".concat(Long.toString(System.currentTimeMillis()).concat("-").concat(Integer
              .toString(odataEntity.hashCode()))));
          break;
        default:
          // accept null
        }
      }
      uriString.append(")");
      return new URI(uriString.toString());
    } catch (final URISyntaxException e) {
      throw new ODataRuntimeException("Unable to create id for entity: " + edmType.getName(), e);
    } catch (final IllegalArgumentException e) {
      return null;
    } catch (final SerializerException e) {
      throw new ODataRuntimeException("Unable to create id for entity: " + edmType.getName(), e);
    }
  }

  /**
   *
   * @param complexValueBuffer
   *            A map containing elements of type:
   *            {@link ComplexValue} or {@link List
   *            List&lt;ComplexValue&gt;}.
   * @param complexValueIndex
   *            In case of a collection of complex values the index
   *            will determine the correct complex value instance
   *            if multiple instances are already present. Range
   *            0..n (must be a valid index!)
   */
  protected final Property convertJPAValue2ODataAttribute(final Object value, final String externalName,
      final String prefix,
      final JPAStructuredType jpaStructuredType, final Map<String, Object> complexValueBuffer,
      final int complexValueIndex,
      final List<Property> properties) throws ODataJPAModelException, ODataJPAConversionException {

    final JPASelector path = jpaStructuredType.getPath(externalName);
    if (path == null) {
      return null;
    }
    if (JPAAssociationPath.class.isInstance(path)) {
      if (log.isLoggable(Level.FINEST)) {
        log.log(Level.FINEST, "Ignore property value targeting a relationship ("
            + jpaStructuredType.getExternalName() + "#" + externalName
            + ")... this happens for key column joins to select id's for target entities in a $expand scenario without columns mapped as attribute where we have to select the key columns to map the source entity to matching target (expanded) entities. Maybe this is no error...");
      }
      return null;
    }
    // take only the first, we are working recursive through the path
    final JPAAttribute<?> attribute = path.getPathElements().get(0);
    if (attribute != null && attribute.ignore()) {
      return null;
    }
    if (attribute != null && !attribute.isKey()
        && attribute.getAttributeMapping() == AttributeMapping.AS_COMPLEX_TYPE) {
      // complex type should never be a 'key'... todo: check that anytime!
      String bufferKey;
      if (prefix == null || prefix.isEmpty()) {
        bufferKey = attribute.getExternalName();
      } else {
        bufferKey = prefix + JPASelector.PATH_SEPERATOR + attribute.getExternalName();
      }
      List<Property> values = null;
      Property complexTypeProperty = null;
      if (attribute.isCollection()) {
        @SuppressWarnings("unchecked")
        List<ComplexValue> listOfComplexValues = (List<ComplexValue>) complexValueBuffer.get(bufferKey);
        if (listOfComplexValues == null) {
          listOfComplexValues = new LinkedList<>();
          complexValueBuffer.put(bufferKey, listOfComplexValues);
          complexTypeProperty = new Property(
              attribute.getStructuredType().getExternalFQN().getFullQualifiedNameAsString(),
              attribute.getExternalName(), ValueType.COLLECTION_COMPLEX, listOfComplexValues);
        }
        if (listOfComplexValues.size() < complexValueIndex + 1) {
          final ComplexValue complexValue = new ComplexValue();
          listOfComplexValues.add(complexValue);
          values = complexValue.getValue();
        } else {
          // take the existing entry
          values = listOfComplexValues.get(complexValueIndex).getValue();
          // skip 'complexTypeProperty' creation, because already existing
        }
      } else {
        ComplexValue complexValue = (ComplexValue) complexValueBuffer.get(bufferKey);
        if (complexValue == null) {
          complexValue = new ComplexValue();
          complexValueBuffer.put(bufferKey, complexValue);
          complexTypeProperty = new Property(
              attribute.getStructuredType().getExternalFQN().getFullQualifiedNameAsString(),
              attribute.getExternalName(), ValueType.COMPLEX, complexValue);
        }
        values = complexValue.getValue();
      }
      if (complexTypeProperty != null) {
        properties.add(complexTypeProperty);
      }
      final int splitIndex = attribute.getExternalName().length() + JPASelector.PATH_SEPERATOR.length();
      final String attributeName = externalName.substring(splitIndex);
      final Property complexTypeNestedProperty = convertJPAValue2ODataAttribute(value, attributeName, bufferKey,
          attribute.getStructuredType(), complexValueBuffer, complexValueIndex, values);
      return complexTypeNestedProperty;
    } else if (attribute != null && attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
      // leaf element is the property in the @EmbeddedId type
      final JPASimpleAttribute attributeComplexProperty = (JPASimpleAttribute) path.getLeaf();
      return convertJPA2ODataProperty(attributeComplexProperty, attributeComplexProperty.getExternalName(), value,
          properties);
    } else {
      // ...$select=Name1,Address/Region
      return convertJPA2ODataProperty((JPASimpleAttribute) attribute, externalName, value, properties);
    }
  }

}