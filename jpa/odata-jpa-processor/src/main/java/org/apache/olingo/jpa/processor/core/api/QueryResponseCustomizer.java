package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.jpa.processor.core.query.EntityQueryBuilder;
import org.apache.olingo.jpa.processor.impl.JPAStructureProcessor;

/**
 * Implementors of this interface may customize response of queries created by the {@link EntityQueryBuilder} and used
 * by the
 * {@link JPAStructureProcessor}. The customizer comes into effect only with this setup.</br>
 * The customizer instance can be registered in
 * {@link JPAODataServletHandler#modifyRequestContext(org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext)}
 * as injectable dependency:</br>
 * <code>requestContext.getDependencyInjector().registerDependencyMapping(QueryResponseCustomizer.class, &lt;customizer
 * instance&gt;);</code>
 */
public interface QueryResponseCustomizer {

  /**
   * There are several use cases to customize a query result before to send back to client:
   * <ul>
   * <li>Remove unwanted property values: only a value can be removed, the property key will be added by serializer
   * always, but then nullified.</li>
   * <li>Reduce the number of entities in result: This should not happen for normal access restrictions, use
   * {@link org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner DataAccessConditioner} instead.</li>
   * <li>Add additional properties to entities: additional properties, not declared via meta model, are only handled by
   * the JSON serializer (not for XML) and only if the JPA entity is marked as
   * {@link org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity#openType() open type}. A additional property must
   * have a {@link org.apache.olingo.commons.api.data.Property#setType(String) type} like
   * <code>EdmPrimitiveTypeKind.String.getFullQualifiedName()</code> for primitives or a already known type for complex
   * values. Entities (and also DTO's) cannot be used for dynamic properties. A complex type can be every
   * JPA @Embeddable or every other class annotated with
   * {@link org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType @ODataComplexType} and known to meta
   * model via DTO registration in {@link org.apache.olingo.jpa.processor.core.mapping.JPAAdapter#getDTOs()
   * JPAAdapter}. The complex type cannot have more dynamic properties, because it not markable as open type.</li>
   * <li>Add additional entities to collection: This should not happen as replacement for usage of
   * {@link org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO DTO}'s.</li>
   * </ul>
   * 
   * @param entityType The JPA entity type of entities in collection.
   * @param result The collection of entities to return.
   * @return A modified collection, a complete new one, the untouched parameter or <code>null</code> to use the given
   * result.
   */
  public EntityCollection customizeResult(Edm edm, Class<?> entityType, EntityCollection result);
}
