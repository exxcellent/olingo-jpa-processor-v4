package org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream;

@Entity
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::PersonImage\"")
public class BusinessPartnerImage {

	@Id
	@Column(name = "\"PID\"")
	private String pID;

	@Column(name = "\"Image\"", columnDefinition = "blob")
	@EdmMediaStream(contentType = "image/png")
	private byte[] image;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE, orphanRemoval = true)
	@JoinColumn(name = "\"PID\"", insertable = false, updatable = false, nullable = true)
	private GenericBusinessPartner businessPartnerPerson;

}
