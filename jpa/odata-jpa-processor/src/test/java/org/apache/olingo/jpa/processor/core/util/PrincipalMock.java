package org.apache.olingo.jpa.processor.core.util;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class PrincipalMock implements Principal {

	private final String user;
	private final Collection<String> roles;

	public PrincipalMock(final String user) {
		this(user, null);
	}

	public PrincipalMock(final String user, final String[] roles) {
		this.user = user;
		if (roles == null || roles.length == 0) {
			this.roles = Collections.emptyList();
		} else {
			this.roles = Arrays.asList(roles);
		}
	}

	@Override
	public String getName() {
		return user;
	}

	public boolean isUserInRole(final String role) {
		return roles.contains(role);
	}
}
