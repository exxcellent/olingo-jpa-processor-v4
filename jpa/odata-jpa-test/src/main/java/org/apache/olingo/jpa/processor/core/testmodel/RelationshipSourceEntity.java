package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;

@Entity(name = "RelationshipSourceEntity")
@DiscriminatorValue(value = "RelationshipSourceEntity")
@ODataEntity(attributeNaming = NamingStrategy.AsIs)
public class RelationshipSourceEntity extends AbstractRelationshipEntity {

  @OneToMany(mappedBy = "SOURCE", fetch = FetchType.LAZY, orphanRemoval = false, cascade = CascadeType.ALL)
  protected Collection<RelationshipTargetEntity> targets;

  // declare the relationship to targets as unidirectional to force usage of @JoinColumn handling inside our converter
  @OneToMany
  @JoinColumn(name = "SOURCE_ID", insertable = false, updatable = false)
  protected Collection<RelationshipTargetEntity> unidirectionalTargets;

  @ManyToMany(fetch = FetchType.EAGER, mappedBy = "rightM2Ns")
  protected Collection<RelationshipTargetEntity> leftM2Ns;

  public void addTarget(final RelationshipTargetEntity target) {
    if (targets == null) {
      targets = new LinkedList<>();
    }
    targets.add(target);
    target.SOURCE = this;
  }


  /**
   * @see #saveEntities(EntityManager, RelationshipSourceEntity)
   */
  @EdmAction
  public static RelationshipSourceEntity createEntities(@Inject final EntityManager em) {
    final RelationshipSourceEntity source = new RelationshipSourceEntity();
    source.name = "created source name";
    // we need an ID, but cannot persist to auto generate
    source.setID(Integer.valueOf(Long.valueOf(System.currentTimeMillis()).intValue()));
    final RelationshipTargetEntity target = new RelationshipTargetEntity();
    target.name = "created target name";
    target.setID(Integer.valueOf(source.getID().intValue() + 1));
    source.addTarget(target);
    return source;
  }

  /**
   * @see #createEntities(EntityManager)
   */
  @EdmAction
  public static RelationshipSourceEntity saveEntities(@Inject final EntityManager em, @EdmActionParameter(
      name = "source") final RelationshipSourceEntity source) {
    for (final RelationshipTargetEntity target : source.targets) {
      assert target.SOURCE != null;
      assert target.SOURCE == source;
    }
    final Integer sourceId = source.getID();
    source.name = "server side modified created source name";
    // with existing id we have to 'merge' instead of 'persist'
    em.merge(source);
    // must not be overwritten
    assert sourceId.equals(source.getID());
    return source;
  }

}
