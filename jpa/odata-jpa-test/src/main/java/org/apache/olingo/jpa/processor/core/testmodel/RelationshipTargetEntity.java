package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "RelationshipTargetEntity")
@DiscriminatorValue(value = "RelationshipTargetEntity")
public class RelationshipTargetEntity extends AbstractRelationshipEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "\"SOURCE_ID\"", referencedColumnName = "\"ID\"")
	private RelationshipSourceEntity source;

}
