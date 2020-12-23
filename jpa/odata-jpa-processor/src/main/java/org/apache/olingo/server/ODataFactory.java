package org.apache.olingo.server;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.olingo.commons.api.edm.EdmAnnotation;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.core.DynamicEdmProperty;

/**
 * Factory to create custom {@link OData} instances. This class was introduced to workaround
 * {@link https://issues.apache.org/jira/browse/OLINGO-1303}.
 * @author Ralf zozmann
 *
 */
public class ODataFactory {
  /**
   *
   * @return A user custom instance (via {@link ServiceLoader SPI}) or the adapter built-in default one.
   */
  public static OData createCustomODataInstance() {
    final ServiceLoader<OData> loader = ServiceLoader.load(OData.class);
    final Iterator<OData> iterator = loader.iterator();
    // take first one
    if (iterator.hasNext()) {
      return iterator.next();
    }
    // our custom default
    return new JPAODataImpl();
  }

  /**
   *
   * @return The EDM representation of an dynamic property.
   */
  public static EdmProperty createDynamicEdmProperty(final EdmComplexType openComplexType, final String propertyName)
      throws ODataRuntimeException {
    if (openComplexType.getAnnotations() != null) {
      final String namespaceType = openComplexType.getFullQualifiedName().getFullQualifiedNameAsString();
      final Optional<EdmAnnotation> optionalAnnotationType = openComplexType.getAnnotations().stream().filter(a -> a
          .getTerm().getFullQualifiedName().getFullQualifiedNameAsString().equals(namespaceType+"."+JPAComplexType.OPEN_TYPE_ANNOTATION_NAME_VALUE_TYPE)).findFirst();
      final Optional<EdmAnnotation> optionalAnnotationCollectionFlag = openComplexType.getAnnotations().stream().filter(
          a -> a.getTerm().getFullQualifiedName().getFullQualifiedNameAsString().equals(namespaceType + "."
              + JPAComplexType.OPEN_TYPE_ANNOTATION_NAME_VALUE_COLLECTION_FLAG)).findFirst();
      if (optionalAnnotationType.isPresent() && optionalAnnotationCollectionFlag.isPresent()) {
        final String sType = optionalAnnotationType.get().getQualifier();
        final EdmPrimitiveTypeKind kind = EdmPrimitiveTypeKind.valueOf(sType);
        final boolean isCollection = Boolean.valueOf(optionalAnnotationCollectionFlag.get().getQualifier())
            .booleanValue();
        return new DynamicEdmProperty(propertyName, kind, isCollection);

      }
    }
    throw new ODataRuntimeException("Missing annotations to handle open type property value type in a proper way");
  }
}
