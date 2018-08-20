package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

public class JPAAssociationPathImpl implements JPAAssociationPath {
	final private String alias;
	final private List<JPAAttribute> pathElements;
	final private IntermediateStructuredType sourceType;
	final private IntermediateStructuredType targetType;
	private List<IntermediateJoinColumn> joinColumns;
	private final PersistentAttributeType cardinality;

	/**
	 * This constructor is used to create a 'composite' association path consisting
	 * the an already existing path from a nested complex type and the owning
	 * attribute in the top level structured type.
	 */
	JPAAssociationPathImpl(final JPAEdmNameBuilder namebuilder, final JPAAttribute attribute,
			final JPAAssociationPath associationPath, final IntermediateStructuredType source,
			final List<IntermediateJoinColumn> joinColumns) {

		final List<JPAAttribute> pathElementsBuffer = new ArrayList<JPAAttribute>();
		pathElementsBuffer.add(attribute);
		pathElementsBuffer.addAll(associationPath.getPathElements());

		alias = namebuilder.buildNaviPropertyBindingName(associationPath, attribute);
		this.sourceType = source;
		this.targetType = (IntermediateStructuredType) associationPath.getTargetType();
		if (joinColumns.isEmpty()) {
			// if nor explicit join columns are given for the 'attribute' the we take the
			// join columns as defined on the nested association path
			this.joinColumns = ((JPAAssociationPathImpl) associationPath).getJoinColumns();
		} else {
			this.joinColumns = joinColumns;
		}
		this.pathElements = Collections.unmodifiableList(pathElementsBuffer);
		this.cardinality = ((JPAAssociationPathImpl) associationPath).getCardinality();
	}

	JPAAssociationPathImpl(final IntermediateNavigationProperty navProperty, final IntermediateStructuredType source)
			throws ODataJPAModelException {

		alias = navProperty.getExternalName();
		// the given source may be a sub class of the class declared via
		// navProperty::sourceType!
		this.sourceType = source;
		this.targetType = (IntermediateStructuredType) navProperty.getTargetEntity();
		this.joinColumns = navProperty.getJoinColumns();
		this.pathElements = Collections.singletonList(navProperty);
		this.cardinality = navProperty.getJoinCardinality();
	}

	private List<IntermediateJoinColumn> getJoinColumns() {
		return joinColumns;
	}

	private PersistentAttributeType getCardinality() {
		return cardinality;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public List<JPAOnConditionItem> getJoinConditions() throws ODataJPAModelException {
		final List<JPAOnConditionItem> joinConditions = new ArrayList<JPAOnConditionItem>();
		JPAOnConditionItemImpl onCondition;
		JPASelector selectorLeft;
		JPASelector selectorRight;
		for (final IntermediateJoinColumn column : this.joinColumns) {
			try {
				switch (cardinality) {
				case MANY_TO_ONE:
				case MANY_TO_MANY: /* TODO m:n really also ?! */
				case ONE_TO_ONE:
					selectorLeft = findJoinConditionPath(sourceType, column.getName());
					selectorRight = findJoinConditionPath(targetType,
							column.getReferencedColumnName());
					onCondition = new JPAOnConditionItemImpl(selectorLeft,
							selectorRight);
					break;
				case ONE_TO_MANY:
					selectorLeft = findJoinConditionPath(sourceType,
							column.getReferencedColumnName());
					selectorRight = findJoinConditionPath(targetType, column.getName());
					onCondition = new JPAOnConditionItemImpl(selectorLeft, selectorRight);
					break;
				default:
					throw new ODataJPAModelException(MessageKeys.GENERAL,
							"Invalid relationship declaration: " + column.getName() + "<->"
									+ column.getReferencedColumnName() + " between " + sourceType.getInternalName()
									+ " and " + targetType.getInternalName());
				}
				joinConditions.add(onCondition);
			} catch (final IllegalArgumentException ex) {
				throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM, ex,
						"Invalid relationship declaration: " + column.getName() + "<->"
								+ column.getReferencedColumnName() + " between " + sourceType.getInternalName()
								+ " and " + targetType.getInternalName());
			}
		}
		return joinConditions;
	}

	private JPASelector findJoinConditionPath(final IntermediateStructuredType type, final String joinColumnName)
			throws ODataJPAModelException {
		final JPASelector selector = type.getPathByDBField(joinColumnName);
		if (selector != null) {
			return selector;
		}

		// try as association (maybe the result is 'this')
		for (final Entry<String, JPAAssociationPathImpl> entry : type.getAssociationPathMap().entrySet()) {
			for (final IntermediateJoinColumn jc : entry.getValue().getJoinColumns()) {
				if (jc.getName().equals(joinColumnName)) {
					return entry.getValue();
				}
			}
		}

		throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM,
				"Invalid relationship declaration: " + joinColumnName + " ->" + " between "
						+ sourceType.getInternalName() + " and " + targetType.getInternalName());
	}

	@Override
	public JPAAttribute getLeaf() {
		return pathElements.get(pathElements.size() - 1);
	}

	@Override
	public List<JPAAttribute> getPathElements() {
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
