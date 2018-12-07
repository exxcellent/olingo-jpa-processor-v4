package org.apache.olingo.jpa.processor.core.testmodel;

import java.io.Serializable;

public class AdministrativeDivisionKey implements Serializable {

	private static final long serialVersionUID = 5482165952249228988L;

	private String codePublisher;

	private String codeID;

	private String divisionCode;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((codeID == null) ? 0 : codeID.hashCode());
		result = prime * result + ((codePublisher == null) ? 0 : codePublisher.hashCode());
		result = prime * result + ((divisionCode == null) ? 0 : divisionCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AdministrativeDivisionKey other = (AdministrativeDivisionKey) obj;
		if (codeID == null) {
			if (other.codeID != null) {
				return false;
			}
		} else if (!codeID.equals(other.codeID)) {
			return false;
		}
		if (codePublisher == null) {
			if (other.codePublisher != null) {
				return false;
			}
		} else if (!codePublisher.equals(other.codePublisher)) {
			return false;
		}
		if (divisionCode == null) {
			if (other.divisionCode != null) {
				return false;
			}
		} else if (!divisionCode.equals(other.divisionCode)) {
			return false;
		}
		return true;
	}
}
