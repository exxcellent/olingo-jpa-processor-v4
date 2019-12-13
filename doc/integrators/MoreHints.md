[Overview](TableOfContent.md)

---
# 1. External annotations affecting the meta model
Additional to the annotations provided by the JPA standard some more annotations can be used to enrich the meta model or replace other annotations:
* *@javax.validation.constraints.NotNull*: Mark an attribute or operation parameter as not *nullable*
* *@javax.validation.constraints.Size*: Can be used to override the *length* attribute of the *@javax.persistence.Column* annotation

# 2. Configure the naming of attributes and entity sets in the OData meta model
The default naming strategy for attributes in the meta model (for entities, DTO's and complex types) is a upper case first character. That means an attribute with the name `iD` will be named as `ID` in the meta model. This behaviour has historical reasons and can be changed on a _per entity_ base.
Entities can have the annotation `@ODataEntity`, DTO's the annotation `@ODataDTO` and complex types (Embeddables) the annotation `@ODataComplexType`. Every annotation allows a setting of the _attributeNaming_. The default is `UpperCamelCase`, but `AsIs` will provide the attribute name in JPA without any adaption as same in the OData model.
The naming strategy must be set for every single entity, DTO or complex type separate. Also abstract super classes must have such an annotation to affect the attributes of that class.
For entities and DTO's the same annotations can be used to define a *Entity Set* name for the entity other than the generated default one.