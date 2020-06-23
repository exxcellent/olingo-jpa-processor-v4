package org.apache.olingo.jpa.processor.core.filter;

import java.lang.annotation.Annotation;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.metamodel.PluralAttribute.CollectionType;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TypeMapping;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.query.ValueConverter;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;

@SuppressWarnings("rawtypes")
public class JPALiteralOperand implements JPAExpressionElement<Object> {

  private static class AnonymousSimpleTypeElement implements JPATypedElement {

    @SuppressWarnings("unused")
    private final EdmPrimitiveTypeKind edmTypeKind;

    public AnonymousSimpleTypeElement(final EdmPrimitiveTypeKind edmTypeKind) {
      this.edmTypeKind = edmTypeKind;
    }

    @Override
    public boolean isNullable() {
      return false;
    }

    @Override
    public boolean isCollection() {
      return false;
    }

    @Override
    public Class<?> getType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public CollectionType getCollectionType() {
      return null;
    }

    @Override
    public Integer getScale() {
      return null;
    }

    @Override
    public Integer getPrecision() {
      return null;
    }

    @Override
    public Integer getMaxLength() {
      return null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
      throw new UnsupportedOperationException();
    }
  }

  private final static ValueConverter CONVERTER = new ValueConverter();
  private final Literal literal;
  private final OData odata;
  private final CriteriaBuilder cb;

  public JPALiteralOperand(final OData odata, final CriteriaBuilder cb, final Literal literal) {
    this.literal = literal;
    this.odata = odata;
    this.cb = cb;
  }

  private Expression<Object> wrapIntoExpression(final Object value) {
    if (value == null) {
      return cb.nullLiteral(Object.class);
    }
    return cb.literal(value);
  }

  @Override
  public Comparable get() throws ODataApplicationException {
    final JPATypedElement typeInformation = new AnonymousSimpleTypeElement(null);
    return convert2Value(null, typeInformation);
  }

  public Expression<Object> getLiteralExpression()
      throws ODataApplicationException {
    return wrapIntoExpression(get());
  }

  /**
   *
   * @param requestedTargetEdmTypeKind
   *            The optional value to give a hint to format
   *            the literal value using that given kind of
   *            data type.
   * @return The literal value represented by an instance of the requested target
   *         type.
   */
  public Expression<Object> getLiteralExpression(final EdmPrimitiveTypeKind requestedTargetEdmTypeKind)
      throws ODataApplicationException {
    // try to convert/cast the literal into an object of requested type
    final JPATypedElement typeInformation = new AnonymousSimpleTypeElement(requestedTargetEdmTypeKind);
    final Comparable<Object> value = convert2Value(requestedTargetEdmTypeKind, typeInformation);
    return wrapIntoExpression(value);
  }

  /**
   * Converts a literal value into system type of attribute
   */
  public Expression<Object> getLiteralExpression(final JPATypedElement attribute)
      throws ODataApplicationException {

    if (isNullLiteral()) {
      return wrapIntoExpression(null);
    }
    if (attribute.getType().isEnum()) {
      final Enum<?> enumValue = getEnumValue(attribute);
      return wrapIntoExpression(enumValue);
    }
    try {
      // normal primitive type handling
      final EdmPrimitiveTypeKind edmTypeKind = TypeMapping.convertToEdmSimpleType(attribute);
      final Object odataValue = convert2Value(edmTypeKind, attribute);
      final Object convertedValue = CONVERTER.convertOData2JPAValue(attribute, odataValue);
      return wrapIntoExpression(convertedValue);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  @SuppressWarnings("unchecked")
  private Comparable<Object> convert2Value(final EdmPrimitiveTypeKind requestedTargetEdmTypeKind,
      final JPATypedElement typeInformation)
          throws ODataApplicationException {
    if (isNullLiteral()) {
      return null;
    }
    final EdmPrimitiveType edmType;
    final Class<?> oadataType;
    if (requestedTargetEdmTypeKind == null) {
      // default behaviour
      edmType = ((EdmPrimitiveType) literal.getType());
      oadataType = edmType.getDefaultType();
    } else {
      // use hint
      edmType = odata.createPrimitiveTypeInstance(requestedTargetEdmTypeKind);
      oadataType = EdmPrimitiveTypeFactory.getInstance(requestedTargetEdmTypeKind).getDefaultType();
    }
    try {
      if (EdmEnumType.class.isInstance(edmType)) {
        // build enum literal instance
        final Class<Enum> clazz = (Class<Enum>) Class
            .forName(((EdmEnumType) edmType).getFullQualifiedName().getFullQualifiedNameAsString());
        return Enum.valueOf(clazz, literal.getText());
      }
      // TODO literal does not convert decimals without scale properly
      // EdmPrimitiveType edmType = ((EdmPrimitiveType) literal.getType());
      final String value = edmType.fromUriLiteral(literal.getText());
      return (Comparable<Object>) edmType.valueOfString(value, Boolean.valueOf(typeInformation.isNullable()),
          typeInformation.getMaxLength(), typeInformation.getPrecision(), typeInformation.getScale(),
          Boolean.TRUE, oadataType);
    } catch (final EdmPrimitiveTypeException | ClassNotFoundException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   *
   * @return TRUE if this literal represents the <i>null</i> literal as defined in
   *         OData specification.
   * @see http://docs.oasis-open.org/odata/odata/v4.01/cs01/part1-protocol/odata-v4.01-cs01-part1-protocol.html#sec_BuiltinQueryFunctions
   */
  protected boolean isNullLiteral() {
    final String text = literal.getText();
    if (text == null) {
      // not the 'null' text value for literal
      return false;
    }
    return "null".equalsIgnoreCase(text);
  }

  @SuppressWarnings({ "unchecked" })
  private Enum<?> getEnumValue(final JPATypedElement attribute) {
    return Enum.valueOf((Class<Enum>) attribute.getType(), literal.getText());
  }

  public Literal getODataLiteral() {
    return literal;
  }
}
