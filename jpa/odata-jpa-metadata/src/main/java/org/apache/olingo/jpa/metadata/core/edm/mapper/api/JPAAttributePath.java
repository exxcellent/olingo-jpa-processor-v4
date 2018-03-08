package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

public interface JPAAttributePath extends JPASelector {

	String getDBFieldName();

	boolean ignore();

}