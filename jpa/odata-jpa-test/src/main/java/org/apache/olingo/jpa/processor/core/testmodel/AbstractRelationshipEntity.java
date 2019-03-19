package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;

@Inheritance
@DiscriminatorColumn(name = "\"Type\"")
@Entity(name = "RelationshipEntity")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::RelationshipEntity\"")
public abstract class AbstractRelationshipEntity extends AbstractEntity {

	@Column(name = "\"Name\"", length = 255)
	protected String name;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "secondRightM2Ns")
	protected Collection<AbstractRelationshipEntity> secondLeftM2Ns;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::RELATIONSHIPJoinTable\"", joinColumns = {
			@JoinColumn(referencedColumnName = "\"ID\"", name = "\"RIGHT_ID\"") }, inverseJoinColumns = {
					@JoinColumn(name = "\"LEFT_ID\"", referencedColumnName = "\"ID\"") })
	protected Collection<AbstractRelationshipEntity> secondRightM2Ns;

	@EdmAction(name = "actionInAbstractEntity")
	public void actionInAbstractEntity() {
		// do nothing
	}

}
