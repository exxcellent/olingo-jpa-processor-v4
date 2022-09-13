package org.apache.olingo.jpa.processor.core.testmodel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;

@Entity(name = "EntityWithSecondaryTableAndEmbedded")
@Table(schema = "\"OLINGO\"", name = "\"org.apache.olingo.jpa::BusinessPartner\"")
//Eclipselink+Hibernate cannot handle qualified table names for @SecondaryTable
@SecondaryTable(schema = "\"OLINGO\"", name = "SECONDARYTABLEEXAMPLEWITHSIMPLENAME",
pkJoinColumns = {
    @PrimaryKeyJoinColumn(name = "\"ID\"") })
@ODataEntity(edmEntitySetName = "EntityWithSecondaryTableAndEmbeddedSet", attributeNaming = NamingStrategy.AsIs)
public class EntityWithSecondaryTableAndEmbedded {
  @Id
  @Column(name = "\"ID\"")
  protected String ID;

  @Column(name = "\"NameLine1\"")
  private String firstName;

  @Column(name = "\"NameLine2\"")
  private String lastName;

  // single attribute targeting another table
  @Column(table = "SECONDARYTABLEEXAMPLEWITHSIMPLENAME", name = "\"DATA\"")
  private String data;

  // use @Embedded + @AttributeOverride targeting values in another table
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "created.by", column = @Column(
        table = "SECONDARYTABLEEXAMPLEWITHSIMPLENAME",
        name = "\"CreatedBy\"", insertable = false, updatable = false)),
    @AttributeOverride(name = "created.at", column = @Column(
        table = "SECONDARYTABLEEXAMPLEWITHSIMPLENAME",
        name = "\"CreatedAt\"", insertable = false, updatable = false)),
    @AttributeOverride(name = "updated.by", column = @Column(
        table = "SECONDARYTABLEEXAMPLEWITHSIMPLENAME",
        name = "\"UpdatedBy\"", insertable = false, updatable = false)),
    @AttributeOverride(name = "updated.at", column = @Column(
        table = "SECONDARYTABLEEXAMPLEWITHSIMPLENAME",
        name = "\"UpdatedAt\"", insertable = false, updatable = false)) })
  private final AdministrativeInformationSearchable editInformation = new AdministrativeInformationSearchable();


}
