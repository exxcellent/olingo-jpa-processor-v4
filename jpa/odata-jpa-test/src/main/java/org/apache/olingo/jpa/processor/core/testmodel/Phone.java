package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Phone {

	@Column(name = "\"PhoneNumber\"", length = 128)
	private String phoneNumber;
}
