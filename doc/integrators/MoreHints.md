[Overview](TableOfContent.md)

---
# 1. External annotations affecting the meta model
Additional to the annotations provided by the JPA standard some more annotations can be used to enrich the meta model or replace other annotations:
* *@javax.validation.constraints.NotNull*: Mark an attribute or operation parameter as not *nullable*
* *@javax.validation.constraints.Size*: Can be used to override the *length* attribute of the *@javax.persistence.Column* annotation

# 2. Configure the naming of attributes and entity sets in the OData meta model
The default naming strategy for attributes in the meta model (for entities, DTO's and complex types) is a upper case first character. That means an attribute with the name `iD` will be named as `ID` in the meta model. This behaviour has historical reasons and can be changed on a  _per entity_  base.
Entities can have the annotation `@ODataEntity`, DTO's the annotation `@ODataDTO` and complex types (Embeddables) the annotation `@ODataComplexType`. Every annotation allows a setting of the  _attributeNaming_  . The default is `UpperCamelCase`, but `AsIs` will provide the attribute name in JPA without any adaption as same in the OData model.
The naming strategy must be set for every single entity, DTO or complex type separate. Also abstract super classes must have such an annotation to affect the attributes of that class.
For entities and DTO's the same annotations can be used to define a *Entity Set* name for the entity other than the generated default one.

# <a id="ExcelExport"></a>3. Enable Excel sheet export for read entities
The OData-JPA-Adapter comes with an add-on to produce Excel sheets (mime type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) instead of JSON for read entity sets.
In the frontend simply the `Accept` header on the request must be changed to the mime type above.
In the backend some more configuration is required...
* Maven needs one more dependency:

```
	<dependency>
		<groupId>org.apache.olingo.jpa</groupId>
		<artifactId>odata-jpa-processor-excelexport</artifactId>
		<version>...</version>
	</dependency>

```
* The servlet must be aware of that new available transformation:

```java
	JPAODataServletHandler handler = ...;
	// register Excel as output format
    handler.activateCustomResponseTransformation(
        QueryEntityResult2ExcelODataResponseContentTransformation.DEFAULT_DECLARATION,
        QueryEntityResult2ExcelODataResponseContentTransformation.class,
        QueryEntityResult2ExcelODataResponseContentTransformation.CONTENTTYPE_EXCEL,
        RepresentationType.COLLECTION_ENTITY);
```
The `Transformation` has a declaration part describing what the transformation needs to run and what it can transform (types of input). The registration part will activate that transformation for an specific subset of that declared capabilities (normally a specific mime type/content type and a representation type).
To control the excel export in more details a `Configuration` can be injected into the context like this way:

```java
	import org.apache.olingo.jpa.processor.transformation.excel.Configuration ;
	
	Configuration configuration = ...;
	
	protected JPAODataServletHandler createServletHandler() throws ODataException {
		return new JPAODataServletHandler(persistenceAdapter) {
			@Override
			protected void prepareRequestContext(final JPAODataRequestContext requestContext) {
				super.prepareRequestContext(requestContext);
				requestContext.getDependencyInjector().registerDependencyMapping(Configuration.class, configuration);
			}
		};
	}

```
The configuration allows:
* Define order of columns
* Suppress columns
* Define data format for columns/cells
* ...
For more details see also into the test classes of that add-on.

# <a id="Weaving"></a>4. EclipseLink, Hibernate and weaving (byte code enhancement)...
Short answer: **Weaving is not necessary!**

Long story: Aside from change tracking or fetch groups weaving is normally used to manage loading of relationships (via lazy or eager settings). But the OData standard does delegate the control over handling of relationships to the client using the $expand capability. On the other side is weaving implemented as byte code enhancement making the entity model less transparent and the access per reflection more complex. The access to fields (attributes/properties) is replaced by synthetic get/set methods having concepts like 'value holder'. More problematic is the fact, that annotations for fields are not longer accessible, because a method (for read) without these annotations is used for property access. As a effect the meta model of the OData-JPA-Adapter may become incomplete resulting in a unexpected behaviour.  
Currently the OData-JPA-Adapter works with a byte code enhanced JPA entity model (for EclipseLink), but there is a risk for uncovered scenarios. So only for an already designed legacy model where the OData interface is only an additional functionality leave weaving still alive.  
With Hibernate byte code enhancement produce problems with every configuration (cannot enhance final fields at build time or invalid queries at runtime), so byte code enhancement for Hibernate seems to be complete broken.

# Hints to modelling entities
* **Avoid circular dependencies**</br>
  This can be a problem for JPA also, but normally it's simply a design issue. The JPA model will be exposed as OData, normally using JSON as representation. JSON represents data as an tree without circular dependencies. That means OData cannot 'reference' another existing entity, it must embed the complete entity content as nested structure. In a circular graph you get an endless recursion. The OData-JPA-Adapter will manage that, but the behaviour may be unexpected for you.
    * The OData-JPA-Adapter will never process the same instance again.
* **Use unidirectional relationships**</br>
  Another more concrete aspect of the bullet point above: bidirectional relationships are the smallest possible circular dependency.
    * The OData-JPA-Adapter will set the backlink while processing.
* **Be careful with shared/reused entities in relationsships**</br>
  If you have 1:n or m:n relationships and the same target entity (instance) is referenced from different source entities then the representation in OData and JPA is not bijective.
    * The OData-JPA-Adapter will enforce that all the redundant/multiple representations on OData side are identical.
    * The OData-JPA-Adapter will merge multiple OData representations of the same entity into one instance on JPA side.
* **Define bound actions not for an abstract entity class**</br>
  Calling bound action means that the entity will be loaded from database. JPA provider like Hibernate cannot (EclipseLink will do) instance/load data for abstract entity classes.
    * The OData-JPA-Adapter will create a entity proxy for abstract classes to work on it.
