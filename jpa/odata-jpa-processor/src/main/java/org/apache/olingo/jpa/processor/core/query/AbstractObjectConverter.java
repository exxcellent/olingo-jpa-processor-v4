package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.http.HttpStatusCode;
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

public abstract class AbstractObjectConverter extends AbstractJPAODataConverter {

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
			final JPAStructuredType jpaType, final JPAAttribute<?> jpaAttribute, final String baseAttributePath)
					throws ODataJPAModelException,
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
					(Class<Object>) ((JPASimpleAttribute) jpaAttribute).getType());
			return Collections.singletonList(element);
		}
	}

}
