package org.apache.olingo.jpa.processor.core.query;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

public final class JPANavigationPropertyInfo {
	private final UriResourcePartTyped navigationResource;
	private final JPANavigationPath navigationPath;

	public JPANavigationPropertyInfo(final UriResourcePartTyped uriResourceNavigation,
			final JPANavigationPath associationPath) {
		super();
		this.navigationResource = uriResourceNavigation;
		this.navigationPath = associationPath;
		if (associationPath == null) {
			throw new IllegalArgumentException("Association path required");
		}
	}

	/**
	 *
	 * @return The {@link org.apache.olingo.server.api.uri.UriResourceNavigation
	 *         UriResourceNavigation} or
	 *         {@link org.apache.olingo.server.api.uri.UriResourceEntitySet
	 *         UriResourceEntitySet} identifying the resource triggering the
	 *         navigation.
	 */
	public UriResourcePartTyped getNavigationUriResource() {
		return navigationResource;
	}

	public JPANavigationPath getNavigationPath() {
		return navigationPath;
	}

}
