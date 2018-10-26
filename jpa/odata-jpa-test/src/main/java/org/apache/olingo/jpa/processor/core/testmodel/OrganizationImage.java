package org.apache.olingo.jpa.processor.core.testmodel;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream;
import org.apache.olingo.jpa.processor.core.testmodel.converter.odata.EdmUrlConverter;

@Entity(name = "OrganizationImage")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::OrganizationImage\"")
public class OrganizationImage {
	@Id
	@Column(name = "\"ID\"")
	private String ID;

	@Column(name = "\"Image\"")
	@EdmMediaStream(contentTypeAttribute = "mimeType")
	private byte[] image;

	@EdmIgnore
	@Column(name = "\"MimeType\"")
	private String mimeType;

	@Column(name = "\"ThumbnailUrl\"", length = 4000)
	@EdmAttributeConverter(EdmUrlConverter.class)
	private URL thumbnailUrl;

	String getID() {
		return ID;
	}

	void setID(final String iD) {
		ID = iD;
	}

	byte[] getImage() {
		return image;
	}

	void setImage(final byte[] image) {
		this.image = image;
	}

	String getMimeType() {
		return mimeType;
	}

	void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public URL getThumbnailUrl() {
		return thumbnailUrl;
	}
}
