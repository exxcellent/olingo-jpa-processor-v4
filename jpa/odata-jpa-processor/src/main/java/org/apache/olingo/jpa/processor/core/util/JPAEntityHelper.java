package org.apache.olingo.jpa.processor.core.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.ServiceDocument;
import org.apache.olingo.jpa.processor.core.query.JPAEntityConverter;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAEntityHelper {

	private final EntityManager em;
	private final ServiceDocument sd;
	private final ServiceMetadata serviceMetadata;
	private final UriHelper uriHelper;

	public JPAEntityHelper(final EntityManager em, final ServiceDocument sd, final ServiceMetadata serviceMetadata,
			final UriHelper uriHelper) {
		this.em = em;
		this.sd = sd;
		this.serviceMetadata = serviceMetadata;
		this.uriHelper = uriHelper;
	}

	/**
	 * Invoke the corresponding JAVA method.
	 *
	 * @return The result or <code>null</code> if no result parameter is defined or value self is <code>null</code>
	 * @see #loadJPAEntity(JPAEntityType, Entity)
	 */
	@SuppressWarnings("unchecked")
	public final <R> R invokeActionMethod(final JPAEntityType jpaType, final Entity oDataEntity,
			final JPAAction jpaAction, final Map<String, Parameter> parameters) throws ODataException {
		// final Method javaMethod = determineJavaMethod(jpaAction);
		// if(javaMethod.getDeclaringClass() != jpaType.getTypeClass()) {
		// throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL);
		// }
		final Object jpaEntity = loadJPAEntity(jpaType, oDataEntity);
		if(jpaEntity == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL);
		}
		final Object[] args = buildArguments(jpaAction, parameters);
		return (R) jpaAction.invoke(jpaEntity, args);
	}

	private Object[] buildArguments(final JPAAction jpaAction, final Map<String, Parameter> parameterValues)
			throws ODataException {
		final Object[] args = new Object[jpaAction.getParameters().size()];
		for(int i=0;i<jpaAction.getParameters().size();i++) {
			args[i] = null;
			final JPAOperationParameter jpaParameter = jpaAction.getParameters().get(i);
			final Parameter p = parameterValues.get(jpaParameter.getName());
			if(p == null) {
				continue;
			}
			switch(p.getValueType()) {
			case PRIMITIVE:
				args[i] = p.getValue();
				break;
			case ENTITY:
				final Entity entity = p.asEntity();
				final EntityType<?> persistenceType = em.getMetamodel().entity(jpaParameter.getType());

				final JPAEntityConverter entityConverter = new JPAEntityConverter(persistenceType,
						uriHelper, sd, serviceMetadata, em.getMetamodel());
				final Object jpaEntity = entityConverter.convertOData2JPAEntity(entity);
				args[i] = jpaEntity;
				break;
			default:
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED);
			}
		}
		return args;
	}

	// private Method determineJavaMethod(final JPAAction jpaAction) {
	// return ((IntermediateAction)jpaAction).getJavaMethod();
	// }

	/**
	 * Load/search a JPA entity based on the identifier taken from the given OData entity. The JPA entity will assigned to the
	 * current {@link #getEntityManager() entity manager}.
	 *
	 * @param oDataEntity The OData entity used to identify the corresponding JPA entity.
	 * @return A instance of one of the {@link Metamodel#getEntities() managed types}, loaded based on the given OData entity.
	 * @throws ODataJPAModelException
	 */
	@SuppressWarnings("unchecked")
	public final <O> O loadJPAEntity(final JPAEntityType jpaType, final Entity oDataEntity) throws ODataJPAModelException {
		final List<Object> listPrimaryKeyValues = new LinkedList<>();
		// TODO risk: the order of (primary) key/id attributes must be the same as in descriptor; that is not ensured
		for(final JPAAttribute jpaAttribute: jpaType.getKeyAttributes()) {
			if(jpaAttribute.isComplex() || jpaAttribute.isAssociation()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_COMPLEX_TYPE);
			}
			final Object value = oDataEntity.getProperty(jpaAttribute.getExternalName()).getValue();
			listPrimaryKeyValues.add(value);
		}
		if(listPrimaryKeyValues.isEmpty()) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
		}
		return em.find((Class<O>)jpaType.getTypeClass(), listPrimaryKeyValues, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
	}

}
