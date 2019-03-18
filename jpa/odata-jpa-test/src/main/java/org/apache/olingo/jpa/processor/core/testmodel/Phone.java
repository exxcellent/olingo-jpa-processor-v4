package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

@Embeddable
public class Phone {

	@Column(name = "\"PhoneNumber\"", length = 128)
	private String phoneNumber;

	@Size(max = 64)
	@Column(name = "\"PreSelection\"", length = 64)
	private String preSelection;
}
