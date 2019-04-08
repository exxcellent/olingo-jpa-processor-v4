package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException.MessageKeys;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

class JPATupleExpandResultConverter extends JPATupleAbstractConverter {
	private final Tuple owningEnityRow;
	private final JPAAssociationPath assoziation;

	public JPATupleExpandResultConverter(final JPAEntityType jpaTargetEntity, final Tuple parentRow,
			final JPAAssociationPath assoziation, final UriHelper uriHelper, final IntermediateServiceDocument sd,
			final ServiceMetadata serviceMetadata) throws ODataApplicationException {

		super(jpaTargetEntity, uriHelper, sd, serviceMetadata);
		this.owningEnityRow = parentRow;
		this.assoziation = assoziation;
	}

	public Link convertTuples2ExpandLink(final JPAQueryEntityResult jpaExpandResult) throws ODataApplicationException {
		final Link link = new Link();
		link.setTitle(assoziation.getLeaf().getExternalName());
		link.setRel(Constants.NS_ASSOCIATION_LINK_REL + link.getTitle());
		link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
		final EntityCollection expandCollection = createEntityCollection(jpaExpandResult);
		if (assoziation.getLeaf().isCollection()) {
			link.setInlineEntitySet(expandCollection);
			expandCollection.setCount(Integer.valueOf(expandCollection.getEntities().size()));
			// TODO link.setHref(parentUri.toASCIIString());
		} else {
			if (expandCollection.getEntities() != null && !expandCollection.getEntities().isEmpty()) {
				final Entity expandEntity = expandCollection.getEntities().get(0);
				link.setInlineEntity(expandEntity);
				// TODO link.setHref(expandCollection.getId().toASCIIString());
			}
		}
		return link;
	}

	/**
	 * @deprecated Merge with
	 *             {@link JPATupleAbstractConverter#buildOwningEntityKey(Tuple, java.util.Collection)}
	 */
	@Deprecated
	private final String buildOwningEntityKey(final Tuple row, final List<JPASelector> joinColumns)
			throws ODataApplicationException {
		final StringBuilder buffer = new StringBuilder();
		// we use all columns used in JOIN from left side (the owning entity) to build a
		// identifying key accessing all nested relationship results
		String alias;
		for (final JPASelector item : joinColumns) {
			if (JPAAssociationPath.class.isInstance(item)) {
				final JPAAssociationPath asso = ((JPAAssociationPath) item);
				try {
					// select all the key attributes from target (right) side table so we can build
					// a 'result
					// key' from the tuples in the result set
					final List<JPASimpleAttribute> keys = asso.getTargetType().getKeyAttributes(true);
					for (final JPASimpleAttribute jpaAttribute : keys) {
						alias = JPAAbstractEntityQuery.buildTargetJoinAlias(asso, jpaAttribute);
						buffer.append(JPASelector.PATH_SEPERATOR);
						buffer.append(row.get(alias));
					}
				} catch (final ODataJPAModelException e) {
					throw new ODataJPAQueryException(MessageKeys.QUERY_RESULT_EXPAND_ERROR,
							HttpStatusCode.INTERNAL_SERVER_ERROR, e);
				}

			} else {
				// if we got here an exception, then a required (key) join column was not
				// selected in the query (see JPAEntityQuery to fix!)
				buffer.append(JPASelector.PATH_SEPERATOR);
				buffer.append(row.get(item.getAlias()));
			}
		}
		if (buffer.length() < 1) {
			return null;
		}
		buffer.deleteCharAt(0);
		return buffer.toString();
	}

	private EntityCollection createEntityCollection(final JPAQueryEntityResult jpaExpandResult)
			throws ODataApplicationException {

		List<Tuple> subResult = null;
		try {
			// build key using source side + source side join columns, resulting key must be
			// identical for target side + target side join columns
			final String key = buildOwningEntityKey(owningEnityRow, assoziation.getLeftPaths());
			subResult = jpaExpandResult.getDirectMappingsResult(key);
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

		final EntityCollection odataEntityCollection = new EntityCollection();
		if (subResult != null) {
			Entity entity;
			for (final Tuple row : subResult) {
				entity = convertTuple2ODataEntity(row, jpaExpandResult);
				odataEntityCollection.getEntities().add(entity);
			}
		}
		// TODO odataEntityCollection.setId(createId());
		return odataEntityCollection;
	}

}
