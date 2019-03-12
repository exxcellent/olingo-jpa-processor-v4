package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.AssociationOverride;
import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlOnDelete;
import org.apache.olingo.commons.api.edm.provider.CsdlOnDeleteAction;
import org.apache.olingo.commons.api.edm.provider.CsdlReferentialConstraint;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;

/**
 * A navigation property describes a relation of one entity type to another entity type and allows to navigate to it.
 * IntermediateNavigationProperty represents a navigation within on service, that is source and target are described by
 * the same service document.
 * <a href=
 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397962"
 * >OData Version 4.0 Part 3 - 7 Navigation Property</a>
 *
 * @author Oliver Grande
 *
 */
class IntermediateNavigationProperty extends IntermediateModelElement
implements IntermediateNavigationPropertyAccess, JPAAssociationAttribute {

	private static class JoinConfiguration {
		private final List<IntermediateJoinColumn> sourceJoinColumns = new LinkedList<IntermediateJoinColumn>();
		private List<IntermediateJoinColumn> targetJoinColumns = null;
		private boolean useJoinTable = false;
	}

	private final static Logger LOG = Logger.getLogger(IntermediateNavigationProperty.class.getName());

	private final Attribute<?, ?> jpaAttribute;
	private CsdlNavigationProperty edmNaviProperty;
	private CsdlOnDelete edmOnDelete;
	private final IntermediateStructuredType<?> sourceType;
	private IntermediateStructuredType<?> targetType;
	private final IntermediateServiceDocument serviceDocument;
	private final JPAAttributeAccessor accessor;
	private JoinConfiguration joinConfiguration = null;
	private InitializationState initStateEdm = InitializationState.NotInitialized;

	IntermediateNavigationProperty(final JPAEdmNameBuilder nameBuilder, final IntermediateStructuredType<?> parent,
			final Attribute<?, ?> jpaAttribute,
			final IntermediateServiceDocument serviceDocument) {
		super(nameBuilder, jpaAttribute.getName());
		this.jpaAttribute = jpaAttribute;
		this.serviceDocument = serviceDocument;
		this.sourceType = parent;

		this.setExternalName(nameBuilder.buildNaviPropertyName(jpaAttribute));

		final AccessibleObject attribute = (AccessibleObject) jpaAttribute.getJavaMember();
		if (Field.class.isInstance(attribute)) {
			accessor = new FieldAttributeAccessor((Field) attribute);
		} else if (Method.class.isInstance(attribute)) {
			throw new UnsupportedOperationException(
					"The attribute access to " + parent.getInternalName() + "#" + jpaAttribute.getName()
					+ " is covered by an method; this happens for example in a scenarion with EclipseLink weaving... Sorry that is not supported, you have to disable weaving!");
		} else {
			throw new UnsupportedOperationException("Unsupported property type: "+attribute);
		}

		// do not wait with setting this important property
		setIgnore(attribute.isAnnotationPresent(EdmIgnore.class));
	}

	@Override
	public JPAAttributeAccessor getAttributeAccessor() {
		return accessor;
	}

	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
		if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
			return ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(annotationClass);
		}
		return null;
	}

	@Override
	public CsdlNavigationProperty getProperty() throws ODataJPAModelException {
		return getEdmItem();
	}

	@Override
	public JPAStructuredType getStructuredType() {
		try {
			return getTargetEntity();
		} catch (final ODataJPAModelException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JPAStructuredType getTargetEntity() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return targetType;
	}

	public Class<?> getType() {
		return jpaAttribute.getJavaType();
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public boolean isCollection() {
		return jpaAttribute.isCollection();
	}

	@Override
	public boolean isComplex() {
		// navigation properties are targeting always a non primitive object
		return true;
	}

	@Override
	public AttributeMapping getAttributeMapping() {
		return AttributeMapping.RELATIONSHIP;
	}

	public boolean isPrimitive() {
		// navigation properties are targeting always a non primitive object
		return false;
	}

	@Override
	public boolean isKey() {
		if (jpaAttribute instanceof SingularAttribute<?, ?>) {
			return ((SingularAttribute<?, ?>) jpaAttribute).isId();
		}
		return false;
	}

	@Override
	public boolean isSearchable() {
		return false;
	}

	@Override
	public void setOnDelete(final CsdlOnDelete onDelete) {
		edmOnDelete = onDelete;
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		switch (initStateEdm) {
		case Initialized:
			return;
		case InProgress:
			throw new IllegalStateException("Initialization already in progress, circular dependency problem!");
		default:
			break;
		}

		if (edmNaviProperty == null) {
			try {
				initStateEdm = InitializationState.InProgress;

				targetType = serviceDocument.getStructuredType(jpaAttribute);
				if (targetType == null) {
					LOG.log(Level.SEVERE, "Target of navigation property (" + sourceType.getInternalName() + "#"
							+ getInternalName() + ") couldn't be found, navigation to target entity is not possible!!");
					setIgnore(true);
				}

				String mappedBy = null;
				edmNaviProperty = new CsdlNavigationProperty();
				edmNaviProperty.setName(getExternalName());
				if (targetType != null) {
					edmNaviProperty.setType(nameBuilder.buildFQN(targetType.getExternalName()));
				}
				edmNaviProperty.setCollection(jpaAttribute.isCollection());

				final AnnotatedElement annotatedElement = (AnnotatedElement) jpaAttribute.getJavaMember();

				switch (jpaAttribute.getPersistentAttributeType()) {
				case ONE_TO_MANY:
					final OneToMany o2m = annotatedElement.getAnnotation(OneToMany.class);
					if (o2m != null) {
						mappedBy = o2m.mappedBy();
						edmNaviProperty.setOnDelete(edmOnDelete != null ? edmOnDelete : setJPAOnDelete(o2m.cascade()));
					}
					break;
				case ONE_TO_ONE:
					final OneToOne oto = annotatedElement.getAnnotation(OneToOne.class);
					edmNaviProperty.setNullable(Boolean.valueOf(oto.optional()));
					mappedBy = oto.mappedBy();
					edmNaviProperty.setOnDelete(edmOnDelete != null ? edmOnDelete : setJPAOnDelete(oto.cascade()));
					break;
				case MANY_TO_ONE:
					final ManyToOne mto = annotatedElement.getAnnotation(ManyToOne.class);
					edmNaviProperty.setNullable(Boolean.valueOf(mto.optional()));
					edmNaviProperty.setOnDelete(edmOnDelete != null ? edmOnDelete : setJPAOnDelete(mto.cascade()));
					break;
				case MANY_TO_MANY:
					final ManyToMany m2m = annotatedElement.getAnnotation(ManyToMany.class);
					edmNaviProperty.setOnDelete(edmOnDelete != null ? edmOnDelete : setJPAOnDelete(m2m.cascade()));
					mappedBy = m2m.mappedBy();
					break;
				default:
					break;
				}

				this.joinConfiguration = buildJoinConfiguration(jpaAttribute, mappedBy, sourceType, targetType);

				// Determine referential constraint
				assignReferentialConstraints();

				// TODO determine ContainsTarget

				if (sourceType instanceof IntermediateEntityType && targetType != null) {
					// Partner Attribute must not be defined at Complex Types.
					// JPA bi-directional associations are defined at both sides, e.g.
					// at the BusinessPartner and at the Roles. JPA only defines the
					// "mappedBy" at the Parent.
					String partnerName = null;
					if (mappedBy != null && !mappedBy.isEmpty()) {
						final JPAAssociationAttribute association = targetType.getAssociation(mappedBy);
						if (association != null) {
							partnerName = association.getExternalName();
						}
					} else {
						// no 'mappedBy'... try alternative ways
						partnerName = targetType.determineCorrespondingMappedByImplementingAssociationName(sourceType,
								getInternalName());
					}
					if (partnerName != null) {
						edmNaviProperty.setPartner(partnerName);
					} else {
						final String relationshipLabel = sourceType.getInternalName().concat("#")
								.concat(getInternalName());
						LOG.log(Level.FINER,
								"Couldn't determine association partner for " + relationshipLabel + " -> "
										+ targetType.getExternalName() + " (mapped by: "
										+ mappedBy + ")");
					}
				}

				if (joinConfiguration.sourceJoinColumns.isEmpty()) {
					final String relationshipLabel = sourceType.getInternalName().concat("#").concat(getInternalName());
					LOG.log(Level.SEVERE, "Navigation property (" + relationshipLabel
							+ ") without columns to join found, navigation to target entity is not possible!");
					setIgnore(true);
				}

			} finally {
				initStateEdm = InitializationState.Initialized;
			}
		}

	}

	private static JoinConfiguration buildJoinConfiguration(final Attribute<?, ?> sourceJpaAttribute,
			final String mappedBy, final IntermediateStructuredType<?> sourceType,
			final IntermediateStructuredType<?> targetType)
					throws ODataJPAModelException {
		final String relationshipLabel = sourceType.getInternalName().concat("#").concat(sourceJpaAttribute.getName());
		final AnnotatedElement annotatedElement = (AnnotatedElement) sourceJpaAttribute.getJavaMember();
		final JoinConfiguration joinConfiguration = new JoinConfiguration();
		final JoinTable annoJoinTable = annotatedElement.getAnnotation(JoinTable.class);
		final JoinColumns annoJoinColumns = annotatedElement.getAnnotation(JoinColumns.class);
		final JoinColumn annoJoinColumn = annotatedElement.getAnnotation(JoinColumn.class);
		if (annoJoinTable != null) {
			joinConfiguration.useJoinTable = true;
			handleSourceJoinColumnAnnotations(joinConfiguration, annoJoinTable.joinColumns(),
					relationshipLabel);
			handleTargetJoinColumnAnnotations(joinConfiguration, annoJoinTable.inverseJoinColumns(), relationshipLabel);
		} else if (annoJoinColumns != null) {
			handleSourceJoinColumnAnnotations(joinConfiguration, annoJoinColumns.value(),
					relationshipLabel);
		} else if (annoJoinColumn != null) {
			handleSourceJoinColumnAnnotations(joinConfiguration, new JoinColumn[] { annoJoinColumn },
					relationshipLabel);
		} else if (mappedBy != null && !mappedBy.isEmpty() && targetType != null) {
			// find the join columns on opposite side and fill up with informations on our
			// (source) side
			final Attribute<?, ?> targetJpaAttribute = targetType.findJPARelationshipAttribute(mappedBy);
			if (targetJpaAttribute != null) {
				handleJoinColumnsMappedByOfTarget(joinConfiguration, targetJpaAttribute, targetType, sourceType);
			}
		} else if (targetType != null) {
			// define joins by 'id' column(s)
			final Collection<IntermediateJoinColumn> intermediateColumns = buildDefaultKeyBasedJoinColumns(
					sourceJpaAttribute.getName(),
					targetType);
			joinConfiguration.sourceJoinColumns.addAll(intermediateColumns);
		}
		return joinConfiguration;
	}

	private static void handleJoinColumnsMappedByOfTarget(final JoinConfiguration joinConfiguration,
			final Attribute<?, ?> oppositeJpaAttribute, final IntermediateStructuredType<?> oppositeType,
			final IntermediateStructuredType<?> originalRelationshipStartingType) throws ODataJPAModelException {

		// take all informations from the other end of relationship (switch source and
		// target)
		final JoinConfiguration oppositeConfiguration = buildJoinConfiguration(oppositeJpaAttribute, null, oppositeType,
				originalRelationshipStartingType);
		joinConfiguration.useJoinTable = oppositeConfiguration.useJoinTable;//then we have also the join table
		String columnName;
		String refernceColumnName;
		IntermediateJoinColumn intermediateJoinColumn;
		if (joinConfiguration.useJoinTable) {
			// invert also source and target join columns, because we are on the other side
			// of join table...
			if (oppositeConfiguration.targetJoinColumns != null) {
				// exchange side
				for (final IntermediateJoinColumn ojc : oppositeConfiguration.targetJoinColumns) {
					refernceColumnName = ojc.getTargetColumnName();
					columnName = ojc.getSourceEntityColumnName();
					intermediateJoinColumn = new IntermediateJoinColumn(columnName, refernceColumnName);
					joinConfiguration.sourceJoinColumns.add(intermediateJoinColumn);

				}
			}
			joinConfiguration.targetJoinColumns = new LinkedList<IntermediateJoinColumn>();
			// exchange side
			for (final IntermediateJoinColumn ojc : oppositeConfiguration.sourceJoinColumns) {
				refernceColumnName = ojc.getTargetColumnName();
				columnName = ojc.getSourceEntityColumnName();
				intermediateJoinColumn = new IntermediateJoinColumn(columnName, refernceColumnName);
				joinConfiguration.targetJoinColumns.add(intermediateJoinColumn);
			}
		} else {
			for (final IntermediateJoinColumn ojc : oppositeConfiguration.sourceJoinColumns) {
				// exchange column names
				columnName = ojc.getTargetColumnName();
				refernceColumnName = ojc.getSourceEntityColumnName();
				intermediateJoinColumn = new IntermediateJoinColumn(columnName, refernceColumnName);
				joinConfiguration.sourceJoinColumns.add(intermediateJoinColumn);
			}
		}
	}

	private static void handleSourceJoinColumnAnnotations(final JoinConfiguration joinConfiguration,
			final JoinColumn[] annoJoinColumns, final String relationshipLabel)
					throws ODataJPAModelException {
		int implicitColumns = 0;
		for (final JoinColumn column : annoJoinColumns) {
			final IntermediateJoinColumn intermediateColumn;
			if (!joinConfiguration.useJoinTable) {
				intermediateColumn = new IntermediateJoinColumn(column);
			} else {
				intermediateColumn = new IntermediateJoinColumn(column.referencedColumnName(), column.name());
			}
			final String refColumnName = intermediateColumn.getTargetColumnName();
			final String name = intermediateColumn.getSourceEntityColumnName();
			if (refColumnName == null || refColumnName.isEmpty() || name == null || name.isEmpty()) {
				implicitColumns += 1;
				if (implicitColumns > 1) {
					throw new ODataJPAModelException(
							ODataJPAModelException.MessageKeys.NOT_SUPPORTED_NO_IMPLICIT_COLUMNS, relationshipLabel);
				}
			}
			joinConfiguration.sourceJoinColumns.add(intermediateColumn);
		}
	}

	private static void handleTargetJoinColumnAnnotations(final JoinConfiguration joinConfiguration,
			final JoinColumn[] annoJoinColumns, final String internalName) throws ODataJPAModelException {
		joinConfiguration.targetJoinColumns = new ArrayList<>(annoJoinColumns.length);
		int implicitColumns = 0;
		for (final JoinColumn column : annoJoinColumns) {
			final IntermediateJoinColumn intermediateColumn;
			if (!joinConfiguration.useJoinTable) {
				intermediateColumn = new IntermediateJoinColumn(column);
			} else {
				intermediateColumn = new IntermediateJoinColumn(column.referencedColumnName(), column.name());
			}
			final String refColumnName = intermediateColumn.getTargetColumnName();
			final String name = intermediateColumn.getSourceEntityColumnName();
			if (refColumnName == null || refColumnName.isEmpty() || name == null || name.isEmpty()) {
				implicitColumns += 1;
				if (implicitColumns > 1) {
					throw new ODataJPAModelException(
							ODataJPAModelException.MessageKeys.NOT_SUPPORTED_NO_IMPLICIT_COLUMNS, internalName);
				}
			}
			joinConfiguration.targetJoinColumns.add(intermediateColumn);
		}
	}

	/**
	 *
	 * @return TRUE if a @JoinTable annotation was found
	 */
	public boolean doesUseJoinTable() {
		return joinConfiguration.useJoinTable;
	}

	private void assignReferentialConstraints() throws ODataJPAModelException {
		final AnnotatedElement annotatedElement = (AnnotatedElement) jpaAttribute.getJavaMember();

		final AssociationOverride overwrite = annotatedElement.getAnnotation(AssociationOverride.class);
		if (overwrite != null) {
			return;
		}

		final PersistentAttributeType cardinality = jpaAttribute.getPersistentAttributeType();
		final List<CsdlReferentialConstraint> constraints = edmNaviProperty.getReferentialConstraints();
		for (int i = 0; i < joinConfiguration.sourceJoinColumns.size(); i++) {

			if (!joinConfiguration.useJoinTable) {
				// with an join table the columns are not easy enough to set
				JPAAssociationPathImpl.fillMissingName(cardinality, sourceType, targetType,
						joinConfiguration.sourceJoinColumns.get(i));
			}

			final Pair<String, String> pair = determineRelationshipPropertyEnds(cardinality, joinConfiguration, i);
			if (pair == null) {
				continue;
			}
			final IntermediateModelElement sP = sourceType
					.getPropertyByDBField(pair.left);
			if (sP == null) {
				// may happen if source db field is part of an @EmbeddedId
				// may happen if relationship join column is not mapped as separate property
				final ODataJPAModelException ex = new ODataJPAModelException(
						ODataJPAModelException.MessageKeys.REFERENCED_PROPERTY_NOT_FOUND, getInternalName(), pair.left,
						sourceType.getExternalName());
				LOG.log(Level.FINER, ex.getMessage());
				continue;
			}
			// do not create referential constraints (visible in $metadata) for ignored
			// attributes
			if (sP.ignore()) {
				continue;
			}
			final IntermediateModelElement tP = targetType.getPropertyByDBField(pair.right);
			if (tP == null) {
				// may happen if target db field is part of an @EmbeddedId
				// may happen for inverted join columns from 'mappedBy' having now an not set
				// 'referencedColumnName'
				final ODataJPAModelException ex = new ODataJPAModelException(
						ODataJPAModelException.MessageKeys.REFERENCED_PROPERTY_NOT_FOUND, getInternalName(), pair.right,
						targetType.getExternalName());
				LOG.log(Level.FINER, ex.getMessage());
				continue;
			}
			// do not create referential constraints (visible in $metadata) for ignored
			// attributes
			if (tP.ignore()) {
				continue;
			}
			final CsdlReferentialConstraint constraint = new CsdlReferentialConstraint();
			constraint.setProperty(sP.getExternalName());
			constraint.setReferencedProperty(tP.getExternalName());
			constraints.add(constraint);
		}

	}

	/**
	 * Determine the name of attribute for left and right side of relationship
	 * derived from join column/table information.
	 *
	 * @return The pair of attribute names for left and right side or
	 *         <code>null</code>.
	 */
	private static Pair<String, String> determineRelationshipPropertyEnds(final PersistentAttributeType cardinality,
			final JoinConfiguration joinConfiguration,
			final int joinColumnIndex) {
		if (joinColumnIndex >= joinConfiguration.sourceJoinColumns.size()) {
			return null;
		}

		final String left;
		final String right;

		if (joinConfiguration.useJoinTable) {
			// having a join table means; it's possible that the number of source side
			// attributes may differ from the number of attributes on target side used to
			// join
			if (joinConfiguration.targetJoinColumns == null) {
				return null;
			}
			if (joinConfiguration.targetJoinColumns.size() != joinConfiguration.sourceJoinColumns.size()) {
				return null;
			}
			// left side is always coming from source join columns
			left = joinConfiguration.sourceJoinColumns.get(joinColumnIndex).getSourceEntityColumnName();
			right = joinConfiguration.targetJoinColumns.get(joinColumnIndex).getSourceEntityColumnName();
		} else {
			left = joinConfiguration.sourceJoinColumns.get(joinColumnIndex).getSourceEntityColumnName();
			right = joinConfiguration.sourceJoinColumns.get(joinColumnIndex).getTargetColumnName();
		}
		return new Pair<String, String>(left, right);
	}

	@SuppressWarnings("unchecked")
	@Override
	CsdlNavigationProperty getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmNaviProperty;
	}

	PersistentAttributeType getJoinCardinality() throws ODataJPAModelException {
		return jpaAttribute.getPersistentAttributeType();
	}

	/**
	 *
	 * @return The list of columns (aka attributes) of the source entity to select
	 *         for this relationship in a JOIN
	 */
	List<IntermediateJoinColumn> getSourceJoinColumns() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return joinConfiguration.sourceJoinColumns;
	}

	/**
	 *
	 * @return The list of columns (aka attributes) of the target entity to select
	 *         for this relationship in a JOIN in 1:n or m:n relationship with
	 *         {@link #doesUseJoinTable() join table}. The result will
	 *         <code>null</code> if no join table is used.
	 */
	List<IntermediateJoinColumn> getTargetJoinColumns() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return joinConfiguration.targetJoinColumns;
	}

	/**
	 * Calculate the join column names based on default JPA naming strategy:
	 * <ul>
	 * <li>the foreign key is located in the source table</li>
	 * <li>the foreign key has name with pattern: <b>&lt;relationship attribute
	 * name&gt;_&lt;target table primary key name&gt</b></li>
	 * </ul>
	 * Requirements: {@link #targetType} must be set, {@link #sourceType} must be
	 * set
	 */
	private static Collection<IntermediateJoinColumn> buildDefaultKeyBasedJoinColumns(
			final String relationshipName, final IntermediateStructuredType<?> targetType)
					throws ODataJPAModelException {
		final List<JPASimpleAttribute> targetKeyAttributes = targetType.getKeyAttributes(true);
		final List<IntermediateJoinColumn> joinColumns = new ArrayList<>(targetKeyAttributes.size());
		for (final JPASimpleAttribute idAttr : targetKeyAttributes) {
			final String targetKeyName = idAttr.getDBFieldName();
			String derivedSourceKey = targetKeyName;
			boolean needsQuoting = false;
			if (derivedSourceKey.startsWith("\"")) {
				// remove wrapping "" characters from generated name
				derivedSourceKey = derivedSourceKey.substring(1, derivedSourceKey.length() - 1);
				needsQuoting = true;
			}
			// if the source column was quoted we assume also quoting on our auto named
			// column
			final String generatedName = (needsQuoting ? "\"\"" : "")
					+ relationshipName.concat("_").concat(derivedSourceKey)
					+ (needsQuoting ? "\"\"" : "");
			final IntermediateJoinColumn intermediateColumn = new IntermediateJoinColumn(
					generatedName,
					targetKeyName);
			joinColumns.add(intermediateColumn);
		}
		return joinColumns;
	}

	private CsdlOnDelete setJPAOnDelete(final CascadeType[] cascades) {
		for (final CascadeType cascade : cascades) {
			if (cascade == CascadeType.REMOVE || cascade == CascadeType.ALL) {
				final CsdlOnDelete onDelete = new CsdlOnDelete();
				onDelete.setAction(CsdlOnDeleteAction.Cascade);
				return onDelete;
			}
		}
		return null;
	}
}
