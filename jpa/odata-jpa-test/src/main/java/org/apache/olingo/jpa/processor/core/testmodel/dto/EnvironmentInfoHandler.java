package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTOHandler;
import org.apache.olingo.server.api.uri.UriInfoResource;

public class EnvironmentInfoHandler implements ODataDTOHandler<EnvironmentInfo> {

	@Override
	public Collection<EnvironmentInfo> read(final UriInfoResource requestedResource) throws RuntimeException {
		final Collection<String> propNames = System.getProperties().keySet().stream().map(Object::toString)
				.collect(Collectors.toList());
		final EnvironmentInfo info = new EnvironmentInfo(System.getProperty("java.version"), propNames);
		return Collections.singleton(info);
	};

	@Override
	public void write(final UriInfoResource requestedResource, final EnvironmentInfo dto)
			throws RuntimeException {
		if (dto == null) {
			throw new IllegalStateException("Existing DTO a sparameter expected");
		}
	}
}
