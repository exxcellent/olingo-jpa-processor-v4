package org.apache.olingo.jpa.processor.core.testmodel;

import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream;
import org.apache.olingo.jpa.processor.core.testmodel.converter.jpa.JPAUrlConverter;
import org.apache.olingo.jpa.processor.core.testmodel.converter.odata.EdmUrlConverter;

@Entity(name = "OrganizationImage")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::OrganizationImage\"")
public class OrganizationImage implements BPImageIfc {
  @Id
  @Column(name = "\"ID\"")
  private String ID;

  @Column(name = "\"Image\"")
  @EdmMediaStream(contentTypeAttribute = "mimeType")
  private byte[] image;

  @EdmIgnore
  @Column(name = "\"MimeType\"")
  private String mimeType;

  @Column(name = "\"ThumbnailUrl\"", length = 250)
  @Convert(converter = JPAUrlConverter.class)
  @EdmAttributeConversion(odataType = EdmPrimitiveTypeKind.String, converter = EdmUrlConverter.class)
  private URL thumbnailUrl;

  @Override
  public String getID() {
    return ID;
  }

  void setID(final String iD) {
    ID = iD;
  }

  @Override
  public byte[] getImage() {
    return image;
  }

  public void setImage(final byte[] image) {
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
