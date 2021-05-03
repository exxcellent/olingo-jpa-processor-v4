[Overview](TableOfContent.md)

---
# Annotations overview for Java modelling
Annotation | Usage
---------- | -----
org.apache.olingo.jpa.cdi.Inject, javax.inject.Inject | Mark an field or action parameter as injected by OData-JPA-Adapter runtime (see [Dependency Injection](DependencyInjection.md))
org.apache.olingo.jpa.security.AccessDefinition, org.apache.olingo.jpa.security.ODataEntityAccess, org.apache.olingo.jpa.security.ODataOperationAccess | Used to configure the annotation based security (see [Servlet security](ServletSecurity.md))
org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity | Optional entity to configure a JPA entity
org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO | Annotation to mark an Java POJO as DTO entity.
org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType | Annotation to mark an Java POJO as OData complex type or configure an javax.persistence.Embeddable.
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable | Mark an JPA field as searchable by OData
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream | Mark an JPA field as streamable content
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore | Mark an JPA entity or JPA/DTO field as ignored in OData meta model
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmGeospatial | Annotation to provide SRID value for JPA/DTO field.
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunctions, org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunction, org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunctionParameter | Annotations to declare an SQL function for OData
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion | Annotation to define a more specific way to convert values of an JPA field into an OData representation
org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction, org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter, org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionResult | Annotations to configure a Java method as OData action
javax.persistence.Transient | Transient fields in an DTO will be ignored
javax.persistence.Version | A JPA field with such an annotation will be handled as ETag in OData for optimistic concurrency control
javax.validation.constraints.NotNull | Mark an OData action parameter or result type as not nullable
javax.validation.constraints.Size | Used for DTO and JPA fields to define the maximum length of OData property

There are more annotations for javax.persistence.* respected by the OData-JPA-Adapter, but without further meaning..