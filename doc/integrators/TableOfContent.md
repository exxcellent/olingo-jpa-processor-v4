# Table of conent
1. [Integrate](GetStarted.md) OData-JPA bridge into project  
1. Start OData-JPA bridge as [standalone servlet](AsWar.md)  
    * [Servlet security](ServletSecurity.md)
1. Extend the OData-JPA bridge  
    * Use [DTO's](DTOConcept.md) instead of JPA mapped entities  
    * Call Java methods as OData [action](Actions.md)  

---
# Known issues
**Integration with JPA providers**  
Recommend is Eclipselink, because no issues are known!

_EclipseLink_
* Weaving is currently not supported the OData-JPA bridge

_Hibernate_
* Hibernate has a broken meta model, so a few workarounds are required:
    * @IdClass with multiple key attributes are not supported
    * Switching from one persistence unit to another in a dynamic way will result in a invalid meta model
* It cannot compare a java.lang.Short as java.lang.Number, so OData filter conditions like 'population lt 6000' will not work
* Hibernate build a default join column name (as in @JoinColumn#name() described) always with quotes as part of the column name
* The handling for columns declared without quoting and the default join column name derivation is not working (adaption of SQL table model by developer required as workaround)