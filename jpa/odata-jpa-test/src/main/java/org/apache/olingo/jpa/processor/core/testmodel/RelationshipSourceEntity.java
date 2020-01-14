package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Collection;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;

@Entity(name = "RelationshipSourceEntity")
@DiscriminatorValue(value = "RelationshipSourceEntity")
@ODataEntity(attributeNaming = NamingStrategy.AsIs)
public class RelationshipSourceEntity extends AbstractRelationshipEntity {

  @OneToMany(mappedBy = "SOURCE", fetch = FetchType.LAZY, orphanRemoval = false)
  protected Collection<RelationshipTargetEntity> targets;

  // declare the relationship to targets as unidirectional to force usage of @JoinColumn handling inside our converter
  @OneToMany
  @JoinColumn(name = "SOURCE_ID", insertable = false, updatable = false)
  protected Collection<RelationshipTargetEntity> unidirectionalTargets;

  @ManyToMany(fetch = FetchType.EAGER, mappedBy = "rightM2Ns")
  protected Collection<RelationshipTargetEntity> leftM2Ns;
}
