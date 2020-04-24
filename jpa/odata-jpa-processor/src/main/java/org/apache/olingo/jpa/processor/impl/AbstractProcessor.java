package org.apache.olingo.jpa.processor.impl;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.Processor;

/**
 *
 * @author Ralf Zozmann
 *
 */
abstract class AbstractProcessor implements Processor {

  protected final IntermediateServiceDocument sd;
  private final EntityManager em;
  private final JPAODataRequestContext requestContext;

  public AbstractProcessor(final JPAODataRequestContext requestContext) {
    this.requestContext = requestContext;
    this.em = requestContext.getEntityManager();
    this.sd = requestContext.getEdmProvider().getServiceDocument();
  }

  /**
   * This method is not longer useful, because all context information is given by constructor.
   */
  @Override
  public final void init(final OData odata, final ServiceMetadata serviceMetadata) {
    if (odata != requestContext.getOdata()) {
      throw new IllegalStateException("Context invalid, different instances of OData in context");
    }
    if (serviceMetadata != requestContext.getServiceMetaData()) {
      throw new IllegalStateException("Context invalid, different instances of ServiceMetadata in context");
    }
  }

  protected final EntityManager getEntityManager() {
    return em;
  }

  protected final JPAODataRequestContext getRequestContext() {
    return requestContext;
  }

  /**
   * @return The OData instance from {@link #init(OData, ServiceMetadata) init()} or <code>null</code>.
   */
  protected final OData getOData() {
    return requestContext.getOdata();
  }

  /**
   * @return The service metadata from {@link #init(OData, ServiceMetadata) init()} or <code>null</code>.
   */
  protected final ServiceMetadata getServiceMetadata() {
    return requestContext.getServiceMetaData();
  }

  /**
   * Merge the content (properties) from first entity into the second entity. The
   * result will be a new entity instance representing the merged state of both
   * entities.
   */
  protected Entity mergeEntities(final Entity from, final Entity to) {
    final Entity odataEntityMerged = new Entity();
    // copy the 'to' entity as base for merged state
    odataEntityMerged.setType(to.getType());
    odataEntityMerged.setBaseURI(to.getBaseURI());
    odataEntityMerged.setEditLink(to.getEditLink());
    odataEntityMerged.setETag(to.getETag());
    odataEntityMerged.setId(to.getId());
    odataEntityMerged.setMediaContentSource(to.getMediaContentSource());
    odataEntityMerged.setMediaContentType(to.getMediaContentType());
    odataEntityMerged.setMediaETag(to.getMediaETag());
    odataEntityMerged.setSelfLink(to.getSelfLink());
    final List<Property> propertiesMerged = odataEntityMerged.getProperties();
    for (final Property pTo : to.getProperties()) {
      // we can take the property instance self (without cloning), because the merged
      // "state" should be affected by changes on the origin 'to' entity (meaning any
      // manipulation on property values)
      propertiesMerged.add(pTo);
    }
    // overwrite properties with values from 'from'
    for (final Property pFrom : from.getProperties()) {
      final Property pExisting = odataEntityMerged.getProperty(pFrom.getName());
      if (pExisting != null) {
        // remove old property (from 'to') if we get a newer one from 'from'
        propertiesMerged.remove(pExisting);
      }
      propertiesMerged.add(pFrom);
    }
    return odataEntityMerged;
  }

  /**
   * @return TRUE if the request contains a header with the requested preference value.
   */
  protected static boolean hasPreference(final ODataRequest request, final String headerName, final String headerValue) {
    final String value = request.getHeader(headerName);
    if (value == null) {
      return false;
    }
    return value.equalsIgnoreCase(headerValue);
  }

}
