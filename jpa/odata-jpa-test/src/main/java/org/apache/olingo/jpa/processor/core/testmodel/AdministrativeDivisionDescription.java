package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;

@Entity
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::AdministrativeDivisionDescription\"")
public class AdministrativeDivisionDescription {

	@EmbeddedId
	private AdministrativeDivisionDescriptionKey key;

	@EdmSearchable
	@Column(name = "\"Name\"", length = 100, updatable = false)
	private String name;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public AdministrativeDivisionDescriptionKey getKey() {
		return key;
	}

	@EdmAction()
	public void boundActionCheckLoadingOfEmbeddedId() {
		if (key == null) {
			throw new IllegalStateException("@EmbeddedId key not set");
		}
	}

	@EdmAction()
	public static void unboundActionCheckLoadingOfEmbeddedId(
			@EdmActionParameter(name = "parameter") final AdministrativeDivisionDescription parameter) {
		if (parameter.key == null) {
			throw new IllegalStateException("@EmbeddedId key not set");
		}
		if (parameter.key.getCodeID() == null || parameter.key.getCodePublisher() == null
				|| parameter.key.getDivisonCode() == null || parameter.key.getLanguage() == null) {
			throw new IllegalStateException("EmbeddedId attribute not set");
		}
	}

}
