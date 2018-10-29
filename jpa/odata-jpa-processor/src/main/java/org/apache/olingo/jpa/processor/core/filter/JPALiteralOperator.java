package org.apache.olingo.jpa.processor.core.filter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPATypeConvertor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;

public class JPALiteralOperator implements JPAOperator<Object> {
	private final Literal literal;
	private final OData odata;

	public JPALiteralOperator(final OData odata, final Literal literal) {
		this.literal = literal;
		this.odata = odata;
	}

	@Override
	public Object get() throws ODataApplicationException {
		final EdmPrimitiveType edmType = ((EdmPrimitiveType) literal.getType());
		try {

			final Class<?> defaultType = edmType.getDefaultType();
			final Constructor<?> c = defaultType.getConstructor(String.class);
			return c.newInstance(edmType.fromUriLiteral(literal.getText()));
		} catch (final InstantiationException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final IllegalAccessException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final IllegalArgumentException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final InvocationTargetException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final EdmPrimitiveTypeException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final NoSuchMethodException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final SecurityException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Converts a literal value into system type of attribute
	 */
	public Object get(final JPAAttribute attribute) throws ODataApplicationException {

		if (attribute.getType().isEnum()) {
			return getEnumValue(attribute);
		}
		try {
			// normal primitive type handling
			final CsdlProperty edmProperty = (CsdlProperty) attribute.getProperty();
			final EdmPrimitiveTypeKind edmTypeKind = JPATypeConvertor.convertToEdmSimpleType(attribute);
			// TODO literal does not convert decimals without scale properly
			// EdmPrimitiveType edmType = ((EdmPrimitiveType) literal.getType());
			String value = null;
			final EdmPrimitiveType edmType = odata.createPrimitiveTypeInstance(edmTypeKind);
			value = edmType.fromUriLiteral(literal.getText());
			return edmType.valueOfString(value, Boolean.valueOf(edmProperty.isNullable()), edmProperty.getMaxLength(),
					edmProperty.getPrecision(), edmProperty.getScale(), Boolean.TRUE, attribute.getType());

		} catch (final EdmPrimitiveTypeException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object getEnumValue(final JPAAttribute attribute) {
		return Enum.valueOf((Class<Enum>) attribute.getType(), literal.getText());
	}

	public Object get(final JPAOperationResultParameter returnType) throws ODataApplicationException {
		final EdmPrimitiveTypeKind edmTypeKind = EdmPrimitiveTypeKind.valueOfFQN(returnType.getTypeFQN());
		final EdmPrimitiveType edmType = odata.createPrimitiveTypeInstance(edmTypeKind);
		String value;
		try {
			value = edmType.fromUriLiteral(literal.getText());
			return edmType.valueOfString(value, Boolean.TRUE, returnType.getMaxLength(), returnType.getPrecision(),
					returnType.getScale(), Boolean.TRUE, returnType.getType());
		} catch (final EdmPrimitiveTypeException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	public Object get(final JPAOperationParameter jpaParameter) throws ODataApplicationException {
		try {
			final EdmPrimitiveTypeKind edmTypeKind = EdmPrimitiveTypeKind.valueOfFQN(jpaParameter.getTypeFQN());
			final EdmPrimitiveType edmType = odata.createPrimitiveTypeInstance(edmTypeKind);
			String value;

			value = edmType.fromUriLiteral(literal.getText());
			return edmType.valueOfString(value, Boolean.TRUE, jpaParameter.getMaxLength(), jpaParameter.getPrecision(),
					jpaParameter.getScale(), Boolean.TRUE, jpaParameter.getType());
		} catch (final EdmPrimitiveTypeException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	Literal getLiteral() {
		return literal;
	}
}
