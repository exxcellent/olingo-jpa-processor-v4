package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Link;
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
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryElementCollectionResult;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;

public abstract class JPATupleAbstractConverter extends AbstractConverter {

	public static final String ACCESS_MODIFIER_GET = "get";
	public static final String ACCESS_MODIFIER_SET = "set";
	public static final String ACCESS_MODIFIER_IS = "is";

	private final IntermediateServiceDocument sd;
	private final ServiceMetadata serviceMetadata;
	private final UriHelper uriHelper;

	private final JPAEntityType jpaConversionTargetEntity;

	public JPATupleAbstractConverter(final JPAEntityType jpaTargetEntity,
			final UriHelper uriHelper, final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata) {
		super();

		this.uriHelper = uriHelper;
		this.sd = sd;
		this.serviceMetadata = serviceMetadata;

		this.jpaConversionTargetEntity = jpaTargetEntity;
	}

	protected final UriHelper getUriHelper() {
		return uriHelper;
	}

	protected final ServiceMetadata getServiceMetadata() {
		return serviceMetadata;
	}

	protected final IntermediateServiceDocument getIntermediateServiceDocument() {
		return sd;
	}

	protected final JPAEntityType getJpaEntityType() {
		return jpaConversionTargetEntity;
	}

	/**
	 * The given row is converted into a OData entity and added to the map of
	 * already processed entities.<br/>
	 * If the <code>row</code> is defining a entity that is already part of entity
	 * collection (that happens if the {@link JPAQueryEntityResult} is built from joined
	 * tables) then the entity values are merged into the exiting entity and hat
	 * already processed entity is returned; otherwise a new entity is created.
	 *
	 * @param alreadyProcessedEntities
	 *            The map containing all already converted
	 *            entities. The key in the map is the
	 *            stringified entity id.
	 */
	protected final Entity convertTuple2ODataEntity(final Tuple row, final JPAQueryEntityResult jpaQueryResult)
			throws ODataJPAModelException, ODataJPAConversionException {
		final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
		final Entity odataEntity = new Entity();
		odataEntity.setType(jpaConversionTargetEntity.getExternalFQN().getFullQualifiedNameAsString());
		final List<Property> properties = odataEntity.getProperties();
		// TODO store @Version to fill ETag Header
		for (final TupleElement<?> element : row.getElements()) {
			convertJPAValue2ODataAttribute(row.get(element.getAlias()), element.getAlias(), "",
					jpaConversionTargetEntity, complexValueBuffer, 0, properties);
		}

		createElementCollections(odataEntity, row, jpaQueryResult);

		odataEntity.setId(createId(odataEntity, jpaConversionTargetEntity, uriHelper));
		for (final String attribute : complexValueBuffer.keySet()) {
			if (attribute.indexOf(JPASelector.PATH_SEPERATOR) > -1) {
				// if the attribute name is an path to an nested complex type, then we can
				// continue, because the nested complex type is also a property in one of the
				// higher level attributes (without / in the key name)
				continue;
			}
			final Object entry = complexValueBuffer.get(attribute);
			// FIXME expanding for complex values seems to do wrong things...
			if (entry instanceof ComplexValue) {
				((ComplexValue) entry).getNavigationLinks().addAll(createExpand(row, jpaQueryResult));
			} else if (entry instanceof List) {
				@SuppressWarnings("unchecked")
				final List<ComplexValue> cvList = (List<ComplexValue>) entry;
				for (final ComplexValue cv : cvList) {
					cv.getNavigationLinks().addAll(createExpand(row, jpaQueryResult));
				}
			}
		}
		odataEntity.getNavigationLinks().addAll(createExpand(row, jpaQueryResult));

		return odataEntity;
	}

	protected final URI createId(final Entity odataEntity, final JPAEntityType jpaEntityType, final UriHelper uriHelper)
			throws ODataJPAModelException {

		final EdmEntityType edmType = serviceMetadata.getEdm()
				.getEntityType(jpaEntityType.getExternalFQN());
		try {
			// TODO Clarify host-name and port as part of ID see
			// http://docs.oasis-open.org/odata/odata-atom-format/v4.0/cs02/odata-atom-format-v4.0-cs02.html#_Toc372792702

			final String setName = sd.getEntitySet(jpaEntityType).getExternalName();
			final StringBuffer uriString = new StringBuffer(setName);
			uriString.append("(");
			uriString.append(uriHelper.buildKeyPredicate(edmType, odataEntity));
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
	 * Build a unique entity identifier based on values in <i>row</i> in the columns
	 * defined by <i>joinColumns</i>.
	 */
	protected static final String buildOwningEntityKey(final Tuple row, final Collection<JPASelector> joinColumns)
			throws ODataJPAModelException {
		final StringBuilder buffer = new StringBuilder();
		// we use all columns used in JOIN from left side (the owning entity) to build a
		// identifying key accessing all nested relationship results
		String alias;
		for (final JPASelector item : joinColumns) {
			if (JPAAssociationPath.class.isInstance(item)) {
				final JPAAssociationPath asso = ((JPAAssociationPath) item);
				// select all the key attributes from target (right) side table so we can build
				// a 'result
				// key' from the tuples in the result set
				final List<JPASimpleAttribute> keys = asso.getTargetType().getKeyAttributes(true);
				for (final JPASimpleAttribute jpaAttribute : keys) {
					alias = JPAAbstractEntityQuery.buildTargetJoinAlias(asso, jpaAttribute);
					buffer.append(JPASelector.PATH_SEPERATOR);
					buffer.append(row.get(alias));
				}
			} else {
				// if we got here an exception, then a required (key) join column was not
				// selected in the query (see JPAEntityQuery to fix!)
				buffer.append(JPASelector.PATH_SEPERATOR);
				buffer.append(row.get(item.getAlias()));
			}
		}
		if (buffer.length() < 1) {
			return null;
		}
		buffer.deleteCharAt(0);
		return buffer.toString();
	}

	/**
	 * Create entries for all @ElementCollection attributes and assign they as
	 * {@link Property} to given <i>entity</i>.
	 *
	 * @param entity
	 *            The entity to create element collection properties for
	 * @throws ODataApplicationException
	 */
	private void createElementCollections(final Entity entity, final Tuple owningEntityRow,
			final JPAQueryEntityResult jpaQueryResult) throws ODataJPAModelException, ODataJPAConversionException {
		final Map<JPAAttribute<?>, JPAQueryElementCollectionResult> elementCollections = jpaQueryResult
				.getElementCollections();
		for (final Entry<JPAAttribute<?>, JPAQueryElementCollectionResult> entry : elementCollections.entrySet()) {

			final Set<String> setKeyColumnAliases = entry.getValue().getKeyColumns().stream().map(x -> x.getAlias())
					.collect(Collectors.toSet());
			final String key = buildOwningEntityKey(owningEntityRow, entry.getValue().getKeyColumns());
			final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
			int index = -1;
			final List<Tuple> tuples = entry.getValue().getDirectMappingsResult(key);
			if (tuples == null) {
				continue;
			}
			for (final Tuple row : tuples) {
				index++;
				for (final TupleElement<?> element : row.getElements()) {
					if ((!entry.getKey().isComplex()
							|| entry.getKey().getStructuredType().getPath(element.getAlias()) == null)
							&& setKeyColumnAliases.contains(element.getAlias())) {
						// ignore columns values only part of result set, because we had to select the
						// key columns for joining
						continue;
					}
					/* final Property property = */ convertJPAValue2ODataAttribute(row.get(element.getAlias()),
							element.getAlias(), "", jpaConversionTargetEntity, complexValueBuffer, index,
							entity.getProperties());
				}

			}
		}
	}

	private Collection<? extends Link> createExpand(final Tuple owningEntityRow, final JPAQueryEntityResult jpaQueryResult)
			throws ODataJPAModelException, ODataJPAConversionException {
		final Map<JPAAssociationPath, JPAQueryEntityResult> children = jpaQueryResult.getExpandChildren();
		if (children == null) {
			return Collections.emptyList();
		}
		final List<Link> entityExpandLinks = new ArrayList<Link>(children.size());
		for (final Entry<JPAAssociationPath, JPAQueryEntityResult> entry : children.entrySet()) {
			if (jpaConversionTargetEntity.getDeclaredAssociation(entry.getKey()) == null) {
				continue;
			}
			final JPATupleExpandResultConverter converter = new JPATupleExpandResultConverter(
					(JPAEntityType) entry.getValue().getEntityType(), owningEntityRow, entry.getKey(), uriHelper,
					getIntermediateServiceDocument(), getServiceMetadata());
			final Link expand = converter.convertTuples2ExpandLink(entry.getValue());
			// TODO Check how to convert Organizations('3')/AdministrativeInformation?$expand=Created/User
			entityExpandLinks.add(expand);
		}
		return entityExpandLinks;
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