package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;

/**
 * Super class without own persistence.
 *
 * @author Ralf Zozmann
 *
 */
@MappedSuperclass
public abstract class AbstractEntity {
	@Id
	@Column(name = "\"ID\"", updatable = false, nullable = false, unique = true, columnDefinition = "bigint")
	private Integer ID;

	public Integer getID() {
		return ID;
	}

	@EdmAction(name = "actionInMappedSuperclass")
	public void actionInMappedSuperclass() {
		// do nothing
	}

}
