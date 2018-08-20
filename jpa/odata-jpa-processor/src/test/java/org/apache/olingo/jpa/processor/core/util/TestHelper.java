package org.apache.olingo.jpa.processor.core.util;

import java.lang.reflect.AnnotatedElement;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunctions;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;

public class TestHelper {
	final private Metamodel jpaMetamodel;
	final public IntermediateServiceDocument sd;
	final public JPAEdmProvider edmProvider;

	public TestHelper(final Metamodel jpaMetamodel, final String namespace) throws ODataException {
		this.jpaMetamodel = jpaMetamodel;
		edmProvider = new JPAEdmProvider(namespace, jpaMetamodel);
		sd = edmProvider.getServiceDocument();
		sd.getEdmSchemas();
	}

	@Deprecated
	public TestHelper(final EntityManagerFactory emf, final String namespace) throws ODataException {
		this.jpaMetamodel = emf.getMetamodel();
		edmProvider = new JPAEdmProvider(namespace, jpaMetamodel);
		sd = edmProvider.getServiceDocument();
		sd.getEdmSchemas();
	}

	public EntityType<?> getEntityType(final String typeName) {
		for (final EntityType<?> entityType : jpaMetamodel.getEntities()) {
			if (entityType.getJavaType().getSimpleName().equals(typeName)) {
				return entityType;
			}
		}
		return null;
	}

	public JPAEntityType getJPAEntityType(final String entitySetName) throws ODataJPAModelException {
		return sd.getEntitySetType(entitySetName);
	}

	public JPAAssociationPath getJPAAssociationPath(final String entitySetName, final String attributeExtName)
			throws ODataJPAModelException {
		final JPAEntityType jpaEntity = sd.getEntitySetType(entitySetName);
		return jpaEntity.getAssociationPath(attributeExtName);
	}

	public JPAAttribute getJPAAssociation(final String entitySetName, final String attributeIntName) throws ODataJPAModelException {
		final JPAEntityType jpaEntity = sd.getEntitySetType(entitySetName);
		return jpaEntity.getAssociation(attributeIntName);
	}

	public JPAAttribute getJPAAttribute(final String entitySetName, final String attributeIntName) throws ODataJPAModelException {
		final JPAEntityType jpaEntity = sd.getEntitySetType(entitySetName);
		return jpaEntity.getAttribute(attributeIntName);
	}

	public EdmFunction getStoredProcedure(final EntityType<?> jpaEntityType, final String string) {
		if (jpaEntityType.getJavaType() instanceof AnnotatedElement) {
			final EdmFunctions jpaStoredProcedureList = ((AnnotatedElement) jpaEntityType.getJavaType())
					.getAnnotation(EdmFunctions.class);
			if (jpaStoredProcedureList != null) {
				for (final EdmFunction jpaStoredProcedure : jpaStoredProcedureList.value()) {
					if (jpaStoredProcedure.name().equals(string)) {
						return jpaStoredProcedure;
					}
				}
			}
		}
		return null;
	}

	public Attribute<?, ?> getAttribute(final ManagedType<?> et, final String attributeName) {
		for (final SingularAttribute<?, ?> attribute : et.getSingularAttributes()) {
			if (attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		return null;
	}

	public EmbeddableType<?> getEmbeddedableType(final String typeName) {
		for (final EmbeddableType<?> embeddableType : jpaMetamodel.getEmbeddables()) {
			if (embeddableType.getJavaType().getSimpleName().equals(typeName)) {
				return embeddableType;
			}
		}
		return null;
	}

	public Attribute<?, ?> getDeclaredAttribute(final ManagedType<?> et, final String attributeName) {
		for (final Attribute<?, ?> attribute : et.getDeclaredAttributes()) {
			if (attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		return null;
	}

}