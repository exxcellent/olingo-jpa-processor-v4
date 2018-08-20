package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

public abstract class JPATupleAbstractConverter extends JPAAbstractConverter {

	public static final String ACCESS_MODIFIER_GET = "get";
	public static final String ACCESS_MODIFIER_SET = "set";
	public static final String ACCESS_MODIFIER_IS = "is";
	private final JPAQueryResult jpaQueryResult;

	public JPATupleAbstractConverter(final JPAQueryResult jpaQueryResult,
			final UriHelper uriHelper, final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata)
					throws ODataApplicationException {
		super(jpaQueryResult.getEntityType(), uriHelper,sd, serviceMetadata);

		this.jpaQueryResult = jpaQueryResult;
	}

	protected JPAQueryResult getJpaQueryResult() {
		return jpaQueryResult;
	}

	@Override
	protected Collection<? extends Link> createExpand(final Tuple row, final URI uri)
			throws ODataApplicationException {
		final Map<JPAAssociationPath, JPAQueryResult> children = jpaQueryResult.getExpandChildren();
		if (children == null) {
			return Collections.emptyList();
		}
		final List<Link> entityExpandLinks = new ArrayList<Link>(children.size());
		for (final Entry<JPAAssociationPath, JPAQueryResult> entry : children.entrySet()) {
			try {
				if (jpaConversionTargetEntity.getDeclaredAssociation(entry.getKey()) == null) {
					continue;
				}
				final Link expand = new JPATupleExpandResultConverter(entry.getValue(), row, entry.getKey(),
						getUriHelper(), sd, serviceMetadata).getResult();
				// TODO Check how to convert Organizations('3')/AdministrativeInformation?$expand=Created/User
				entityExpandLinks.add(expand);
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, entry.getKey().getAlias());
			}
		}
		return entityExpandLinks;
	}

}