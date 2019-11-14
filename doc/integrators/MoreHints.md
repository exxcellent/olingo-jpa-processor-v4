[Overview](TableOfContent.md)

---
# 1. Annotations affecting the meta model
Additional to the annotations provided by the JPA standard some more annotations can be used to enrich the meta model or replace other annotations:
* *@javax.validation.constraints.NotNull*: Mark an attribute or operation parameter as not *nullable*
* *@javax.validation.constraints.Size*: Can be used to override the *length* attribute of the *@javax.persistence.Column* annotation
