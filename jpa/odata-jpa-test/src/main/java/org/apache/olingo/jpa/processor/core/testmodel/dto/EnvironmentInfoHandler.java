package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTOHandler;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

public class EnvironmentInfoHandler implements ODataDTOHandler<EnvironmentInfo> {

	private long extractTargetingId(final UriInfoResource requestedResource) throws RuntimeException {
		final List<UriResource> resPath = requestedResource.getUriResourceParts();
		if (resPath.isEmpty()) {
			return -1;
		}
		final UriResource targetResource = resPath.get(resPath.size() - 1);
		if (!UriResourceEntitySet.class.isInstance(targetResource)) {
			throw new UnsupportedOperationException("Unsupported request type: " + targetResource.getKind());
		}
		final UriResourceEntitySet entityResource = (UriResourceEntitySet) targetResource;
		if (entityResource.getKeyPredicates().isEmpty()) {
			// the default (expected) state
			return -1;
		}
		if (!"Id".equals(entityResource.getKeyPredicates().get(0).getName())) {
			throw new IllegalStateException("'Id' is not longer the DTO key attribute?!");
		}
		return Long.parseLong(entityResource.getKeyPredicates().get(0).getText());
	}

	@Override
	public Collection<EnvironmentInfo> read(final UriInfoResource requestedResource) throws RuntimeException {
		final long id = extractTargetingId(requestedResource);
		if (id != -1) {
			throw new IllegalStateException("Loading specific resoure with id "+id+" not supported");
		}
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
