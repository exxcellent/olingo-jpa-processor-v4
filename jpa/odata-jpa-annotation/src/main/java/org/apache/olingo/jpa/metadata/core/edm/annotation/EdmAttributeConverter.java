/**
 *
 */
package org.apache.olingo.jpa.metadata.core.edm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * Declare a converter to map from the JPA attribute datatype to an OData specific one. The converter will adapt the datatype visible in
 * OData metadata and convert between OData and JPA entity representations.
 *
 * @author Ralf Zozmann
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EdmAttributeConverter {

	/**
	 *
	 * @return The class implementing the attribute conversion.
	 */
	Class<? extends ODataAttributeConverter<?, ?>> value();
}
