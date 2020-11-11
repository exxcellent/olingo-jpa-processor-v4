package org.apache.olingo.jpa.processor.core.testmodel;

import java.util.Arrays;
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


  public void addLeftM2Ns(final RelationshipTargetEntity leftM2NsEntry) {
    if (leftM2Ns == null) {
      leftM2Ns = new LinkedList<>();
    }
    leftM2Ns.add(leftM2NsEntry);
    if (leftM2NsEntry.rightM2Ns == null) {
      leftM2NsEntry.rightM2Ns = new LinkedList<>();
    }
    leftM2NsEntry.rightM2Ns.add(this);
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
    source.name = "server side modified created source name";
    // with existing id we have to 'merge' instead of 'persist'
    // in Hibernate merge will generate new id's!
    final RelationshipSourceEntity merged = em.merge(source);
    return merged;
  }

  /**
   * Create 2 entities sharing the same target entity (m:n relationship)
   */
  @EdmAction
  public static Collection<RelationshipSourceEntity> createEntityCollection(@Inject final EntityManager em) {
    final RelationshipSourceEntity source1 = new RelationshipSourceEntity();
    source1.name = "created source1 name";
    // we need an ID, but cannot persist to auto generate
    source1.setID(Integer.valueOf(Long.valueOf(System.currentTimeMillis()).intValue()));

    final RelationshipSourceEntity source2 = new RelationshipSourceEntity();
    source2.name = "created source2 name";
    source2.setID(Integer.valueOf(source1.getID().intValue() + 1));

    final RelationshipTargetEntity target = new RelationshipTargetEntity();
    target.name = "created shared target name";
    target.setID(Integer.valueOf(source1.getID().intValue() + 100));

    source1.addLeftM2Ns(target);
    source2.addLeftM2Ns(target);

    return Arrays.asList(source1, source2);
  }

  /**
   * @see #createEntityCollection(EntityManager)
   */
  @EdmAction
  public static Collection<RelationshipSourceEntity> validateEntityCollection(@Inject final EntityManager em,
      @EdmActionParameter(
          name = "entities") final Collection<RelationshipSourceEntity> entities) {
    RelationshipTargetEntity sharedTarget = null;
    for (final RelationshipSourceEntity source : entities) {
      assert source.leftM2Ns.size() == 1;

      if (sharedTarget == null) {
        sharedTarget = source.leftM2Ns.iterator().next();
        //        final RelationshipTargetEntity mergedTarget = em.merge(sharedTarget);
      } else {
        // the id must be the same
        final RelationshipTargetEntity second = source.leftM2Ns.iterator().next();
        assert sharedTarget.getID().intValue() == second.getID().intValue();
        // the instance should also be the same
        assert sharedTarget == second;
      }
    }
    return entities;
  }

}
