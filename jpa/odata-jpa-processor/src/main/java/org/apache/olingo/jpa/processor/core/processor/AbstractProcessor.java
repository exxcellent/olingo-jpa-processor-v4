package org.apache.olingo.jpa.processor.core.processor;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
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
	protected final JPAODataSessionContextAccess context;
	protected final EntityManager em;

	private ServiceMetadata serviceMetadata = null;
	private OData odata = null;

	public AbstractProcessor(final JPAODataSessionContextAccess context, final EntityManager em) {
		this.context = context;
		this.em = em;
		this.sd = context.getEdmProvider().getServiceDocument();
	}

	@Override
	public final void init(final OData odata, final ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	/**
	 * @return The OData instance from {@link #init(OData, ServiceMetadata) init()} or <code>null</code>.
	 */
	protected OData getOData() {
		return odata;
	}

	/**
	 * @return The service metadata from {@link #init(OData, ServiceMetadata) init()} or <code>null</code>.
	 */
	protected ServiceMetadata getServiceMetadata() {
		return serviceMetadata;
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
