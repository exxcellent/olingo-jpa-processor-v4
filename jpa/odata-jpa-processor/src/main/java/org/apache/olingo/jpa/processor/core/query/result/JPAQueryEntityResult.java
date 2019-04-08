package org.apache.olingo.jpa.processor.core.query.result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;

/**
 * Builds a hierarchy of expand results. One instance contains on the on hand of the result itself, a map which has the
 * join columns values of the parent as its key and on the other hand a map that point the results of the next expand.
 * The join columns are concatenated in the order they are stored in the corresponding Association Path.
 * @author Oliver Grande
 *
 */
public final class JPAQueryEntityResult {

	public final static String ROOT_RESULT = "root";
	private final Map<JPAAssociationPath, JPAQueryEntityResult> resultRelationshipTargets = new HashMap<>();
	private final Map<JPAAttribute<?>, JPAQueryElementCollectionResult> resultElementCollectionTargets = new HashMap<>();
	private final Map<String, List<Tuple>> resultValues;
	private final Long count;
	private final JPAEntityType jpaEntityType;

	public JPAQueryEntityResult(final Map<String, List<Tuple>> result, final Long count,
			final JPAEntityType jpaEntityType) {
		super();
		assertNotNull(result);
		assertNotNull(jpaEntityType);
		this.resultValues = result;
		this.count = count;
		this.jpaEntityType = jpaEntityType;
	}

	private void assertNotNull(final Object instance) {
		if (instance == null) {
			throw new NullPointerException();
		}
	}

	public void putExpandResults(final Map<JPAAssociationPath, JPAQueryEntityResult> childResults)
			throws ODataApplicationException {
		// check already present entries
		for (final JPAAssociationPath child : childResults.keySet()) {
			if (resultRelationshipTargets.get(child) != null) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_EXPAND_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}
		resultRelationshipTargets.putAll(childResults);
	}

	public void putElementCollectionResults(
			final Map<JPAAttribute<?>, JPAQueryElementCollectionResult> collectionResults)
			throws ODataApplicationException {
		// check already present entries
		for (final Entry<JPAAttribute<?>, JPAQueryElementCollectionResult> entry : collectionResults.entrySet()) {
			if (resultElementCollectionTargets.containsKey(entry.getKey())) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}
		resultElementCollectionTargets.putAll(collectionResults);
	}

	/**
	 * @see #ROOT_RESULT for 'root' entries
	 */
	public List<Tuple> getDirectMappingsResult(final String key) {
		return resultValues.get(key);
	}

	public Map<JPAAssociationPath, JPAQueryEntityResult> getExpandChildren() {
		return resultRelationshipTargets;
	}

	public Map<JPAAttribute<?>, JPAQueryElementCollectionResult> getElementCollections() {
		return resultElementCollectionTargets;
	}

	public boolean hasCount() {
		return count != null;
	}

	public Integer getCount() {
		return count != null ? Integer.valueOf(count.intValue()) : null;
	}

	public JPAEntityType getEntityType() {
		return jpaEntityType;
	}
}
