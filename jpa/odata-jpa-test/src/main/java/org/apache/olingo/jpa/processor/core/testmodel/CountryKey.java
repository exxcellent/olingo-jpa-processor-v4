package org.apache.olingo.jpa.processor.core.testmodel;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;

public class CountryKey implements Serializable {
	private static final long serialVersionUID = 229175464207091262L;

	/*
	 * Don't remove the @Id and @Column declaration. We know they are useless, but
	 * we have to check whether the JPA provider will handle such stupid
	 * declarations in a proper way.
	 */

	@Id
	@Column(name = "\"ISOCode\"")
	private String code;

	@Id
	@Column(name = "\"LanguageISO\"")
	private String language;

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
		final CountryKey other = (CountryKey) obj;
		if (code == null) {
			if (other.code != null) {
				return false;
			}
		} else if (!code.equals(other.code)) {
			return false;
		}
		if (language == null) {
			if (other.language != null) {
				return false;
			}
		} else if (!language.equals(other.language)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		return result;
	}
}
