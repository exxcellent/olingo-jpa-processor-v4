package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

class JPATupleExpandResultConverter extends JPATupleAbstractConverter {
	private final Tuple parentRow;
	private final JPAAssociationPath assoziation;

	public JPATupleExpandResultConverter(final JPAQueryResult jpaExpandResult, final Tuple parentRow,
			final JPAAssociationPath assoziation, final UriHelper uriHelper, final IntermediateServiceDocument sd,
			final ServiceMetadata serviceMetadata) throws ODataApplicationException {

		super(jpaExpandResult, uriHelper, sd, serviceMetadata);
		this.parentRow = parentRow;
		this.assoziation = assoziation;
	}

	public Link getResult() throws ODataApplicationException {
		final Link link = new Link();
		link.setTitle(assoziation.getLeaf().getExternalName());
		link.setRel(Constants.NS_ASSOCIATION_LINK_REL + link.getTitle());
		link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
		final EntityCollection expandCollection = createEntityCollection();
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

	private final String buildConcatenatedKey(final Tuple row, final List<JPAOnConditionItem> joinColumns) {
		final StringBuilder buffer = new StringBuilder();
		JPASelector left;
		for (final JPAOnConditionItem item : joinColumns) {
			// ignored attributes are not part of the query selection result
			left = item.getLeftPath();
			buffer.append(JPASelector.PATH_SEPERATOR);
			// if we got here an exception, then a required (key) join column was not
			// selected in the query (see JPAQuery to fix!)
			buffer.append(row.get(left.getAlias()));
		}
		if (buffer.length() < 1) {
			return null;
		}
		buffer.deleteCharAt(0);
		return buffer.toString();
	}

	private EntityCollection createEntityCollection() throws ODataApplicationException {

		List<Tuple> subResult = null;
		try {
			final String key = buildConcatenatedKey(parentRow, assoziation.getJoinConditions());
			subResult = getJpaQueryResult().getDirectMappingsResult(key);
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

		final EntityCollection odataEntityCollection = new EntityCollection();
		if (subResult != null) {
			for (final Tuple row : subResult) {
				convertRow2ODataEntity(row, odataEntityCollection);
			}
		}
		// TODO odataEntityCollection.setId(createId());
		return odataEntityCollection;
	}

}
