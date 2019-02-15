package org.apache.olingo.jpa.metadata.core.edm.converter;

/**
 * A class that implements this interface can be used to convert entity
 * attribute state into oData representation and back again.
 *
 * @param <X> The type of the JPA entity attribute.
 * @param <Y> The (JAVA representation) type of the oData entity attribute. The
 *        JAVA type must be supported by the Olingo serialization as defined by
 *        {@link org.apache.olingo.commons.api.edm.EdmPrimitiveType#getDefaultType()}
 */
public interface ODataAttributeConverter<X, Y> {

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
	public X convertToJPA(Y oDataValue);
}
