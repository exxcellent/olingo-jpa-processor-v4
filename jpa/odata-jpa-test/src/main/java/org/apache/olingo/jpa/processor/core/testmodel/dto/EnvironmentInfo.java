package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Id;

import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;

/**
 * Test POJO to realize a OData entity without JPA persistence.
 *
 * @author rzozmann
 *
 */
@ODataDTO(handler = EnvironmentInfoHandler.class)
public class EnvironmentInfo {

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
}
