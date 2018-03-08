package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Super class without own persistence.
 *
 * @author rzozmann
 *
 */
@MappedSuperclass
public abstract class AbstractEntity {
	@Id
	@Column(name = "\"ID\"", updatable = false, nullable = false, unique = true)
	private Integer ID;

}
