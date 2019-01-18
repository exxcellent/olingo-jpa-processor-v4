package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.security.ODataOperationAccess;
import org.apache.olingo.server.api.ODataApplicationException;

/**
 * Test POJO to realize a OData entity without JPA persistence.
 *
 * @author Ralf Zozmann
 *
 */
@ODataDTO(handler = EnvironmentInfoHandler.class)
public class EnvironmentInfo {

	@EdmIgnore
	private final Object ignoredSerializableField = new Serializable() {
		private static final long serialVersionUID = 1L;
	};

	private String javaVersion = null;

	@Id
	private long id = System.currentTimeMillis();

	private final Collection<String> envNames = new ArrayList<>();

	public EnvironmentInfo() {
		// default constructor for JPA
	}

	EnvironmentInfo(final String javaVersion, final Collection<String> envNames) {
		this.javaVersion = javaVersion;
		this.envNames.addAll(envNames);
	}

	public void setJavaVersion(final String jv) {
		this.javaVersion = jv;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getJavaVersion() {
		return javaVersion;
	}

	public Collection<String> getEnvNames() {
		return envNames;
	}

	/**
	 * Unbound oData action without specific name.
	 */
	@EdmAction
	public static void unboundVoidAction(@Inject final EntityManager em) {
		if (em == null) {
			throw new IllegalStateException("Entitymanager was not injected");
		}
	}

	@EdmAction
	@ODataOperationAccess(authenticationRequired = false)
	public static int actionWithNoSecurity() {
		return 42;
	}

	@EdmAction
	@ODataOperationAccess
	public static String actionWithOnlyAuthentication(@Inject final Principal user) {
		return user.getName();
	}

	@EdmAction
	@ODataOperationAccess(rolesAllowed = { "access" })
	public static void actionWithOnlyRole(@Inject final Principal user) {
		if (user == null) {
			throw new IllegalStateException("User was not injected or not authenticated");
		}
	}

	@EdmAction
	public static void throwODataApplicationException() throws ODataApplicationException {
		throw new ODataApplicationException("Proprietary status code 911 thrown", 911, Locale.getDefault());
	}

}
