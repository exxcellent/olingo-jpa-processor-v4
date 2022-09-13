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
	 * ({@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#BASIC
	 * BASIC}) and
	 * {@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#ELEMENT_COLLECTION
	 * collections} of simple properties.
	 */
	SIMPLE,

	/**
	 * Used for embedded complex property types
	 * ({@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#EMBEDDED @Embedded})
	 * and for
	 * {@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#ELEMENT_COLLECTION
	 * collections} of complex properties, but NOT for @EmbeddedId.
	 */
	AS_COMPLEX_TYPE,

	/**
	 * Used for composite key properties
	 * ({@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#EMBEDDED @EmbeddedId}).
	 */
	EMBEDDED_ID,

	/**
	 * Used for navigation properties
	 * ({@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#MANY_TO_MANY
	 * m:n},
	 * {@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#MANY_TO_ONE
	 * m:1},
	 * {@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#ONE_TO_MANY
	 * 1:n},
	 * {@link jakarta.persistence.metamodel.Attribute.PersistentAttributeType#ONE_TO_ONE
	 * 1:1}).
	 */
	RELATIONSHIP;
}
