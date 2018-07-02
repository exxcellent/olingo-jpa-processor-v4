package org.apache.olingo.jpa.metadata.core.edm.dto;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a non JPA POJO as a OData entity.
 *
 * @author rzozmann
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ODataDTO {

	/**
	 *
	 * @return The class implementing the logic to handle the DTO.
	 */
	Class<? extends ODataDTOHandler<?>> handler();
}
