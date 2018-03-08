package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class IntermediateStructuredType extends IntermediateModelElement implements JPAStructuredType {
	//
	protected final Map<String, IntermediateProperty> declaredPropertiesList;
	protected final Map<String, IntermediateNavigationProperty> declaredNaviPropertiesList;
	protected final Map<String, JPAPathImpl> simpleAttributePathMap;
	protected final Map<String, JPAPathImpl> complexAttributePathMap;
	protected final Map<String, JPAAssociationPathImpl> associationPathMap;
	protected final ManagedType<?> jpaManagedType;
	protected final ServiceDocument serviceDocument;

	IntermediateStructuredType(final JPAEdmNameBuilder nameBuilder, final ManagedType<?> jpaManagedType,
			final ServiceDocument serviceDocument) throws ODataJPAModelException {

		super(nameBuilder, JPANameBuilder.buildStructuredTypeName(jpaManagedType.getJavaType()));
		this.declaredPropertiesList = new HashMap<String, IntermediateProperty>();
		this.simpleAttributePathMap = new HashMap<String, JPAPathImpl>();
		this.complexAttributePathMap = new HashMap<String, JPAPathImpl>();
		this.declaredNaviPropertiesList = new HashMap<String, IntermediateNavigationProperty>();
		this.associationPathMap = new HashMap<String, JPAAssociationPathImpl>();
		this.jpaManagedType = jpaManagedType;
		this.serviceDocument = serviceDocument;

	}

	ManagedType<?> getJpaManagedType() {
		return jpaManagedType;
	}

	@Override
	public JPAAssociationAttribute getAssociation(final String internalName) throws ODataJPAModelException {
		for (final JPAAttribute attribute : this.getAssociations()) {
			if (attribute.getInternalName().equals(internalName)) {
				return (JPAAssociationAttribute) attribute;
			}
		}
		return null;
	}

	@Override
	public JPAAssociationPath getAssociationPath(final String externalName) throws ODataJPAModelException {
		lazyBuildCompleteAssociationPathMap();
		return associationPathMap.get(externalName);
	}

	@Override
	public List<JPAAssociationPath> getAssociationPathList() throws ODataJPAModelException {
		lazyBuildCompleteAssociationPathMap();
		final List<JPAAssociationPath> associationList = new ArrayList<JPAAssociationPath>();

		for (final String externalName : associationPathMap.keySet()) {
			associationList.add(associationPathMap.get(externalName));
		}
		return associationList;
	}

	@Override
	public JPASimpleAttribute getAttribute(final String internalName) throws ODataJPAModelException {
		lazyBuildEdmItem();
		JPASimpleAttribute result = declaredPropertiesList.get(internalName);
		if (result == null && getBaseType() != null) {
			result = getBaseType().getAttribute(internalName);
		} else if (result != null && ((IntermediateProperty) result).ignore()) {
			return null;
		}
		return result;
	}

	@Override
	public List<JPASimpleAttribute> getAttributes() throws ODataJPAModelException {
		lazyBuildEdmItem();
		final List<JPASimpleAttribute> result = new ArrayList<JPASimpleAttribute>();
		for (final String propertyKey : declaredPropertiesList.keySet()) {
			final IntermediateProperty attribute = declaredPropertiesList.get(propertyKey);
			if (!attribute.ignore()) {
				result.add(attribute);
			}
		}
		if (getBaseType() != null) {
			result.addAll(getBaseType().getAttributes());
		}
		return result;
	}

	/**
	 * Returns a resolved list of all attributes that are marked as Id, so the
	 * attributes of an EmbeddedId are returned as separate entries.
	 *
	 * @return The list with attributes or empty list.
	 *
	 * @throws ODataJPAModelException
	 */
	public List<JPASimpleAttribute> getKeyAttributes() throws ODataJPAModelException {
		final List<JPASimpleAttribute> keyList = new LinkedList<JPASimpleAttribute>();
		for (final JPASimpleAttribute attribute : getAttributes()) {
			if (!attribute.isKey()) {
				continue;
			}
			if (attribute.isComplex()) {
				keyList.addAll(attribute.getStructuredType().getAttributes());
			} else {
				keyList.add(attribute);
			}
		}
		return keyList;
	}

	@Override
	public JPAAssociationPath getDeclaredAssociation(final String externalName) throws ODataJPAModelException {
		lazyBuildCompleteAssociationPathMap();
		for (final String internalName : declaredNaviPropertiesList.keySet()) {
			if (externalName.equals(declaredNaviPropertiesList.get(internalName).getExternalName())) {
				return associationPathMap.get(externalName);
			}
		}
		final IntermediateStructuredType baseType = getBaseType();
		if (baseType != null) {
			return baseType.getDeclaredAssociation(externalName);
		}
		return null;
	}

	@Override
	public JPAAssociationPath getDeclaredAssociation(final JPAAssociationPath associationPath)
			throws ODataJPAModelException {
		lazyBuildCompleteAssociationPathMap();

		if (associationPathMap.containsKey(associationPath.getAlias())) {
			return associationPathMap.get(associationPath.getAlias());
		}
		final IntermediateStructuredType baseType = getBaseType();
		if (baseType != null) {
			return baseType.getDeclaredAssociation(associationPath);
		}
		return null;
	}

	@Override
	public JPAAttributePath getPath(final String externalName) throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		JPAAttributePath targetPath = simpleAttributePathMap.get(externalName);
		if (targetPath == null) {
			targetPath = complexAttributePathMap.get(externalName);
		}
		if (targetPath == null || targetPath.ignore()) {
			return null;
		}
		return targetPath;
	}

	@Override
	public List<JPASelector> getPathList() throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		final List<JPASelector> pathList = new ArrayList<JPASelector>();
		for (final Entry<String, JPAPathImpl> entry : simpleAttributePathMap.entrySet()) {
				pathList.add(entry.getValue());
		}
		return pathList;
	}

	@Override
	public Class<?> getTypeClass() {
		return this.jpaManagedType.getJavaType();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<Attribute<?, ?>> determineJPAAttributes() {
		if (isMappedSuperclass(jpaManagedType.getJavaType().getSuperclass())) {
			// include attributes from mapped superclass -> required for Hibernate,
			// EclipseLink doesn't need it
			return (Collection) jpaManagedType.getAttributes();
		}
		// attributes without superclass
		return (Collection) jpaManagedType.getDeclaredAttributes();
	}

	protected void buildPropertyList() throws ODataJPAModelException {
		IntermediateProperty property;
		IntermediateNavigationProperty navProp;
		for (final Attribute<?, ?> jpaAttribute : determineJPAAttributes()) {
			final PersistentAttributeType attributeType = jpaAttribute.getPersistentAttributeType();
			switch (attributeType) {
			case BASIC:
			case EMBEDDED:
				if (jpaAttribute instanceof SingularAttribute<?, ?>
				&& ((SingularAttribute<?, ?>) jpaAttribute).isId()
				&& attributeType == PersistentAttributeType.EMBEDDED) {
					property = new IntermediateEmbeddedIdProperty(nameBuilder, jpaAttribute, serviceDocument);
					declaredPropertiesList.put(property.internalName, property);
				} else {
					property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
					declaredPropertiesList.put(property.internalName, property);
				}
				break;
			case ONE_TO_MANY:
			case ONE_TO_ONE:
			case MANY_TO_MANY:
			case MANY_TO_ONE:
				navProp = new IntermediateNavigationProperty(nameBuilder, this, jpaAttribute, serviceDocument);
				declaredNaviPropertiesList.put(navProp.internalName, navProp);
				break;
			case ELEMENT_COLLECTION:
				if (JPATypeConvertor.isCollectionTypeOfPrimitive(jpaAttribute)) {
					property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
					declaredPropertiesList.put(property.internalName, property);
					break;
				} else if (JPATypeConvertor.isCollectionTypeOfEmbeddable(jpaAttribute)) {
					property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
					declaredPropertiesList.put(property.internalName, property);
					break;
				}
				// fall through
			default:
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
						attributeType.name(), jpaAttribute.getDeclaringType().getJavaType().getName());
			}
		}

	}

	protected FullQualifiedName determineBaseType() throws ODataJPAModelException {

		final IntermediateStructuredType baseEntity = getBaseType();
		if (baseEntity != null && !baseEntity.isAbstract() && isAbstract()) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INHERITANCE_NOT_ALLOWED,
					this.internalName, baseEntity.internalName);
		}
		return baseEntity != null ? nameBuilder.buildFQN(baseEntity.getExternalName()) : null;
	}

	protected boolean isAbstract() {
		return false;
	}

	protected IntermediateStructuredType getBaseType() throws ODataJPAModelException {
		final Class<?> baseType = jpaManagedType.getJavaType().getSuperclass();
		if (baseType == null) {
			return null;
		}
		if (isMappedSuperclass(baseType)) {
			return null;
		}
		return serviceDocument.getEntityType(baseType);
	}

	private static boolean isMappedSuperclass(final Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		return clazz.getAnnotation(MappedSuperclass.class) != null;
	}

	private List<JPAAssociationAttribute> getAssociations() throws ODataJPAModelException {
		lazyBuildEdmItem();
		final List<JPAAssociationAttribute> jpaAttributes = new LinkedList<JPAAssociationAttribute>();
		for (final String internalName : declaredNaviPropertiesList.keySet()) {
			final IntermediateNavigationProperty property = declaredNaviPropertiesList.get(internalName);
			if (!property.ignore()) {
				jpaAttributes.add(property);
			}
		}
		final IntermediateStructuredType baseType = getBaseType();
		if (baseType != null) {
			jpaAttributes.addAll(baseType.getAssociations());
		}
		return jpaAttributes;
	}

	/**
	 * This does not resolve associations!
	 *
	 * @param dbFieldName
	 * @return
	 * @throws ODataJPAModelException
	 */
	JPAAttributePath getPathByDBField(final String dbFieldName) throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		for (final JPAPathImpl path : simpleAttributePathMap.values()) {
			if (path.getDBFieldName().equals(dbFieldName)) {
				return path;
			}
		}
		return null;
	}

	IntermediateProperty getPropertyByDBField(final String dbFieldName) throws ODataJPAModelException {
		lazyBuildEdmItem();
		for (final String internalName : declaredPropertiesList.keySet()) {
			final IntermediateProperty property = declaredPropertiesList.get(internalName);
			if (property.getDBFieldName().equals(dbFieldName)) {
				return property;
			}
		}
		if (getBaseType() != null) {
			return getBaseType().getPropertyByDBField(dbFieldName);
		}
		return null;
	}

	/**
	 * Returns an property regardless if it should be ignored or not
	 * @param internalName
	 * @return
	 * @throws ODataJPAModelException
	 */
	IntermediateProperty getProperty(final String internalName) throws ODataJPAModelException {
		lazyBuildEdmItem();
		IntermediateProperty result = declaredPropertiesList.get(internalName);
		if (result == null && getBaseType() != null) {
			result = getBaseType().getProperty(internalName);
		}
		return result;
	}

	IntermediateNavigationProperty getCorrespondingAssiciation(final IntermediateStructuredType sourceType,
			final String sourceRelationshipName) {
		final Attribute<?, ?> jpaAttribute = findCorrespondingAssociation(sourceType, sourceRelationshipName);
		return jpaAttribute == null ? null : new IntermediateNavigationProperty(nameBuilder, sourceType, jpaAttribute,
				serviceDocument);
	}

	private Attribute<?, ?> findCorrespondingAssociation(final IntermediateStructuredType sourceType,
			final String sourceRelationshipName) {
		Class<?> targetClass = null;

		for (final Attribute<?, ?> jpaAttribute : determineJPAAttributes()) {
			if (jpaAttribute.getPersistentAttributeType() != null
					&& jpaAttribute.getJavaMember() instanceof AnnotatedElement
					&& !sourceRelationshipName.equals(JPANameBuilder.buildAssociationName(jpaAttribute))) {
				if (jpaAttribute.isCollection()) {
					targetClass = ((PluralAttribute<?, ?, ?>) jpaAttribute).getElementType().getJavaType();
				} else {
					targetClass = jpaAttribute.getJavaType();
				}
				if (targetClass.equals(sourceType.getTypeClass())) {
					final OneToMany cardinalityOtM = ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(
							OneToMany.class);
					if (cardinalityOtM != null && cardinalityOtM.mappedBy() != null
							&& cardinalityOtM.mappedBy().equals(sourceRelationshipName)) {
						return jpaAttribute;
					}
				}
			}
		}

		return null;
	}

	@Override
	abstract CsdlStructuralType getEdmItem() throws ODataJPAModelException;

	List<IntermediateJoinColumn> getJoinColumns(final IntermediateStructuredType sourceType,
			final String relationshipName) {

		final Attribute<?, ?> jpaAttribute = jpaManagedType.getAttribute(relationshipName);
		if (jpaAttribute == null) {
			return Collections.emptyList();
		}

		final List<IntermediateJoinColumn> result = new ArrayList<IntermediateJoinColumn>();
		final AnnotatedElement annotatedElement = (AnnotatedElement) jpaAttribute.getJavaMember();
		final JoinColumns columns = annotatedElement.getAnnotation(JoinColumns.class);
		if (columns != null) {
			for (final JoinColumn column : columns.value()) {
				result.add(new IntermediateJoinColumn(column));
			}
		} else {
			final JoinColumn column = annotatedElement.getAnnotation(JoinColumn.class);
			if (column != null) {
				result.add(new IntermediateJoinColumn(column));
			}
		}
		return result;
	}

	Map<String, JPAPathImpl> getSimpleAttributePathMap() throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		return simpleAttributePathMap;
	}

	Map<String, JPAPathImpl> getComplexAttributePathMap() throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		return complexAttributePathMap;
	}

	Map<String, JPAAssociationPathImpl> getAssociationPathMap() throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		return associationPathMap;
	}

	private String determineDBFieldName(final IntermediateProperty property, final JPAAttributePath jpaPath) {
		final Attribute<?, ?> jpaAttribute = jpaManagedType.getAttribute(property.getInternalName());
		if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
			final AnnotatedElement a = (AnnotatedElement) jpaAttribute.getJavaMember();
			final AttributeOverrides overwriteList = a.getAnnotation(AttributeOverrides.class);
			if (overwriteList != null) {
				for (final AttributeOverride overwrite : overwriteList.value()) {
					if (overwrite.name().equals(jpaPath.getLeaf().getInternalName())) {
						return overwrite.column().name();
					}
				}
			} else {
				final AttributeOverride overwrite = a.getAnnotation(AttributeOverride.class);
				if (overwrite != null) {
					if (overwrite.name().equals(jpaPath.getLeaf().getInternalName())) {
						return overwrite.column().name();
					}
				}
			}
		}
		return jpaPath.getDBFieldName();
	}

	private List<IntermediateJoinColumn> determineJoinColumns(final IntermediateProperty property,
			final JPAAssociationPath association) {
		final List<IntermediateJoinColumn> result = new ArrayList<IntermediateJoinColumn>();

		final Attribute<?, ?> jpaAttribute = jpaManagedType.getAttribute(property.getInternalName());
		if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
			final AnnotatedElement a = (AnnotatedElement) jpaAttribute.getJavaMember();
			if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
				final AssociationOverrides overwriteList = a.getAnnotation(AssociationOverrides.class);
				if (overwriteList != null) {
					for (final AssociationOverride overwrite : overwriteList.value()) {
						if (overwrite.name().equals(association.getLeaf().getInternalName())) {
							for (final JoinColumn column : overwrite.joinColumns()) {
								result.add(new IntermediateJoinColumn(column));
							}
						}
					}
				} else {
					final AssociationOverride overwrite = a.getAnnotation(AssociationOverride.class);
					if (overwrite != null) {
						if (overwrite.name().equals(association.getLeaf().getInternalName())) {
							for (final JoinColumn column : overwrite.joinColumns()) {
								result.add(new IntermediateJoinColumn(column));
							}
						}
					}
				}
			}
		}
		return result;
	}

	private void lazyBuildCompleteAssociationPathMap() throws ODataJPAModelException {
		JPAAssociationPathImpl associationPath;
		lazyBuildCompletePathMap();
		// TODO check if ignore has to be handled
		if (associationPathMap.size() == 0) {
			for (final JPAAssociationAttribute association : getAssociations()) {
				associationPath = new JPAAssociationPathImpl((IntermediateNavigationProperty) association, this);
				associationPathMap.put(associationPath.getAlias(), associationPath);
			}

			for (final String key : this.complexAttributePathMap.keySet()) {
				final JPAAttributePath attributePath = this.complexAttributePathMap.get(key);
				if (attributePath.getPathElements().size() == 1) {
					// Only direct attributes
					final IntermediateProperty property = (IntermediateProperty) attributePath.getLeaf();
					final IntermediateStructuredType is = (IntermediateStructuredType) property.getStructuredType();

					for (final JPAAssociationPath association : is.getAssociationPathList()) {
						associationPath = new JPAAssociationPathImpl(nameBuilder, association,
								this, determineJoinColumns(property, association), property);
						associationPathMap.put(associationPath.getAlias(), associationPath);
					}
				}
			}
		}
	}

	private void lazyBuildCompletePathMap() throws ODataJPAModelException {
		ArrayList<JPAAttribute> pathList;

		lazyBuildEdmItem();
		if (simpleAttributePathMap.size() == 0) {
			String externalName;
			for (final String internalName : declaredPropertiesList.keySet()) {
				final IntermediateProperty property = declaredPropertiesList.get(internalName);
				if (property.isComplex()) {
					complexAttributePathMap.put(property.getExternalName(),
							new JPAPathImpl(property.getExternalName(), null, property));
					final Map<String, JPAPathImpl> nestedComplexAttributePathMap = ((IntermediateStructuredType) property
							.getStructuredType()).getComplexAttributePathMap();
					for (final Entry<String, JPAPathImpl> entry : nestedComplexAttributePathMap.entrySet()) {
						externalName = entry.getKey();
						pathList = new ArrayList<JPAAttribute>(entry.getValue().getPathElements());
						pathList.add(0, property);
						complexAttributePathMap.put(nameBuilder.buildPath(property.getExternalName(), externalName),
								new JPAPathImpl(nameBuilder.buildPath(property.getExternalName(),
										externalName), null, pathList));
					}

					// add the (simple) properties of complex type as path to this type
					final Map<String, JPAPathImpl> nestedSimpleAttributePathMap = ((IntermediateStructuredType) property
							.getStructuredType()).getSimpleAttributePathMap();
					JPAPathImpl newPath;
					for (final Entry<String, JPAPathImpl> entry : nestedSimpleAttributePathMap.entrySet()) {
						externalName = entry.getKey();
						pathList = new ArrayList<JPAAttribute>(entry.getValue().getPathElements());
						pathList.add(0, property);
						if (property.isKey()) {
							newPath = new JPAPathImpl(externalName, determineDBFieldName(property, entry.getValue()),
									pathList);
						} else {
							newPath = new JPAPathImpl(nameBuilder.buildPath(property.getExternalName(), externalName),
									determineDBFieldName(property, entry.getValue()), pathList);
						}
						simpleAttributePathMap.put(newPath.getAlias(), newPath);
					}
				} else {
					simpleAttributePathMap.put(property.getExternalName(), new JPAPathImpl(property.getExternalName(), property
							.getDBFieldName(), property));
				}
			}
		}
		final IntermediateStructuredType baseType = getBaseType();
		if (baseType != null) {
			simpleAttributePathMap.putAll(baseType.getSimpleAttributePathMap());
			complexAttributePathMap.putAll(baseType.getComplexAttributePathMap());
		}

	}

	protected boolean determineHasStream() throws ODataJPAModelException {
		return getStreamProperty() == null ? false : true;
	}

	protected IntermediateProperty getStreamProperty() throws ODataJPAModelException {
		int count = 0;
		IntermediateProperty result = null;
		for (final String internalName : declaredPropertiesList.keySet()) {
			if (declaredPropertiesList.get(internalName).isStream()) {
				count += 1;
				result = declaredPropertiesList.get(internalName);
			}
		}
		if (this.getBaseType() != null) {
			final IntermediateProperty superResult = getBaseType().getStreamProperty();
			if (superResult != null) {
				count += 1;
				result = superResult;
			}
		}
		if (count > 1) {
			// Only one stream property per entity is allowed. For %1$s %2$s have been found
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TO_MANY_STREAMS, internalName, Integer
					.toString(count));
		}
		return result;
	}
}