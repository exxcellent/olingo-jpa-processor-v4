package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TypeMapping;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDateTime2SqlTimestampODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalTime2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.SqlDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.UtilDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;

public abstract class JPAAbstractConverter {

	private static class ConverterMapping {

		private final Class<?> odataAttributeType;
		private final Class<?> jpaAttributeType;
		private final boolean matchSubClasses4JPA;
		private final ODataAttributeConverter<Object, Object> converterInstance;

		@SuppressWarnings("unchecked")
		ConverterMapping(final Class<?> odataAttributeType, final Class<?> jpaAttributeType,
				final boolean matchSubClassesForJPA,
				@SuppressWarnings("rawtypes") final ODataAttributeConverter converterInstance) {
			this.odataAttributeType = odataAttributeType;
			this.jpaAttributeType = jpaAttributeType;
			this.matchSubClasses4JPA = matchSubClassesForJPA;
			this.converterInstance = converterInstance;
		}

		ODataAttributeConverter<Object, Object> getConverterInstance() {
			return converterInstance;
		}

		boolean isMatching(final Class<?> odataAttributeType, final Class<?> jpaAttributeType) {
			if (!this.odataAttributeType.isAssignableFrom(odataAttributeType)) {
				return false;
			}
			if (matchSubClasses4JPA) {
				return this.jpaAttributeType.isAssignableFrom(jpaAttributeType);
			} else {
				return this.jpaAttributeType.equals(jpaAttributeType);
			}
		}
	}

	protected final static Logger LOG = Logger.getLogger(JPAAbstractConverter.class.getName());

	private final static Collection<ConverterMapping> DEFAULT_ODATA_ATTRIBUTE_CONVERTERS = new LinkedList<>();

	static {
		DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class,
				java.sql.Date.class, false,
				new SqlDate2UtilCalendarODataAttributeConverter()));
		DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class, java.util.Date.class,
				false, new UtilDate2UtilCalendarODataAttributeConverter()));
		DEFAULT_ODATA_ATTRIBUTE_CONVERTERS
		.add(new ConverterMapping(java.util.Calendar.class, java.time.LocalDate.class, true,
				new LocalDate2UtilCalendarODataAttributeConverter()));
		DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class,
				java.time.LocalTime.class, true, new LocalTime2UtilCalendarODataAttributeConverter()));
		DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.sql.Timestamp.class,
				java.time.LocalDateTime.class, true, new LocalDateTime2SqlTimestampODataAttributeConverter()));
	}

	protected final JPAEntityType jpaConversionTargetEntity;
	protected final IntermediateServiceDocument sd;
	protected final ServiceMetadata serviceMetadata;
	protected final Logger log = Logger.getLogger(JPAAbstractConverter.class.getName());
	private final UriHelper uriHelper;
	@SuppressWarnings("rawtypes")
	private final Map<String, ODataAttributeConverter> converterLookupCache = new HashMap<>();

	public JPAAbstractConverter(final JPAEntityType jpaConversionTargetEntity, final UriHelper uriHelper,
			final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata) throws ODataApplicationException {
		super();

		this.jpaConversionTargetEntity = jpaConversionTargetEntity;
		this.uriHelper = uriHelper;
		this.sd = sd;
		this.serviceMetadata = serviceMetadata;
	}

	private static String buildConverterKey(final Class<?> odataAttributeType, final Class<?> jpaAttributeType) {
		return odataAttributeType.getName().concat("<->").concat(jpaAttributeType.getName());
	}

	/**
	 * Look for any matching converter, including default implementation for some
	 * data type combinations.
	 *
	 * @param jpaElement The attribute to look for an assigned converter.
	 *
	 * @return A found converter or <code>null</code> if no converter is available.
	 */
	@SuppressWarnings("unchecked")
	protected ODataAttributeConverter<Object, Object> determineODataAttributeConverter(final JPATypedElement jpaElement,
			final Class<?> odataAttributeType) throws ODataJPAModelException {
		final EdmAttributeConversion annoConverter = jpaElement.getAnnotation(EdmAttributeConversion.class);
		if (annoConverter != null) {
			try {
				if (!EdmAttributeConversion.DEFAULT.class.equals(annoConverter.converter())) {
					return (ODataAttributeConverter<Object, Object>) annoConverter.converter().newInstance();
				}
			} catch (InstantiationException | IllegalAccessException e) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_MAPPER_COULD_NOT_INSANTIATE,
						e);
			}
		}
		// look for default converter
		return determineDefaultODataAttributeConverter(jpaElement.getType(), odataAttributeType);
	}

	@SuppressWarnings("unchecked")
	private ODataAttributeConverter<Object, Object> determineDefaultODataAttributeConverter(
			final Class<?> jpaAttributeType,
			final Class<?> odataAttributeType) {
		final String key = buildConverterKey(odataAttributeType, jpaAttributeType);
		if (converterLookupCache.containsKey(key)) {
			return converterLookupCache.get(key);
		}
		// lookup...
		final List<ODataAttributeConverter<Object, Object>> matchingConverters = new LinkedList<>();
		for (final ConverterMapping mapping : DEFAULT_ODATA_ATTRIBUTE_CONVERTERS) {
			if (!mapping.isMatching(odataAttributeType, jpaAttributeType)) {
				continue;
			}
			matchingConverters.add(mapping.getConverterInstance());
		}
		if (matchingConverters.size() > 1) {
			LOG.log(Level.WARNING, "Multiple default converters are matching for " + odataAttributeType.getSimpleName() + "<->"
					+ jpaAttributeType.getSimpleName() + ". Will NOT use any of them!");
			return null;
		}
		if (matchingConverters.isEmpty()) {
			converterLookupCache.put(key, null);
			return null;
		}
		final ODataAttributeConverter<Object, Object> converter = matchingConverters.get(0);
		converterLookupCache.put(key, converter);
		return converter;
	}

	protected UriHelper getUriHelper() {
		return uriHelper;
	}

	protected JPAEntityType getJpaEntityType() {
		return jpaConversionTargetEntity;
	}

	/**
	 * The given row is converted into a OData entity and added to the
	 * {@link EntityCollection#getEntities() list of entities} of entity
	 * collection.<br/>
	 * If the <code>row</code> is defining a entity that is already part of entity
	 * collection (that happens if the {@link JPAQueryResult} is built from joined
	 * tables) then the entity values are merged into the exiting entity and hat
	 * already processed entity is returned; otherwise a new entity is created.
	 */
	protected final Entity convertRow2ODataEntity(final Tuple row, final EntityCollection odataEntityCollection)
			throws ODataApplicationException {

		final List<Entity> odataResults = odataEntityCollection.getEntities();

		final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
		Entity odataEntity = new Entity();
		odataEntity.setType(jpaConversionTargetEntity.getExternalFQN().getFullQualifiedNameAsString());
		final List<Property> properties = odataEntity.getProperties();
		// TODO store @Version to fill ETag Header
		for (final TupleElement<?> element : row.getElements()) {
			try {
				convertJPA2ODataAttribute(row.get(element.getAlias()), element.getAlias(), "", jpaConversionTargetEntity,
						complexValueBuffer, properties);
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
		}
		odataEntity.setId(createId(odataEntity));
		for (final String attribute : complexValueBuffer.keySet()) {
			final Object entry = complexValueBuffer.get(attribute);
			if (entry instanceof ComplexValue) {
				((ComplexValue) entry).getNavigationLinks().addAll(createExpand(row, odataEntity.getId()));
			} else if (entry instanceof List) {
				// TODO: can a list of complex values (types) have navigation links?
			}
		}
		odataEntity.getNavigationLinks().addAll(createExpand(row, odataEntity.getId()));

		final Entity odataExisting = findExistingEntity(odataEntity.getId(), odataEntityCollection);
		if (odataExisting != null) {
			mergeEntity(odataEntity, odataExisting);
			odataEntity = odataExisting;
		} else {
			odataResults.add(odataEntity);
		}

		return odataEntity;
	}

	private Entity findExistingEntity(final URI idUri, final EntityCollection odataEntityCollection) {
		final List<Entity> odataResults = odataEntityCollection.getEntities();
		for (final Entity entity : odataResults) {
			if (entity.getId().equals(idUri)) {
				return entity;
			}
		}
		return null;
	}

	private void mergeEntity(final Entity from, final Entity to) throws ODataApplicationException {
		// navigation links (associations) must be (already) the same, so we can simply
		// check that state
		if (from.getNavigationLinks().size() != to.getNavigationLinks().size()) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		final List<Property> propertiesTo = to.getProperties();
		List<JPASimpleAttribute> attributes = null;
		try {
			attributes = jpaConversionTargetEntity.getAttributes();
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}
		for (final JPASimpleAttribute attribute : attributes) {
			// we have to merge only collections and x-to-many properties resulting from
			// JOIN queries

			final Property propertyFrom = from.getProperty(attribute.getExternalName());
			if (propertyFrom == null) {
				if (to.getProperty(attribute.getExternalName()) != null) {
					throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
							HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
				continue;
			}
			// resolve join from @EntityCollection into one entity
			if (attribute.isCollection()) {
				if (attribute.isComplex()) {
					mergeComplexTypeCollection(propertyFrom, to.getProperty(attribute.getExternalName()));
				} else {
					try {
						convertJPA2ODataPropertyValue(attribute, attribute.getExternalName(), propertyFrom.getValue(), propertiesTo);
					} catch (final ODataJPAModelException e) {
						throw new ODataJPAProcessorException(
								ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
								HttpStatusCode.INTERNAL_SERVER_ERROR, e);
					}
				}
			}
		}
	}

	private void mergeComplexTypeCollection(final Property propertyFrom, final Property propertyTo) throws ODataApplicationException {
		@SuppressWarnings("unchecked")
		final List<ComplexValue> entriesFrom = (List<ComplexValue>) propertyFrom.asCollection();
		@SuppressWarnings("unchecked")
		final List<ComplexValue> entriesTo = (List<ComplexValue>) propertyTo.asCollection();

		for (final ComplexValue existingFrom : entriesFrom) {
			final ComplexValue existingTo = findExistingComplexValue(existingFrom, entriesTo);
			if (existingTo != null) {
				continue;
			}
			// merge the existingFrom into target list
			// TODO: do we have to process navigation properties in that type in a special
			// way, because some entities can be referenced?
			entriesTo.add(existingFrom);
		}
	}

	/**
	 * Search the given <i>objectToFind</i> in the list of other complex types. The
	 * type is identified by equality of simple attributes.
	 *
	 * @return The found type in list or <code>null</code> if not found.
	 */
	private ComplexValue findExistingComplexValue(final ComplexValue objectToFind, final Collection<ComplexValue> listExistingTypes) {

		for (final ComplexValue existing : listExistingTypes) {
			if (!isSameComplexValue(existing, objectToFind, true)) {
				continue;
			}
			return existing;
		}
		return null;
	}

	@SuppressWarnings("null")
	private boolean isSameComplexValue(final ComplexValue one, final ComplexValue second, final boolean recursive) {
		final List<Property> propertiesOne = one.getValue();
		final List<Property> propertiesSecond = second.getValue();
		if (propertiesOne.size() != propertiesSecond.size()) {
			return false;
		}
		Property propertyOne;
		Property propertySecond;
		List<?> listValuesOne;
		List<?> listValuesSecond;
		Object valueOne;
		Object valueTwo;
		int j;
		// assuming same order of properties
		for (int i = 0; i < propertiesOne.size(); i++) {
			propertyOne = propertiesOne.get(i);
			propertySecond = propertiesSecond.get(i);
			if (propertyOne.isCollection()) {
				if (propertyOne.isComplex()) {
					// complex collection
					listValuesOne = propertyOne.asCollection();
					listValuesSecond = propertySecond.asCollection();
					if (listValuesOne.size() != listValuesSecond.size()) {
						return false;
					}
					for (j = 0; j < listValuesOne.size(); j++) {
						if (!isSameComplexValue((ComplexValue) listValuesOne.get(j), (ComplexValue) listValuesSecond.get(j), recursive)) {
							return false;
						}
					}
				} else if (propertyOne.isPrimitive()) {
					// primitive collection
					listValuesOne = propertyOne.asCollection();
					listValuesSecond = propertySecond.asCollection();
					if (listValuesOne.size() != listValuesSecond.size()) {
						return false;
					}
					for (j = 0; j < listValuesOne.size(); j++) {
						valueOne = listValuesOne.get(j);
						valueTwo = listValuesSecond.get(j);
						if (valueOne == null && valueTwo != null || valueOne != null && valueTwo == null) {
							return false;
						}
						if (valueOne == null && valueTwo == null) {
							continue;
						}
						if (!valueOne.equals(valueTwo)) {
							return false;
						}
					}
				} else {
					log.log(Level.WARNING,
							"Couldn't inspect unsupported property type for " + propertyOne.getName() + "/" + propertyOne.getValueType());
				}
			} else if (propertyOne.isComplex()) {
				if (!recursive) {
					continue;
				}
				if (!isSameComplexValue(propertyOne.asComplex(), propertySecond.asComplex(), recursive)) {
					return false;
				}
			} else if (propertyOne.isEnum()) {
				if (propertyOne.asEnum() != propertySecond.asEnum()) {
					return false;
				}
			} else if (propertyOne.isPrimitive()) {
				if (propertyOne.asPrimitive() == null && propertySecond.asPrimitive() == null) {
					continue;
				}
				if (propertyOne.asPrimitive() == null && propertySecond.asPrimitive() != null
						|| !propertyOne.asPrimitive().equals(propertySecond.asPrimitive())) {
					return false;
				}
			} else {
				log.log(Level.WARNING,
						"Couldn't inspect unsupported property type for " + propertyOne.getName() + "/" + propertyOne.getValueType());
			}
		}
		return true;
	}

	protected abstract Collection<? extends Link> createExpand(final Tuple row, final URI uri) throws ODataApplicationException;

	private final URI createId(final Entity entity) throws ODataApplicationException, ODataRuntimeException {

		final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaConversionTargetEntity.getExternalFQN());
		try {
			// TODO Clarify host-name and port as part of ID see
			// http://docs.oasis-open.org/odata/odata-atom-format/v4.0/cs02/odata-atom-format-v4.0-cs02.html#_Toc372792702

			String setName;
			try {
				setName = sd.getEntitySet(jpaConversionTargetEntity).getExternalName();
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_ENTITY_SET_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, jpaConversionTargetEntity.getExternalFQN().getFullQualifiedNameAsString());
			}
			final StringBuffer uriString = new StringBuffer(setName);
			uriString.append("(");
			uriString.append(uriHelper.buildKeyPredicate(edmType, entity));
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
	 *            A map containing elements of type: {@link ComplexValue} or
	 *            {@link List List&lt;ComplexValue&gt;}.
	 */
	private Property convertJPA2ODataAttribute(final Object value, final String externalName, final String prefix,
			final JPAStructuredType jpaStructuredType, final Map<String, Object> complexValueBuffer, final List<Property> properties)
					throws ODataJPAModelException {

		final JPASelector path = jpaStructuredType.getPath(externalName);
		if (path == null) {
			return null;
		}
		// take only the first, we are working recursive through the path
		final JPAAttribute attribute = path.getPathElements().get(0);
		if (attribute != null && attribute.ignore()) {
			return null;
		}
		if (attribute != null && !attribute.isKey() && attribute.getAttributeMapping() == AttributeMapping.AS_COMPLEX_TYPE) {
			// complex type should never be a 'key'... todo: check that anytime!
			String bufferKey;
			if (prefix.isEmpty()) {
				bufferKey = attribute.getExternalName();
			} else {
				bufferKey = prefix + JPASelector.PATH_SEPERATOR + attribute.getExternalName();
			}
			List<Property> values = null;
			Property complexTypeProperty = null;
			if (attribute.isCollection()) {
				// Remark: One result set row is representing always one entity with maximal ONE
				// of any nested types (JOINED via LEFT JOIN)
				// So we can assume, that only ONE complex type value will assigned to that
				// entity instance we create
				// here...
				@SuppressWarnings("unchecked")
				List<ComplexValue> listOfComplexValues = (List<ComplexValue>) complexValueBuffer.get(bufferKey);
				if (listOfComplexValues == null) {
					listOfComplexValues = new LinkedList<>();
					complexValueBuffer.put(bufferKey, listOfComplexValues);
					complexTypeProperty = new Property(attribute.getStructuredType().getExternalFQN().getFullQualifiedNameAsString(),
							attribute.getExternalName(), ValueType.COLLECTION_COMPLEX, listOfComplexValues);
					// push exactly one value holder for the whole list... the list will not have
					// more entries
					final ComplexValue compexValue = new ComplexValue();
					listOfComplexValues.add(compexValue);
					values = compexValue.getValue();
				} else {
					// take the existing entry
					values = listOfComplexValues.get(0).getValue();
					// skip 'complexTypeProperty' creation, because already existing
				}
			} else {
				ComplexValue compexValue = (ComplexValue) complexValueBuffer.get(bufferKey);
				if (compexValue == null) {
					compexValue = new ComplexValue();
					complexValueBuffer.put(bufferKey, compexValue);
					complexTypeProperty = new Property(attribute.getStructuredType().getExternalFQN().getFullQualifiedNameAsString(),
							attribute.getExternalName(), ValueType.COMPLEX, compexValue);
				}
				values = compexValue.getValue();
			}
			final int splitIndex = attribute.getExternalName().length() + JPASelector.PATH_SEPERATOR.length();
			final String attributeName = externalName.substring(splitIndex);
			final Property complexTypeNestedProperty = convertJPA2ODataAttribute(value, attributeName, bufferKey,
					attribute.getStructuredType(), complexValueBuffer, values);
			if (complexTypeNestedProperty != null && complexTypeProperty != null) {
				// only create/add a complex type instance if at least one property of complex
				// type is not null
				properties.add(complexTypeProperty);
			}
			return complexTypeProperty;
		} else if (attribute != null && attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
			//leaf element is the property in the @EmbeddedId type
			final JPAAttribute attributeComplexProperty = path.getLeaf();
			return convertJPA2ODataPropertyValue(attributeComplexProperty, attributeComplexProperty.getExternalName(), value, properties);
		} else {
			// ...$select=Name1,Address/Region
			return convertJPA2ODataPropertyValue(attribute, externalName, value, properties);
		}
	}

	private Object convertJPA2ODataPrimitiveValue(final JPAAttribute attribute, final Object jpaValue)
			throws ODataJPAModelException {

		// use a intermediate conversion to an supported JAVA type in the Olingo library
		final EdmPrimitiveTypeKind kind = TypeMapping.convertToEdmSimpleType(attribute);
		final Class<?> oadataType = EdmPrimitiveTypeFactory.getInstance(kind).getDefaultType();

		final Class<?> javaType = attribute.getType();
		if (javaType.equals(oadataType)) {
			return jpaValue;
		}
		final ODataAttributeConverter<Object, Object> converter = determineODataAttributeConverter(attribute,
				oadataType);
		if (converter != null) {
			return converter.convertToOData(jpaValue);
		}
		// use 'as is' without conversion
		return jpaValue;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Property convertJPA2ODataPropertyValue(final JPAAttribute attribute, final String propertyName, final Object input,
			final List<Property> properties) throws ODataJPAModelException, IllegalArgumentException {
		if (attribute == null) {
			throw new IllegalArgumentException("JPA attribute required for property " + propertyName);
		}
		ValueType valueType = null;
		final Class<?> javaType = attribute.getType();
		if (javaType == null) {
			throw new IllegalArgumentException("Java type required for property " + propertyName);
		}
		if (javaType.isEnum() /* && input != null && !Number.class.isInstance(input) */) {
			if (attribute.isCollection()) {
				valueType = ValueType.COLLECTION_ENUM;
			} else {
				valueType = ValueType.ENUM;
			}
		} else if (attribute.isCollection() && attribute.isPrimitive()) {
			valueType = ValueType.COLLECTION_PRIMITIVE;
		} else if (attribute.isPrimitive()) {
			valueType = ValueType.PRIMITIVE;
		} else {
			throw new IllegalArgumentException(
					"Given value is not of primitive type managable in this method: " + propertyName);
		}
		if (valueType == null) {
			throw new IllegalArgumentException(
					"Given value is not of primitive type managable in this method for property " + propertyName);
		}
		Property property;
		Object convertedValue;
		switch (valueType) {
		case ENUM:
			// for OData we have to convert the value into a number (if not yet)
			convertedValue = input != null ? Integer.valueOf(((Enum<?>) input).ordinal()) : null;
			property = new Property(null, propertyName);
			property.setValue(valueType, convertedValue);
			properties.add(property);
			return property;
		case COLLECTION_ENUM:
			property = findOrCreateProperty(properties, propertyName);
			convertedValue = input != null ? Integer.valueOf(((Enum<?>) input).ordinal()) : null;
			if (property.getValueType() == null) {
				// new created property
				final List<Object> list = new LinkedList<>();
				list.add(convertedValue);
				property.setValue(valueType, list);
			} else {
				// add value to existing property
				addSingleValueIfUnique((List) property.getValue(), convertedValue);
			}
			return property;
		case COLLECTION_PRIMITIVE:
			property = findOrCreateProperty(properties, propertyName);
			if (property.getValueType() == null) {
				// new created property
				final List<Object> list = new LinkedList<>();
				list.add(input);
				property.setValue(valueType, list);
			} else {
				// add value to existing property
				if (input instanceof Collection<?>) {
					addCollectionValuesIfUnique((List) property.getValue(), (Collection) input);
				} else {
					addSingleValueIfUnique((List) property.getValue(), input);
				}
			}
			return property;
		default:
			// handle as primitive
			convertedValue = convertJPA2ODataPrimitiveValue(attribute, input);
			property = new Property(null, propertyName);
			property.setValue(valueType, convertedValue);
			properties.add(property);
			return property;
		}
	}

	/**
	 * Add values of input collection into target collection if not yet present.
	 */
	private void addCollectionValuesIfUnique(final Collection<Object> targetList, final Collection<Object> listToAdd) {
		for (final Object v : listToAdd) {
			addSingleValueIfUnique(targetList, v);
		}
	}

	private void addSingleValueIfUnique(final Collection<Object> targetList, final Object valueToAdd) {
		if (targetList.contains(valueToAdd)) {
			return;
		}
		targetList.add(valueToAdd);
	}

	private Property findOrCreateProperty(final List<Property> properties, final String propertyName) {
		for (final Property p : properties) {
			if (p.getName().equals(propertyName)) {
				return p;
			}
		}
		final Property p = new Property(null, propertyName);
		properties.add(p);
		return p;
	}
}