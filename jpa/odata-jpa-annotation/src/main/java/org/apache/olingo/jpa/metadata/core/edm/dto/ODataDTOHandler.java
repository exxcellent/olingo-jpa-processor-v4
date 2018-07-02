package org.apache.olingo.jpa.metadata.core.edm.dto;

import java.util.Collection;

import org.apache.olingo.server.api.uri.UriInfoResource;

/**
 * The handler must have a default constructor.
 *
 * @author Ralf Zozmann
 *
 * @param <T>
 *            The DTO class type.
 */
public interface ODataDTOHandler<T> {

	/**
	 *
	 * @return The loaded/read DTO instance(s).
	 */
	public Collection<T> read(UriInfoResource requestedResource) throws RuntimeException;

	/**
	 * Update/Store the given DTO. Normally the DTO is created via
	 * {@link #read(UriInfoResource)} before an update is possible, because the
	 * identifier should be managed by server side.
	 */
	public void write(UriInfoResource requestedResource, T dto) throws RuntimeException;

}
