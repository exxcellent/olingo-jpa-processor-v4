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

import javax.persistence.GeneratedValue;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
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

	@SuppressWarnings({ "unchecked" })
	protected void convertOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType,
	        /* final ManagedType<?> persistenceType, */ final JPAAttribute jpaAttribute, final Property sourceOdataProperty)
	        throws ODataApplicationException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException,
	        ODataJPAModelException {
		if (jpaAttribute.isAssociation()) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
			        HttpStatusCode.INTERNAL_SERVER_ERROR);
		} else if (jpaAttribute.isComplex()) {
			if (!sourceOdataProperty.isComplex()) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
				        HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
			final Object embeddedFieldObject = jpaAttribute.getAttributeAccessor().getPropertyValue(targetJPAObject);
			// final Object embeddedFieldObject = readJPAFieldValue(targetJPAObject,
			// targetJPAObject.getClass(), jpaAttribute.getInternalName());
			if (embeddedFieldObject == null) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
				        HttpStatusCode.EXPECTATION_FAILED);
			}
			final JPAStructuredType embeddedJPAType = jpaAttribute.getStructuredType();
			if (sourceOdataProperty.isCollection()) {
				// manage structured types in a collection
				final Collection<Object> collectionOfComplexTypes = (Collection<Object>) embeddedFieldObject;
				if (!collectionOfComplexTypes.isEmpty()) {
					throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					        HttpStatusCode.EXPECTATION_FAILED);
				}
				// final ManagedType<?> embeddedPersistenceType =
				// metamodel.managedType(embeddedJPAType.getTypeClass());
				for (final Object entry : sourceOdataProperty.asCollection()) {
					final Object embeddedJPAInstance = newJPAInstance(embeddedJPAType);
					for (final Property embeddedProperty : ((ComplexValue) entry).getValue()) {
						final JPAAttribute embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName()).getLeaf();
						convertOData2JPAProperty(embeddedJPAInstance, embeddedJPAType/* , embeddedPersistenceType */, embeddedJPAAttribute,
						        embeddedProperty);
					}
					collectionOfComplexTypes.add(embeddedJPAInstance);
				}
			} else {
				// single structured type attribute
				for (final Property embeddedProperty : sourceOdataProperty.asComplex().getValue()) {
					final JPAAttribute embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName()).getLeaf();
					// final ManagedType<?> embeddedPersistenceType =
					// metamodel.managedType(embeddedJPAType.getTypeClass());
					convertOData2JPAProperty(embeddedFieldObject, embeddedJPAType/* , embeddedPersistenceType */, embeddedJPAAttribute,
					        embeddedProperty);
				}
			}
		} else {
			final boolean isGenerated = jpaAttribute.getAnnotation(GeneratedValue.class) != null;
			// do not allow to set ID attributes if that attributes must be generated
			if (isGenerated && jpaAttribute.isKey()/*
			                                        * SingularAttribute.class.isInstance(persistenceAttribute) &&
			                                        * ((SingularAttribute<?,?>)persistenceAttribute).isId()
			                                        */) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
				        HttpStatusCode.INTERNAL_SERVER_ERROR,
				        new IllegalArgumentException("The id attribute must not be set, because is generated"));
			}
			// convert simple/primitive value
			final Object jpaPropertyValue = convertOData2JPAPropertyValue(jpaAttribute, sourceOdataProperty);
			jpaAttribute.getAttributeAccessor().setPropertyValue(targetJPAObject, jpaPropertyValue);
		}
	}

	/**
	 * Convert a <b>simple</b> OData attribute value into a JPA entity attribute type matching one.
	 *
	 * @param jpaAttribute
	 *            The affected attribute description.
	 * @param sourceOdataProperty
	 *            The OData attribute value.
	 * @return The JPA attribute type compliant instance value.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object convertOData2JPAPropertyValue(final JPAAttribute jpaAttribute, final Property sourceOdataProperty)
	        throws ODataJPAModelException {
		final Object odataPropertyValue = sourceOdataProperty.getValue();// assume primitive value
		if (odataPropertyValue == null)
			return null;
		final Class<?> javaType = jpaAttribute.getType();
		if (javaType.isEnum() && Number.class.isInstance(odataPropertyValue)) {
			// convert enum ordinal value into enum literal
			return lookupEnum((Class<Enum>) javaType, ((Number) odataPropertyValue).intValue());
		}
		final Class<?> oadataType = odataPropertyValue.getClass();
		if (javaType.equals(oadataType))
			return odataPropertyValue;
		final ODataAttributeConverter<Object, Object> converter = determineODataAttributeConverter(jpaAttribute, oadataType);
		if (converter != null)
			return converter.convertToJPAEntity(odataPropertyValue);
		// no conversion
		return odataPropertyValue;
	}

	/**
	 * Look for any matching converter, including default implementation for some data type combinations.
	 *
	 * @return A found converter or <code>null</code> if no converter is available.
	 */
	@SuppressWarnings("unchecked")
	private ODataAttributeConverter<Object, Object> determineODataAttributeConverter(final JPAAttribute jpaAttribute,
	        final Class<?> odataAttributeType) throws ODataJPAModelException {
		final EdmAttributeConverter annoConverter = jpaAttribute.getAnnotation(EdmAttributeConverter.class);
		if (annoConverter != null) {
			try {
				return (ODataAttributeConverter<Object, Object>) annoConverter.value().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_MAPPER_COULD_NOT_INSANTIATE, e);
			}
		}
		// look for default converter
		return determineDefaultODataAttributeConverter(odataAttributeType, jpaAttribute.getType());
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
