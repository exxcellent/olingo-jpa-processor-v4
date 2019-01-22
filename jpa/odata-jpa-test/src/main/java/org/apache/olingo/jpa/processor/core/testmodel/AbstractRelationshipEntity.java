package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.Table;

@Inheritance
@DiscriminatorColumn(name = "\"Type\"")
@Entity(name = "RelationshipEntity")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::RelationshipEntity\"")
public abstract class AbstractRelationshipEntity extends AbstractEntity {

	@Column(name = "\"Name\"", length = 255)
	protected String name;

}
