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
 * Declare a converter for the datatype of attribute for OData.
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
