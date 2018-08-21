package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAAssociationPath extends JPASelector {

	List<JPAOnConditionItem> getJoinConditions() throws ODataJPAModelException;

	JPAStructuredType getTargetType();

	JPAStructuredType getSourceType();

}