# Table of content
1. [Integrate](GetStarted.md) OData-JPA-Adapter into project  
1. Start OData-JPA-Adapter as [standalone servlet](AsWar.md)  
    * [Servlet security](ServletSecurity.md)
    * [Dependency Injection](DependencyInjection.md)
1. Extend the OData-JPA-Adapter  
    * Use [DTO's](DTOConcept.md) instead of JPA mapped entities  
    ** OpenTypes and [Map]'s(DTOConcept.md#OpenTypes)
    * Call Java methods as OData [action](Actions.md)  
    * Upload files via [Multipart/form-data](Actions.md#UploadFilesViaMultipartFormData)  
1. Detailed informations  
    * More [hints](MoreHints.md)  
    * Use [Excel export](MoreHints.md#ExcelExport)  
    * [EclipseLink, Hibernate](MoreHints.md#Weaving) and weaving (byte code enhancement)  
    * [Migration guide](MigrationGuide.md)  
    * [Annotation overview](Annotations.md)

---
# Known issues
**Integration with JPA providers**  
Recommend is Eclipselink, because no major issues are known!

_Hibernate_
* Hibernate has a broken meta model, so a few workarounds are required:
    * @IdClass with multiple key attributes are not supported
    * Switching from one persistence unit to another in a dynamic way will result in a invalid meta model
* It cannot compare a java.lang.Short as java.lang.Number, so OData filter conditions like 'population lt 6000' will not work
* Hibernate build a default join column name (as in @JoinColumn#name() described) always with quotes as part of the column name
* The handling for columns declared without quoting and the default join column name derivation is not working (adaption of SQL table model by developer required as workaround)
* Weaving (byte code enhancement) seems to work only in very specific scenarios and must assumed as not usable.
