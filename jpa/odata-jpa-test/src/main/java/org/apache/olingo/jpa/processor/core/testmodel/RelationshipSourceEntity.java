package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Collection;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

@Entity(name = "RelationshipSourceEntity")
@DiscriminatorValue(value = "RelationshipSourceEntity")
public class RelationshipSourceEntity extends AbstractRelationshipEntity {

	@OneToMany(mappedBy = "source", fetch = FetchType.LAZY, orphanRemoval = false)
	private Collection<RelationshipTargetEntity> targets;

}
