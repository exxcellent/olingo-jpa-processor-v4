package org.apache.olingo.jpa.metadata.core.edm.dto;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.server.api.uri.UriInfoResource;

/**
 * Annotation to mark a non JPA POJO as a OData entity.
 *
 * @author Ralf Zozmann
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface ODataDTO {

  /**
   * Dummy used as default/marker to make handler optional.
   *
   */
  static final class DEFAULT implements ODataDTOHandler<Object> {

    @Override
    public Collection<Object> read(final UriInfoResource requestedResource) throws RuntimeException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void write(final UriInfoResource requestedResource, final Object dto) throws RuntimeException {
      throw new UnsupportedOperationException();
    }
  };

  /**
   *
   * @return The class implementing the logic to handle the DTO.
   */
  Class<? extends ODataDTOHandler<?>> handler() default DEFAULT.class;

  /**
   * @see NamingStrategy#UpperCamelCase
   */
  NamingStrategy attributeNaming() default NamingStrategy.UpperCamelCase;

  /**
   * Define the name of the entity set for this DTO manually.
   */
  String edmEntitySetName() default "";
}
