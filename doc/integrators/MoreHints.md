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

```
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

```
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