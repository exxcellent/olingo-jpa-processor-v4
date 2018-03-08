package org.apache.olingo.jpa.metadata.core.edm.converter;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;

/**
 * A class that implements this interface can be used to convert entity
 * attribute state into oData representation and back again.
 *
 * @param <X>
 *            The type of the JPA entity attribute.
 * @param <Y>
 *            The type of the oData entity attribute. The type must be supported
 *            by {@link org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind
 *            EdmPrimitiveTypeKind}.
 */
public interface ODataAttributeConverter<X, Y> {

	/**
	 *
	 * @return The oData type.
	 */
	public EdmPrimitiveTypeKind getODataType();

	/**
	 * Converts the value stored in the entity attribute into the data
	 * representation to be stored in the database.
	 *
	 * @param jpaValue
	 *            the entity attribute value to be converted
	 * @return the converted data to be stored in the oData entity
	 */
	public Y convertToOData(X jpaValue);

	/**
	 * @param oDataValue
	 *            the data from the oData entity attribute to be converted
	 * @return the converted value to be stored in the JPA entity attribute
	 */
	public X convertToJPAEntity(Y oDataValue);
}
