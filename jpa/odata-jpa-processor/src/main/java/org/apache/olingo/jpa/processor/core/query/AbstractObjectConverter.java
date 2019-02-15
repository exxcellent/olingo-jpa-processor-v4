package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.GeneratedValue;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.Valuable;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

public abstract class AbstractObjectConverter extends JPAAbstractConverter {

	protected static class TupleElementFacade<X> implements TupleElement<X> {

		// alias
		private final String alias;
		private final X value;
		private final Class<X> valueType;

		public TupleElementFacade(final String alias, final X value, final Class<X> valueType) {
			super();
			this.alias = alias;
			this.value = value;
			this.valueType = valueType;
		}

		@Override
		public String getAlias() {
			return alias;
		}

		@Override
		public Class<? extends X> getJavaType() {
			return valueType;
		}

	}

	protected static class TupleFacade<X> implements Tuple {

		private final Map<String, TupleElementFacade<X>> elementsMap;

		public TupleFacade(final Collection<TupleElementFacade<X>> elements) {
			elementsMap = new HashMap<>();
			for (final TupleElementFacade<X> element : elements) {
				elementsMap.put(element.alias, element);
			}
		}

		@Override
		@SuppressWarnings({ "hiding", "unchecked" })
		public <X> X get(final TupleElement<X> tupleElement) {
			for (final TupleElementFacade<?> element : elementsMap.values()) {
				if (element == tupleElement) {
					return (X) element.value;
				}
			}
			throw new IllegalArgumentException();
		}

		@Override
		@SuppressWarnings("hiding")
		public <X> X get(final String alias, final Class<X> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(final String alias) {
			final TupleElementFacade<X> element = elementsMap.get(alias);
			if (element != null) {
				return element.value;
			}
			throw new IllegalArgumentException();
		}

		@Override
		@SuppressWarnings("hiding")
		public <X> X get(final int i, final Class<X> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(final int i) {
			return toArray()[i];
		}

		@Override
		public Object[] toArray() {
			final Object[] values = new Object[elementsMap.size()];
			int i = 0;
			for (final TupleElementFacade<?> element : elementsMap.values()) {
				values[i] = element.value;
				i++;
			}
			return values;
		}

		@Override
		public List<TupleElement<?>> getElements() {
			return new ArrayList<>(elementsMap.values());
		}
	}

	public AbstractObjectConverter(final JPAEntityType jpaConversionTargetEntity, final UriHelper uriHelper,
			final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata) throws ODataApplicationException {
		super(jpaConversionTargetEntity, uriHelper, sd, serviceMetadata);
	}

	protected Collection<TupleElementFacade<Object>> convertJPAStructuredType(final Object persistenceObject,
			final JPAStructuredType jpaType, final String baseAttributePath) throws ODataJPAProcessorException, ODataJPAModelException {
		if (jpaType == null) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		final Collection<TupleElementFacade<Object>> elements = new LinkedList<>();
		Collection<TupleElementFacade<Object>> complexAttributeElements;

		// 1. attributes
		for (final JPASimpleAttribute attribute : jpaType.getAttributes()) {
			try {
				complexAttributeElements = convertAttribute2TupleElements(persistenceObject, jpaType, attribute, baseAttributePath);
				elements.addAll(complexAttributeElements);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, ex);
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
		}
		// 2. associations
		for (final JPAAssociationAttribute attribute : jpaType.getAssociations()) {
			try {
				complexAttributeElements = convertAttribute2TupleElements(persistenceObject, jpaType, attribute, baseAttributePath);
				elements.addAll(complexAttributeElements);
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, ex);
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
		}
		return elements;
	}

	private Collection<TupleElementFacade<Object>> convertAttribute2TupleElements(final Object persistenceObject,
			final JPAStructuredType jpaType, final JPAAttribute jpaAttribute, final String baseAttributePath) throws ODataJPAModelException,
	NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ODataJPAProcessorException {
		final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(persistenceObject);
		// final Object value = readJPAFieldValue(persistenceObject,
		// persistenceObject.getClass(),
		// jpaAttribute.getInternalName());
		final String alias = jpaAttribute.getExternalName();
		if (jpaAttribute.isAssociation() && value != null) {
			// TODO: how we can support navigation to other entities while converting a
			// single entity?
			if (Collection.class.isInstance(value) && ((Collection<?>) value).isEmpty()) {
				// we are lucky... a empty collection is no problem
				return Collections.emptyList();
			}
			// in all other cases give up
			final IllegalStateException throwable = new IllegalStateException(
					"Attribute " + alias + " of " + jpaType.getExternalName() + " contains unsupported association content");
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ASSOCIATION, throwable);
		} else if (jpaAttribute.isComplex()) {
			// ignore complex types that are not set (null)
			if (value == null) {
				return Collections.emptyList();
			}
			// create as navigation path to nested complex path property
			final JPASelector path = jpaType.getPath(alias);
			if (path == null) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_COMPLEX_TYPE);
			}
			// final ManagedType<?> persistenceEmbeddedType =
			// metamodel.managedType(jpaAttribute.getType());
			final JPAStructuredType jpaEmbeddedType = path.getLeaf().getStructuredType();
			final String newPath = baseAttributePath.concat(alias).concat(JPASelector.PATH_SEPERATOR);
			if (jpaAttribute.isCollection()) {
				final Collection<TupleElementFacade<Object>> elements = new LinkedList<>();
				for (final Object entry : ((Collection<?>) value)) {
					elements.addAll(convertJPAStructuredType(entry, jpaEmbeddedType/* , persistenceEmbeddedType */, newPath));
				}
				return elements;
			} else {
				return convertJPAStructuredType(value, jpaEmbeddedType/* , persistenceEmbeddedType */, newPath);
			}
		} else {
			// simple attribute
			@SuppressWarnings("unchecked")
			final TupleElementFacade<Object> element = new TupleElementFacade<Object>(baseAttributePath.concat(alias), value,
					(Class<Object>) jpaAttribute.getType());
			return Collections.singletonList(element);
		}
	}

	private Object determineSimpleOData2JPAProperty(final JPAStructuredType jpaEntityType,
			final JPAAttribute jpaAttribute, final Property sourceOdataProperty)
					throws ODataJPAProcessorException, ODataJPAModelException {
		if (sourceOdataProperty == null) {
			return null;
		}

		// convert simple/primitive value
		return convertOData2JPAPropertyValue(jpaAttribute, sourceOdataProperty);
	}

	private boolean convertSimpleOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType,
			final JPAAttribute jpaAttribute, final Property sourceOdataProperty)
					throws ODataJPAProcessorException, ODataJPAModelException {
		if (sourceOdataProperty == null) {
			if (jpaAttribute.isKey()) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.BAD_REQUEST);
			}
			return false;
		}

		final boolean isGenerated = jpaAttribute.getAnnotation(GeneratedValue.class) != null;
		// do not allow to set ID attributes if that attributes must be generated
		if (isGenerated && jpaAttribute.isKey()) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR,
					new IllegalArgumentException("The id attribute must not be set, because is generated"));
		}
		final Object jpaPropertyValue = determineSimpleOData2JPAProperty(jpaEntityType, jpaAttribute,
				sourceOdataProperty);
		jpaAttribute.getAttributeAccessor().setPropertyValue(targetJPAObject, jpaPropertyValue);
		return true;
	}

	private Object determineEmbeddedIdOData2JPAProperty(final JPAStructuredType jpaEntityType,
			final JPAAttribute jpaAttribute, final Object embeddedIdFieldObjectOrNull,
			final Collection<Property> odataObjectProperties)
					throws ODataJPAModelException, ODataApplicationException, NoSuchFieldException, IllegalArgumentException,
					IllegalAccessException {
		final JPAStructuredType embeddedIdFieldType = jpaAttribute.getStructuredType();
		Object embeddedIdFieldObject = embeddedIdFieldObjectOrNull;
		if (embeddedIdFieldObject == null) {
			embeddedIdFieldObject = newJPAInstance(embeddedIdFieldType);
		}
		for (final JPAAttribute jpaEmbeddedIdAttribute : embeddedIdFieldType.getAttributes()) {
			final boolean done = convertOData2JPAProperty(embeddedIdFieldObject, embeddedIdFieldType,
					jpaEmbeddedIdAttribute, odataObjectProperties);
			if (!done) {
				LOG.log(Level.SEVERE, "Missing key attribute value: " + jpaEntityType.getExternalName() + "/"
						+ jpaAttribute.getExternalName() + "#" + jpaEmbeddedIdAttribute.getExternalName());
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.BAD_REQUEST);
			}
		}
		return embeddedIdFieldObject;
	}

	private boolean convertEmbeddedIdOData2JPAProperty(final Object targetJPAObject,
			final JPAStructuredType jpaEntityType,
			final JPAAttribute jpaAttribute, final Collection<Property> odataObjectProperties)
					throws ODataJPAModelException, ODataApplicationException, NoSuchFieldException, IllegalArgumentException,
					IllegalAccessException {
		final Object embeddedIdFieldObject = jpaAttribute.getAttributeAccessor().getPropertyValue(targetJPAObject);
		final Object embeddedIdFieldObjectFilled = determineEmbeddedIdOData2JPAProperty(jpaEntityType, jpaAttribute,
				embeddedIdFieldObject, odataObjectProperties);
		if (embeddedIdFieldObject == null) {
			jpaAttribute.getAttributeAccessor().setPropertyValue(targetJPAObject, embeddedIdFieldObjectFilled);
		}
		return true;
	}

	private boolean convertComplexOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType,
			final JPAAttribute jpaAttribute, final Property sourceOdataProperty)
					throws ODataJPAModelException, ODataApplicationException, NoSuchFieldException, IllegalArgumentException,
					IllegalAccessException {
		if (sourceOdataProperty == null) {
			return false;
		}
		if (!sourceOdataProperty.isComplex()) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		final Object embeddedFieldObject = jpaAttribute.getAttributeAccessor().getPropertyValue(targetJPAObject);
		if (embeddedFieldObject == null) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.EXPECTATION_FAILED);
		}
		final JPAStructuredType embeddedJPAType = jpaAttribute.getStructuredType();
		boolean done = true;
		if (sourceOdataProperty.isCollection()) {
			// manage structured types in a collection
			@SuppressWarnings("unchecked")
			final Collection<Object> collectionOfComplexTypes = (Collection<Object>) embeddedFieldObject;
			if (!collectionOfComplexTypes.isEmpty()) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
						HttpStatusCode.EXPECTATION_FAILED);
			}
			for (final Object entry : sourceOdataProperty.asCollection()) {
				final Object embeddedJPAInstance = newJPAInstance(embeddedJPAType);
				final List<Property> listEmbeddedProperties = ((ComplexValue) entry).getValue();
				for (final Property embeddedProperty : listEmbeddedProperties) {
					final JPAAttribute embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName()).getLeaf();
					convertOData2JPAProperty(embeddedJPAInstance, embeddedJPAType, embeddedJPAAttribute,
							listEmbeddedProperties);
				}
				collectionOfComplexTypes.add(embeddedJPAInstance);
			}
		} else {
			// single structured type attribute
			for (final Property embeddedProperty : sourceOdataProperty.asComplex().getValue()) {
				final JPAAttribute embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName()).getLeaf();
				done = done && convertOData2JPAProperty(embeddedFieldObject, embeddedJPAType, embeddedJPAAttribute,
						Collections.singletonList(embeddedProperty));
			}
		}
		return done;
	}

	/**
	 *
	 * @param targetJPAObject       The JPA related instance or DTO instance.
	 * @param jpaEntityType         The meta model description of JPA related
	 *                              object.
	 * @param jpaAttribute          The JPA related attribute to process.
	 * @param odataObjectProperties The list of all OData related properties as
	 *                              source for conversion.
	 * @return TRUE if conversion has done something (The OData property was not
	 *         <code>null</code>).
	 * @throws ODataApplicationException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws ODataJPAModelException
	 */
	protected boolean convertOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType,
			final JPAAttribute jpaAttribute, final Collection<Property> odataObjectProperties)
					throws ODataApplicationException,
					NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ODataJPAModelException {

		Property sourceOdataProperty;
		switch (jpaAttribute.getAttributeMapping()) {
		case SIMPLE:
			sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
			return convertSimpleOData2JPAProperty(targetJPAObject, jpaEntityType, jpaAttribute, sourceOdataProperty);
		case AS_COMPLEX_TYPE:
			sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
			return convertComplexOData2JPAProperty(targetJPAObject, jpaEntityType, jpaAttribute, sourceOdataProperty);
		case EMBEDDED_ID:
			return convertEmbeddedIdOData2JPAProperty(targetJPAObject, jpaEntityType, jpaAttribute,
					odataObjectProperties);
		case RELATIONSHIP:
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		default:
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 *
	 * @return The JPA related value for attribute/property or <code>null</code>.
	 * @throws ODataJPAProcessorException For unsupported attribute mappings.
	 *
	 * @see #convertOData2JPAProperty(Object, JPAStructuredType, JPAAttribute,
	 *      Collection)
	 */
	public Object determineOData2JPAProperty(final JPAStructuredType jpaEntityType, final JPAAttribute jpaAttribute,
			final Collection<Property> odataObjectProperties) throws ODataApplicationException, NoSuchFieldException,
	IllegalArgumentException, IllegalAccessException, ODataJPAModelException {

		Property sourceOdataProperty;
		switch (jpaAttribute.getAttributeMapping()) {
		case SIMPLE:
			sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
			return determineSimpleOData2JPAProperty(jpaEntityType, jpaAttribute, sourceOdataProperty);
		case AS_COMPLEX_TYPE:
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		case EMBEDDED_ID:
			return determineEmbeddedIdOData2JPAProperty(jpaEntityType, jpaAttribute, null, odataObjectProperties);
		case RELATIONSHIP:
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		default:
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 *
	 * @return The property of requested name or <code>null</code>.
	 */
	private Property selectProperty(final Collection<Property> odataObjectProperties, final String propertyname) {
		for (final Property p : odataObjectProperties) {
			if (propertyname.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Convert a <b>simple</b> OData attribute value into a JPA entity attribute
	 * type matching one.
	 *
	 * @param jpaElement        The affected attribute/parameter description.
	 * @param sourceOdataProperty The OData attribute value.
	 * @return The JPA attribute type compliant instance value.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object convertOData2JPAPropertyValue(final JPATypedElement jpaElement, final Valuable sourceOdataProperty)
			throws ODataJPAModelException {
		final Object odataPropertyValue = sourceOdataProperty.getValue();// assume primitive value
		if (odataPropertyValue == null) {
			return null;
		}
		final Class<?> javaType = jpaElement.getType();
		if (javaType.isEnum() && Number.class.isInstance(odataPropertyValue)) {
			// convert enum ordinal value into enum literal
			return lookupEnum((Class<Enum>) javaType, ((Number) odataPropertyValue).intValue());
		}
		final Class<?> oadataType = odataPropertyValue.getClass();
		if (javaType.equals(oadataType)) {
			return odataPropertyValue;
		}
		final ODataAttributeConverter<Object, Object> converter = determineODataAttributeConverter(jpaElement, oadataType);
		if (converter != null) {
			return converter.convertToJPA(odataPropertyValue);
		}
		// no conversion
		return odataPropertyValue;
	}

	private static <E extends Enum<E>> E lookupEnum(final Class<E> clzz, final int ordinal) {
		final EnumSet<E> set = EnumSet.allOf(clzz);
		if (ordinal < set.size()) {
			final Iterator<E> iter = set.iterator();
			for (int i = 0; i < ordinal; i++) {
				iter.next();
			}
			final E rval = iter.next();
			assert (rval.ordinal() == ordinal);
			return rval;
		}
		throw new IllegalArgumentException("Invalid value " + ordinal + " for " + clzz.getName() + ", must be < " + set.size());
	}

	/**
	 *
	 * @param jpaEntityType
	 *            The type of object to create instance of.
	 * @return The new instance.
	 * @throws ODataJPAModelException
	 *             If construction of new instance failed.
	 */
	protected Object newJPAInstance(final JPAStructuredType jpaEntityType) throws ODataJPAModelException {
		try {
			return jpaEntityType.getTypeClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL, e);
		}
	}

}
