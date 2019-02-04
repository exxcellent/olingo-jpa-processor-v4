package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Collection;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity(name = "RelationshipTargetEntity")
@DiscriminatorValue(value = "RelationshipTargetEntity")
public class RelationshipTargetEntity extends AbstractRelationshipEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"SOURCE_ID\"", insertable = false, updatable = false)
	protected RelationshipSourceEntity source;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::RELATIONSHIPJoinTable\"", joinColumns = {
			@JoinColumn(referencedColumnName = "\"ID\"", name = "\"RIGHT_ID\"") }, inverseJoinColumns = {
					@JoinColumn(name = "\"LEFT_ID\"", referencedColumnName = "\"ID\"") })
	protected Collection<RelationshipSourceEntity> rightM2Ns;

	/**
	 * Define a (not really useful) 1:n relationship with same number of join
	 * columns on both side, to create referential constraints
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@JoinTable(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::RELATIONSHIPJoinTable\"", joinColumns = {
			@JoinColumn(referencedColumnName = "\"ID\"", name = "\"RIGHT_ID\"") }, inverseJoinColumns = {
					@JoinColumn(name = "\"LEFT_ID\"", referencedColumnName = "\"ID\"") })
	protected Collection<RelationshipSourceEntity> one2ManyTest;

}
