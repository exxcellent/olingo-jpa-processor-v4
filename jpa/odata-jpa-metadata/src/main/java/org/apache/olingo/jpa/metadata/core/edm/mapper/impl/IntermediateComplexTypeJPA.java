package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.stream.Collectors;

import javax.persistence.metamodel.EmbeddableType;

import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Complex Types are used to structure Entity Types by grouping properties that belong together. Complex Types can
 * contain of
 * <ul>
 * <li>Properties
 * <li>Navigation Properties
 * </ul>
 * This means that they can contain of primitive, complex, or enumeration type, or a collection of primitive, complex,
 * or enumeration types.
 * <p>
 * <b>Limitation:</b> As of now the attributes BaseType, Abstract and OpenType are not supported. There is also no
 * support for nested complex types
 * <p>
 * Complex Types are generated from JPA Embeddable Types.
 * <p>
 * For details about Complex Type metadata see:
 * <a href=
 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397985"
 * >OData Version 4.0 Part 3 - 9 Complex Type</a>
 *
 * @author Oliver Grande
 *
 */
class IntermediateComplexTypeJPA extends AbstractStructuredTypeJPA<EmbeddableType<?>, CsdlComplexType> implements
JPAComplexType {

  private CsdlComplexType edmComplexType;

  IntermediateComplexTypeJPA(final JPAEdmNameBuilder nameBuilder, final EmbeddableType<?> jpaEmbeddable,
      final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {

    super(determineComplexTypeNameBuilder(nameBuilder, jpaEmbeddable.getJavaType()), jpaEmbeddable, serviceDocument);
    this.setExternalName(getNameBuilder().buildComplexTypeName(jpaEmbeddable.getJavaType()));

  }

  private static JPAEdmNameBuilder determineComplexTypeNameBuilder(final JPAEdmNameBuilder nameBuilderDefault,
      final Class<?> ctClass) {
    final ODataComplexType ctAnnotation = ctClass.getAnnotation(ODataComplexType.class);
    if (ctAnnotation == null || ctAnnotation.attributeNaming() == null) {
      // nothing to change
      return nameBuilderDefault;
    }
    // prepare a custom name builder
    return new JPAEdmNameBuilder(nameBuilderDefault.getNamespace(), ctAnnotation.attributeNaming());
  }

  @Override
  final protected void lazyBuildEdmItem() throws ODataJPAModelException {
    initializeType();
    if (edmComplexType == null) {
      edmComplexType = new CsdlComplexType();

      edmComplexType.setName(this.getExternalName());
      edmComplexType.setProperties(getAttributes(true).stream().map(attribute -> attribute.getProperty()).collect(
          Collectors
          .toList()));
      edmComplexType.setNavigationProperties(getAssociations().stream().map(association -> association.getProperty())
          .collect(Collectors.toList()));
      edmComplexType.setBaseType(determineBaseType());
      edmComplexType.setAbstract(isAbstract());
      edmComplexType.setOpenType(isOpenType());
      if (determineHasStream()) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_STREAM,
            getInternalName());
      }
    }
  }

  @Override
  CsdlComplexType getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmComplexType;
  }
}
