package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.server.api.etag.CustomETagSupport;

/**
 *
 * @author Oliver Grande
 *
 */
class IntermediateEntitySet extends IntermediateModelElement implements CustomETagSupport {
  private final static Logger LOG = Logger.getLogger(IntermediateEntitySet.class.getName());

  private final JPAEntityType entityType;
  private CsdlEntitySet edmEntitySet;

  IntermediateEntitySet(final JPAEntityType et)
      throws ODataJPAModelException {
    // use same name builder as the entity is using
    super(((IntermediateModelElement) et).getNameBuilder(), ((IntermediateModelElement) et).getNameBuilder().buildFQN(et
        .getInternalName())
        .getFullQualifiedNameAsString());
    entityType = et;
    setExternalName(et.getEntitySetName());
  }

  public JPAEntityType getEntityType() {
    return entityType;
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmEntitySet == null) {
      edmEntitySet = new CsdlEntitySet();

      edmEntitySet.setName(getExternalName());
      edmEntitySet.setType(getNameBuilder().buildFQN(entityType.getExternalName()));

      // Create navigation Property Binding
      // V4: An entity set or a singleton SHOULD contain an edm:NavigationPropertyBinding element for each navigation
      // property of its entity type, including navigation properties defined on complex typed properties.
      // If omitted, clients MUST assume that the target entity set or singleton can vary per related entity.

      final List<JPAAssociationPath> naviPropertyList = entityType.getAssociationPathList();

      if (naviPropertyList != null && !naviPropertyList.isEmpty()) {
        // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406398035
        final List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
        for (final JPAAssociationPath naviPropertyPath : naviPropertyList) {
          final IntermediateNavigationProperty naviProperty = ((IntermediateNavigationProperty) naviPropertyPath
              .getLeaf());
          final JPAStructuredType target = naviProperty.getTargetEntity();
          if (JPAEntityType.class.isInstance(target)) {
            final CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
            navPropBinding.setPath(naviPropertyPath.getAlias());
            navPropBinding.setTarget(JPAEntityType.class.cast(target).getEntitySetName());
            navPropBindingList.add(navPropBinding);
          } else {
            // should never happen
            LOG.log(Level.SEVERE, "Navigation property targeting a non-entity: " + naviProperty.getInternalName()
            + " -> " + target.getInternalName());
          }
        }
        edmEntitySet.setNavigationPropertyBindings(returnNullIfEmpty(navPropBindingList));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  CsdlEntitySet getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmEntitySet;
  }

  @Override
  public boolean hasETag(final EdmBindingTarget entitySetOrSingleton) {
    try {
      return entityType.hasEtag();
    } catch (final ODataJPAModelException e) {
      LOG.log(Level.WARNING, "Couldn't detect eTag presence for " + entityType.getInternalName(), e);
      return false;
    }
  }

  @Override
  public boolean hasMediaETag(final EdmBindingTarget entitySetOrSingleton) {
    // TODO implement this
    return false;
  }
}
