package org.apache.olingo.jpa.processor.core.testmodel;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream;

@Entity(name = "PersonImage")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::PersonImage\"")
public class PersonImage {
	@Id
	@Column(name = "\"PID\"")
	private String pID;

	@Column(name = "\"Image\"")
	@EdmMediaStream(contentType = "image/png")
	private byte[] image;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "created.by", column = @Column(name = "\"CreatedBy\"", nullable = false)),
		@AttributeOverride(name = "created.at", column = @Column(name = "\"CreatedAt\"", insertable = false)),
		@AttributeOverride(name = "updated.by", column = @Column(name = "\"UpdatedBy\"")),
		@AttributeOverride(name = "updated.at", column = @Column(name = "\"UpdatedAt\"")) })
	private final AdministrativeInformation administrativeInformation = new AdministrativeInformation();

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE, orphanRemoval = true)
	// Do not set 'referencedColumnName' in @JoinColumn! We want to test auto
	// detect
	@JoinColumn(name = "\"NOT_MAPPED_PID\"", insertable = false, updatable = false, nullable = true)
	private Person personReferenceWithoutMappedAttribute;

	// Workaround to have a selection column for $expand queries without JOINing the
	// complete JPA entity (Person)
	@EdmIgnore
	@Column(name = "\"NOT_MAPPED_PID\"")
	private String notMappedPid;

	// force usage of (default id name pattern) as join column, because no
	// 'mappedBy' or @JoinColumn is given
	@OneToOne
	private Person personWithDefaultIdMapping;

	String getID() {
		return pID;
	}

	void setID(final String iD) {
		pID = iD;
	}

	byte[] getImage() {
		return image;
	}

	void setImage(final byte[] image) {
		this.image = image;
	}

}
