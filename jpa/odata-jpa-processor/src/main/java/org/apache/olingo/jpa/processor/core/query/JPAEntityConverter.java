package org.apache.olingo.jpa.processor.core.query;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPANameBuilder;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.ServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAEntityConverter extends JPAAbstractConverter {

	public class TupleElementFacade<X> implements TupleElement<X> {
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

	private static class TupleFacade<X> implements Tuple {
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

	private final Metamodel metamodel;
	private final EntityType<?> persistenceType;

	public JPAEntityConverter(final EntityType<?> persistenceType, final UriHelper uriHelper,
			final ServiceDocument sd, final ServiceMetadata serviceMetadata, final Metamodel metamodel)
					throws ODataApplicationException, ODataJPAModelException {
		super(determineJPAEntityType(sd, persistenceType), uriHelper, sd, serviceMetadata);
		this.metamodel = metamodel;
		this.persistenceType = persistenceType;
	}

	public final static JPAEntityType determineJPAEntityType(final ServiceDocument sd, final EntityType<?> persistenceType) throws ODataJPAModelException {
		FullQualifiedName fqn;
		JPAEntityType jpaType;
		for(final CsdlSchema schema: sd.getEdmSchemas()) {
			fqn = new FullQualifiedName(schema.getNamespace(), persistenceType.getName());
			jpaType = sd.getEntityType(fqn);
			if(jpaType != null) {
				return jpaType;
			}
		}
		throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_RETURN_TYPE_ENTITY_NOT_FOUND);
	}

	@Override
	protected Collection<? extends Link> createExpand(final Tuple row, final URI uri) throws ODataApplicationException {
		// TODO how to 'expand' aggregated entities in JPA objects?
		return Collections.emptyList();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void convertOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType, final ManagedType<?> persistenceType, final JPAAttribute jpaAttribute, final Property sourceOdataProperty) throws ODataApplicationException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ODataJPAModelException {
		final Attribute<?, ?> persistenceAttribute = persistenceType.getAttribute(jpaAttribute.getInternalName());

		if (jpaAttribute.isAssociation()) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		} else if (jpaAttribute.isComplex()) {
			if(!sourceOdataProperty.isComplex()) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
			final Object embeddedFieldObject = readJPAValue(targetJPAObject, targetJPAObject.getClass(), jpaAttribute.getInternalName());
			if(embeddedFieldObject == null) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR, HttpStatusCode.EXPECTATION_FAILED);
			}
			final JPAStructuredType embeddedJPAType = jpaAttribute.getStructuredType();
			if (sourceOdataProperty.isCollection()) {
				// manage structured types in a collection
				final Collection<Object> collectionOfComplexTypes = (Collection<Object>) embeddedFieldObject;
				if (!collectionOfComplexTypes.isEmpty()) {
					throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR, HttpStatusCode.EXPECTATION_FAILED);
				}
				final ManagedType<?> embeddedPersistenceType = metamodel.managedType(embeddedJPAType.getTypeClass());
				for (final Object entry : sourceOdataProperty.asCollection()) {
					final Object embeddedJPAInstance = newJPAInstance(embeddedJPAType);
					for (final Property embeddedProperty : ((ComplexValue) entry).getValue()) {
						final JPAAttribute embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName())
								.getLeaf();
						convertOData2JPAProperty(embeddedJPAInstance, embeddedJPAType, embeddedPersistenceType,
								embeddedJPAAttribute, embeddedProperty);
					}
					collectionOfComplexTypes.add(embeddedJPAInstance);
				}
			} else {
				// single structured type attribute
				for(final Property embeddedProperty: sourceOdataProperty.asComplex().getValue()) {
					final JPAAttribute embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName()).getLeaf();
					final ManagedType<?> embeddedPersistenceType = metamodel.managedType(embeddedJPAType.getTypeClass());
					convertOData2JPAProperty(embeddedFieldObject, embeddedJPAType, embeddedPersistenceType, embeddedJPAAttribute, embeddedProperty);
				}
			}
		} else {
			// convert simple/primitive value
			Object jpaPropertyValue = sourceOdataProperty.getValue();// assume primitive value
			final boolean isGenerated = ((Field)persistenceAttribute.getJavaMember()).getAnnotation(GeneratedValue.class) != null;
			// do not allow to set ID attributes if that attributes must be generated
			if(isGenerated && SingularAttribute.class.isInstance(persistenceAttribute) && ((SingularAttribute<?,?>)persistenceAttribute).isId()) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR, HttpStatusCode.INTERNAL_SERVER_ERROR, new IllegalArgumentException("The id attribute must not be set, because is generated"));
			}
			if (persistenceAttribute.getJavaType().isEnum() && Number.class.isInstance(jpaPropertyValue)) {
				// convert enum ordinal value into enum literal
				jpaPropertyValue = lookupEnum((Class<Enum>) persistenceAttribute.getJavaType(),
						((Number) jpaPropertyValue).intValue());
			}
			writeJPAValue(targetJPAObject, persistenceAttribute, jpaPropertyValue);
		}

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
		throw new IllegalArgumentException(
				"Invalid value " + ordinal + " for " + clzz.getName() + ", must be < " + set.size());
	}

	/**
	 * Convert a OData entity into a JPA entity.
	 */
	public Object convertOData2JPAEntity(final Entity entity) throws ODataApplicationException {
		final JPAEntityType jpaEntityType = getJpaEntityType();
		if(!jpaEntityType.getExternalFQN().getFullQualifiedNameAsString().equals(entity.getType())) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		try {
			final Object targetJPAInstance = newJPAInstance(jpaEntityType);
			for(final JPAAttribute jpaAttribute: jpaEntityType.getAttributes()) {
				final Property sourceOdataProperty = entity.getProperty(jpaAttribute.getExternalName());
				if(sourceOdataProperty == null) {
					continue;
				}
				convertOData2JPAProperty(targetJPAInstance, jpaEntityType, persistenceType, jpaAttribute, sourceOdataProperty);
			}
			return targetJPAInstance;
		} catch (ODataJPAModelException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR, HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 *
	 * @param jpaEntityType The type of object to create instance of.
	 * @return The new instance.
	 * @throws ODataJPAModelException If construction of new instance failed.
	 */
	protected Object newJPAInstance(final JPAStructuredType jpaEntityType) throws ODataJPAModelException {
		try {
			return jpaEntityType.getTypeClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL, e);
		}
	}

	/**
	 * Convert an object managed by the {@link EntityManager entity manager} into a OData entity representation.
	 */
	public Entity convertJPA2ODataEntity(final Object jpaEntity) throws ODataApplicationException {
		final Collection<TupleElementFacade<Object>> elements = convertJPAStructuredType(jpaEntity, getJpaEntityType(), persistenceType, "");
		final Tuple tuple = new TupleFacade<Object>(elements);
		return convertRow2ODataEntity(tuple, new EntityCollection());
	}

	private Collection<TupleElementFacade<Object>> convertJPAStructuredType(final Object persistenceObject, final JPAStructuredType jpaType, final ManagedType<?> persistenceType, final String baseAttributePath) throws ODataJPAProcessorException {
		if(jpaType == null || persistenceType == null) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		final Collection<TupleElementFacade<Object>> elements = new LinkedList<>();
		Collection<TupleElementFacade<Object>> complexAttributeElements;
		for (final Attribute<?, ?> attribute : persistenceType.getAttributes()) {
			try {
				final String internalName = JPANameBuilder.buildAttributeName(attribute);
				JPAAttribute jpaAttribute = jpaType.getAttribute(internalName);
				if(jpaAttribute == null) {
					jpaAttribute = jpaType.getAssociation(internalName);
				}
				if(jpaAttribute == null) {
					// 'ignored' attributes are also hidden to us, so we cannot throw an exception, we have to continue...
					continue;
				}
				complexAttributeElements = convertAttribute2TupleElements(persistenceObject, jpaType, jpaAttribute, baseAttributePath);
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

	private Collection<TupleElementFacade<Object>> convertAttribute2TupleElements(final Object persistenceObject, final JPAStructuredType jpaType, final JPAAttribute jpaAttribute, final String baseAttributePath) throws ODataJPAModelException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ODataJPAProcessorException {
		final Object value = readJPAValue(persistenceObject, persistenceObject.getClass(), jpaAttribute.getInternalName());
		final String alias = jpaAttribute.getExternalName();
		if(jpaAttribute.isAssociation() && value != null) {
			// TODO: how we can support navigation to other entities while converting a single entity?
			if (Collection.class.isInstance(value) && ((Collection<?>) value).isEmpty()) {
				// we are lucky... a empty collection is no problem
				return Collections.emptyList();
			}
			// in all other cases give up
			final IllegalStateException throwable = new IllegalStateException("Attribute " + alias + " of "
					+ jpaType.getExternalName() + " contains unsupported association content");
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ASSOCIATION, throwable);
		} else if(jpaAttribute.isComplex()) {
			//ignore complex types that are not set (null)
			if(value == null) {
				return Collections.emptyList();
			}
			// create as navigation path to nested complex path property
			final JPAAttributePath path = jpaType.getPath(alias);
			if(path == null) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_COMPLEX_TYPE);
			}
			final ManagedType<?> persistenceEmbeddedType = metamodel.managedType(jpaAttribute.getType());
			final JPAStructuredType jpaEmbeddedType = path.getLeaf().getStructuredType();
			final String newPath = baseAttributePath.concat(alias).concat(JPASelector.PATH_SEPERATOR);
			if (jpaAttribute.isCollection()) {
				final Collection<TupleElementFacade<Object>> elements = new LinkedList<>();
				for (final Object entry : ((Collection<?>) value)) {
					elements.addAll(convertJPAStructuredType(entry, jpaEmbeddedType, persistenceEmbeddedType, newPath));
				}
				return elements;
			} else {
				return convertJPAStructuredType(value,jpaEmbeddedType, persistenceEmbeddedType, newPath);
			}
		} else {
			// simple attribute
			@SuppressWarnings("unchecked")
			final TupleElementFacade<Object> element = new TupleElementFacade<Object>(baseAttributePath.concat(alias), value,
					(Class<Object>) jpaAttribute.getType());
			return Collections.singletonList(element);
		}
	}

	private void writeJPAValue(final Object jpaEntity, final Attribute<?, ?> attribute, final Object jpaPropertyValue) throws IllegalArgumentException, IllegalAccessException {
		boolean revertAccessibility = false;
		final Field field = (Field) attribute.getJavaMember();
		if(!field.isAccessible()) {
			field.setAccessible(true);
			revertAccessibility = true;
		}
		if (attribute.isCollection() && Collection.class.isInstance(jpaPropertyValue) && field.get(jpaEntity) != null) {
			// do not set the collection directly, because some specific implementations may
			// cause problems... add entries in collection instead
			@SuppressWarnings("unchecked")
			final Collection<Object> target = (Collection<Object>) field.get(jpaEntity);
			@SuppressWarnings("unchecked")
			final Collection<Object> source = (Collection<Object>) jpaPropertyValue;
			target.addAll(source);
		} else {
			field.set(jpaEntity, jpaPropertyValue);
		}
		if(revertAccessibility) {
			field.setAccessible(false);
		}
	}

	private Object readJPAValue(final Object object, final Class<?> jpaClassType, final String fieldName)
			throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		boolean revertAccessibility = false;
		try {
			final Field field = jpaClassType.getDeclaredField(fieldName);
			if(!field.isAccessible()) {
				field.setAccessible(true);
				revertAccessibility = true;
			}
			final Object value = field.get(object);
			if(revertAccessibility) {
				field.setAccessible(false);
			}
			return value;

		} catch (final NoSuchFieldException ex) {
			if (jpaClassType.getSuperclass() != null) {
				return readJPAValue(object, jpaClassType.getSuperclass(), fieldName);
			}
			// if not found in super classes, throw the exception
			throw ex;
		}
	}
}
