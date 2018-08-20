package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;

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

}
