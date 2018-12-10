package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAAttribute extends JPAElement, JPATypedElement {

	/**
	 *
	 * @return The type of the attribute represented by the intermediate api.
	 */
	public JPAStructuredType getStructuredType();

	/**
	 *
	 * @return The wrapper to encapsulate reading/writing property values.
	 */
	public JPAAttributeAccessor getAttributeAccessor();

	/**
	 * The mapping is used to convert between JPA and OData representation:
	 * <ul>
	 * <li>Build correct SQL column selection queries for database</li>
	 * <li>Convert JPA entities in memory into OData entities and reverse</li>
	 * </ul>
	 */
	public AttributeMapping getAttributeMapping();

	/**
	 *
	 * @return TRUE if the type of the property is a complex type, having embedded
	 *         properties.
	 */
	public boolean isComplex();

	public boolean isKey();

	public boolean isAssociation();

	public boolean isSearchable();

	public boolean isCollection();

	public boolean ignore();

	/**
	 *
	 * @return TRUE if the property/attribute is of any JAVA simple type (not
	 *         {@link #isComplex()} and not {@link #isAssociation()}), maybe in a
	 *         collection.
	 */
	public boolean isPrimitive();

	public CsdlAbstractEdmItem getProperty() throws ODataJPAModelException;

}
