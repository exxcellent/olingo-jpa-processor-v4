package org.apache.olingo.jpa.processor.core.testmodel.dto;

import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;

/**
 * Test POJO to realize a OData entity without JPA persistence.
 *
 * @author Ralf Zozmann
 *
 */
@ODataDTO
public class SystemRequirement {

	private final String requirementName;
	private final String requirementDescription;

	public SystemRequirement() {
		// default constructor for JPA
		this(null, null);
	}

	SystemRequirement(final String requirementName, final String requirementDescription) {
		this.requirementName = requirementName;
		this.requirementDescription = requirementDescription;
	}

	public String getRequirementName() {
		return requirementName;
	}

	public String getRequirementDescription() {
		return requirementDescription;
	}
}
