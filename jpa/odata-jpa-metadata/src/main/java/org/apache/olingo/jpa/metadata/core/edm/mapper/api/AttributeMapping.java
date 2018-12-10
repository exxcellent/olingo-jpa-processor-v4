package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

/**
 * Enumeration for kind of mapping from JPA/Java attribute type to OData
 * representation.
 *
 * @author Ralf Zozmann
 *
 */
public enum AttributeMapping {
	/**
	 * Used for simple properties
	 * ({@link javax.persistence.metamodel.Attribute.PersistentAttributeType#BASIC
	 * BASIC}) and
	 * {@link javax.persistence.metamodel.Attribute.PersistentAttributeType#ELEMENT_COLLECTION
	 * collections} of simple properties.
	 */
	SIMPLE,

	/**
	 * Used for embedded complex property types
	 * ({@link javax.persistence.metamodel.Attribute.PersistentAttributeType#EMBEDDED @Embedded})
	 * and for
	 * {@link javax.persistence.metamodel.Attribute.PersistentAttributeType#ELEMENT_COLLECTION
	 * collections} of complex properties, but NOT for @EmbeddedId.
	 */
	AS_COMPLEX_TYPE,

	/**
	 * Used for composite key properties
	 * ({@link javax.persistence.metamodel.Attribute.PersistentAttributeType#EMBEDDED @EmbeddedId}).
	 */
	EMBEDDED_ID,

	/**
	 * Used for navigation properties
	 * ({@link javax.persistence.metamodel.Attribute.PersistentAttributeType#MANY_TO_MANY
	 * m:n},
	 * {@link javax.persistence.metamodel.Attribute.PersistentAttributeType#MANY_TO_ONE
	 * m:1},
	 * {@link javax.persistence.metamodel.Attribute.PersistentAttributeType#ONE_TO_MANY
	 * 1:n},
	 * {@link javax.persistence.metamodel.Attribute.PersistentAttributeType#ONE_TO_ONE
	 * 1:1}).
	 */
	RELATIONSHIP;
}
