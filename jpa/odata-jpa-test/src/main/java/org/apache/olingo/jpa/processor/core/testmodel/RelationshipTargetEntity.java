package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity(name = "RelationshipTargetEntity")
@DiscriminatorValue(value = "RelationshipTargetEntity")
public class RelationshipTargetEntity extends AbstractRelationshipEntity {

  // force usage of (default id name pattern) as join column, because no
  // 'mappedBy' or @JoinColumn is given
  // do not lower case the attribute name: depending on the writing in *.sql file
  // EclipseLInk or Hibernate will fail with it's auto naming
  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
  // @JoinColumn(name = "SOURCE_ID", insertable = false, updatable = false)
  protected RelationshipSourceEntity SOURCE;

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
