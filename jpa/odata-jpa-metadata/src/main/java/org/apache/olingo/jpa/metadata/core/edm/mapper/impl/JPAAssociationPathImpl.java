package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

public class JPAAssociationPathImpl implements JPAAssociationPath {
	final private String alias;
	final private List<JPAAttribute<?>> pathElements;
	final private IntermediateStructuredType<?> sourceType;
	final private IntermediateStructuredType<?> targetType;
	private final List<IntermediateJoinColumn> sourceJoinColumns;
	private final List<IntermediateJoinColumn> targetJoinColumns;
	private final PersistentAttributeType cardinality;
	private final boolean useJoinTable;
	private List<JPASelector> leftSelectors = null;
	private List<JPASelector> rightSelectors = null;

	/**
	 * This constructor is used to create a 'composite' association path consisting
	 * the an already existing path from a nested complex type and the owning
	 * attribute in the top level structured type.
	 */
	JPAAssociationPathImpl(final JPAEdmNameBuilder namebuilder, final JPAAttribute<?> attribute,
			final JPAAssociationPath associationPath, final IntermediateStructuredType<?> source,
			final List<IntermediateJoinColumn> joinColumns) {

		final List<JPAAttribute<?>> pathElementsBuffer = new ArrayList<JPAAttribute<?>>();
		pathElementsBuffer.add(attribute);
		pathElementsBuffer.addAll(associationPath.getPathElements());

		alias = namebuilder.buildNaviPropertyBindingName(associationPath, attribute);
		this.sourceType = source;
		this.targetType = (IntermediateStructuredType<?>) associationPath.getTargetType();
		if (joinColumns.isEmpty()) {
			// if nor explicit join columns are given for the 'attribute' the we take the
			// join columns as defined on the nested association path
			this.sourceJoinColumns = ((JPAAssociationPathImpl) associationPath).getSourceJoinColumns();
			this.useJoinTable = associationPath.hasJoinTableBetweenSourceAndTarget();
			this.targetJoinColumns = getTargetJoinColumns();
		} else {
			this.sourceJoinColumns = joinColumns;
			this.useJoinTable = false;
			this.targetJoinColumns = null;
		}
		this.pathElements = Collections.unmodifiableList(pathElementsBuffer);
		this.cardinality = ((JPAAssociationPathImpl) associationPath).getCardinality();
	}

	JPAAssociationPathImpl(final IntermediateNavigationProperty navProperty, final IntermediateStructuredType<?> source)
			throws ODataJPAModelException {

		alias = navProperty.getExternalName();
		// the given source may be a sub class of the class declared via
		// navProperty::sourceType!
		this.sourceType = source;
		this.targetType = (IntermediateStructuredType<?>) navProperty.getTargetEntity();
		this.sourceJoinColumns = navProperty.getSourceJoinColumns();
		this.targetJoinColumns = navProperty.getTargetJoinColumns();
		this.useJoinTable = navProperty.doesUseJoinTable();
		this.pathElements = Collections.singletonList(navProperty);
		this.cardinality = navProperty.getJoinCardinality();
	}

	private List<IntermediateJoinColumn> getSourceJoinColumns() {
		return sourceJoinColumns;
	}

	private List<IntermediateJoinColumn> getTargetJoinColumns() {
		return targetJoinColumns;
	}

	@Override
	public boolean hasJoinTableBetweenSourceAndTarget() {
		return useJoinTable;
	}

	private PersistentAttributeType getCardinality() {
		return cardinality;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public List<JPASelector> getLeftPaths() throws ODataJPAModelException {
		if (leftSelectors == null) {
			determineJoinSelectors();
		}
		return leftSelectors;
	}

	@Override
	public List<JPASelector> getRightPaths() throws ODataJPAModelException {
		if (rightSelectors == null) {
			determineJoinSelectors();
		}
		return rightSelectors;
	}

	private void determineJoinSelectors() throws ODataJPAModelException {
		leftSelectors = new ArrayList<JPASelector>(sourceJoinColumns.size());
		rightSelectors = new ArrayList<JPASelector>(sourceJoinColumns.size());
		JPASelector selectorLeft;
		JPASelector selectorRight;

		for (final IntermediateJoinColumn column : this.sourceJoinColumns) {
			fillMissingName(cardinality, sourceType, targetType, column);
			// always take source for left side
			selectorLeft = findJoinConditionPath(sourceType, column.getSourceEntityColumnName());
			leftSelectors.add(selectorLeft);
			selectorRight = null;
			// take source for right side only if no join table exists, defining target
			// columns
			if(!useJoinTable) {
				selectorRight = findJoinConditionPath(targetType, column.getTargetColumnName());
				if (selectorRight != null) {
					rightSelectors.add(selectorRight);
				}
			}
		}

		if (useJoinTable) {
			// force target join columns
			rightSelectors.addAll(determineTargetSelectorBehindJoinTable());
		}
		if (rightSelectors.isEmpty()) {
			throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM,
					"Invalid relationship declaration: " + alias + " ->" + " between " + sourceType.getInternalName()
					+ " and " + targetType.getInternalName());
		}

	}


	static void fillMissingName(final PersistentAttributeType cardinality, final JPAStructuredType sourceType,
			final JPAStructuredType targetType,
			final IntermediateJoinColumn intermediateColumn) throws ODataJPAModelException {

		final String refColumnName = intermediateColumn.getTargetColumnName();
		final String name = intermediateColumn.getSourceEntityColumnName();
		if ((name == null || name.isEmpty())) {
			final JPASimpleAttribute keyAttribute = determineSingleKeyAttribute(sourceType);
			intermediateColumn.setSourceEntityColumnName(keyAttribute.getDBFieldName());
		}
		if ((refColumnName == null || refColumnName.isEmpty())) {
			final JPASimpleAttribute keyAttribute = determineSingleKeyAttribute(targetType);
			intermediateColumn.setTargetColumnName(keyAttribute.getDBFieldName());
		}
	}

	private static JPASimpleAttribute determineSingleKeyAttribute(final JPAStructuredType theType)
			throws ODataJPAModelException {
		final List<JPASimpleAttribute> attributes = theType.getKeyAttributes(false);
		if (attributes.isEmpty()) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ASSOCIATION);
		}
		if (attributes.size() > 1) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
					theType.getExternalName(), "?");
		}
		return attributes.get(0);
	}

	private JPASelector findJoinConditionPath(final IntermediateStructuredType<?> type,
			final String joinColumnName)
					throws ODataJPAModelException {
		final JPASelector selector = type.getPathByDBField(joinColumnName);
		if (selector != null) {
			return selector;
		}

		// try as association (maybe the result is 'this'), this will lead to an 'join'
		// for navigation queries, for entity queries we will throw an warning
		for (final Entry<String, JPAAssociationPathImpl> entry : type.getAssociationPathMap().entrySet()) {
			for (final IntermediateJoinColumn jc : entry.getValue().getSourceJoinColumns()) {
				if (jc.getSourceEntityColumnName().equals(joinColumnName)) {
					return entry.getValue();
				}
			}
		}

		if (useJoinTable && targetJoinColumns != null) {
			// accept incomplete join path, because we have no knowledge about the things
			// behind the join table; we select all attributes existing in target entity
			// identified by join column declaration
			return null;
		}

		throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM,
				"Invalid relationship declaration: " + joinColumnName + " ->" + " between "
						+ sourceType.getInternalName() + " and " + targetType.getInternalName());
	}

	private List<JPASelector> determineTargetSelectorBehindJoinTable() throws ODataJPAModelException {
		// the target join columns are ALL required columns, so we can take all without
		// further logic
		final List<JPASelector> selectors = new LinkedList<>();
		JPASelector selector;
		for (final IntermediateJoinColumn jc : targetJoinColumns) {
			fillMissingName(cardinality, sourceType, targetType, jc);
			selector = targetType.getPathByDBField(jc.getSourceEntityColumnName());
			if (selector == null) {
				throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM,
						"Invalid relationship declaration: between " + sourceType.getInternalName() + " and "
								+ targetType.getInternalName());
			}
			selectors.add(selector);
		}
		if (!selectors.isEmpty()) {
			return selectors;
		}
		throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM, "Invalid relationship declaration: between "
				+ sourceType.getInternalName() + " and " + targetType.getInternalName());
	}

	@Override
	public JPAAttribute<?> getLeaf() {
		return pathElements.get(pathElements.size() - 1);
	}

	@Override
	public List<JPAAttribute<?>> getPathElements() {
		return pathElements;
	}

	@Override
	public JPAStructuredType getTargetType() {
		return targetType;
	}

	@Override
	public JPAStructuredType getSourceType() {
		return sourceType;
	}

	@Override
	public int compareTo(final JPASelector o) {
		if (o == null) {
			return -1;
		}
		return this.alias.compareTo(o.getAlias());
	}
}
