package org.apache.olingo.jpa.processor.core.testmodel;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ChangeInformation {

	// all values are controlled by annotations in parent classes
	@Column
	private String by;
	@Column
	private Timestamp at;

	public ChangeInformation() {
		super();
	}

	public ChangeInformation(String by, Timestamp at) {
		super();
		this.by = by;
		this.at = at;
	}

	public Timestamp getAt() {
		return at;
	}

	public String getBy() {
		return by;
	}

}
