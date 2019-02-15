/**
 *
 */
package org.apache.olingo.jpa.metadata.core.edm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * Declare a conversion to map from the JPA attribute datatype to an OData
 * specific one. The converter will adapt the datatype visible in OData metadata
 * and convert between OData and JPA entity representations.
 *
 * @author Ralf Zozmann
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EdmAttributeConversion {

	/**
	 * Dummy used as default/marker triggering the built-in data type conversion.
	 *
	 */
	static final class DEFAULT implements ODataAttributeConverter<Object, Object> {
		@Override
		public Object convertToJPA(final Object oDataValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object convertToOData(final Object jpaValue) {
			throw new UnsupportedOperationException();
		}

	};

	/**
	 *
	 * @return The primitive type used for the JPA attribute on the OData side.
	 */
	public EdmPrimitiveTypeKind odataType();

	/**
	 * Maybe DEFAULT to use built-in data type conversion without explicit
	 * converter.
	 *
	 * @return The class implementing the attribute conversion.
	 */
	Class<? extends ODataAttributeConverter<?, ?>> converter() default DEFAULT.class;

}
