package org.apache.olingo.jpa.processor.core.query.result;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;

/**
 * @author Ralf Zozmann
 *
 */
public final class JPAQueryElementCollectionResult {

	private final Map<String, List<Tuple>> resultValues;
	private final Collection<JPASelector> keyColumns;

	/**
	 *
	 * @param result The key in the map is the 'derived identifier' for the entity
	 *               where the result tuple list belongs to.
	 */
	public JPAQueryElementCollectionResult(final Map<String, List<Tuple>> result, final List<JPASelector> keyColumns) {
		super();
		assert result != null;
		assert keyColumns != null;
		this.resultValues = result;
		this.keyColumns = keyColumns;// TODO copy list + unmodifiable?
	}

	public Collection<JPASelector> getKeyColumns() {
		return keyColumns;
	}

	public List<Tuple> getDirectMappingsResult(final String key) {
		return resultValues.get(key);
	}

}
