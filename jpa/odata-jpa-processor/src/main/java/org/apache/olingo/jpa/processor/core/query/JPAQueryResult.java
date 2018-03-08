package org.apache.olingo.jpa.processor.core.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
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
public final class JPAQueryResult {

	public final static String ROOT_RESULT = "root";
	private final Map<JPAAssociationPath, JPAQueryResult> resultRelationshipTargets = new HashMap<JPAAssociationPath, JPAQueryResult>();;
	private final Map<String, List<Tuple>> resultValues;
	private final Long count;
	private final JPAEntityType jpaEntityType;

	public JPAQueryResult(final Map<String, List<Tuple>> result, final Long count,
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

	void putChildren(final Map<JPAAssociationPath, JPAQueryResult> childResults)
			throws ODataApplicationException {
		for (final JPAAssociationPath child : childResults.keySet()) {
			if (resultRelationshipTargets.get(child) != null) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_EXPAND_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}
		resultRelationshipTargets.putAll(childResults);
	}

	/**
	 * @see #ROOT_RESULT for 'root' entries
	 */
	public List<Tuple> getDirectMappingsResult(final String key) {
		return resultValues.get(key);
	}

	public Map<JPAAssociationPath, JPAQueryResult> getExpandChildren() {
		return resultRelationshipTargets;
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
