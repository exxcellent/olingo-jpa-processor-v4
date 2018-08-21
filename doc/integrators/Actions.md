# Bound actions
That are method calls bound to an entity class. On OData side are bound actions operations with the entity as first parameter. On Java side a bound action is a method called on the entity instance. Dependency injection is supported for field and method parameter.

#Unbound actions
A java method is marked as unbound if the method is declared as `public static`. For a unbound action is dependency injection via method parameter injection supported.

#Declare actions
A action can be declared by:
* Annotate a method with `@EdmAction`
* That method must be located in a entity class (annotated with `@Entity` or `@ODataDTO`)
* Method parameters must be annotated with `@EdmActionParameter` for OData related parameters or with `@Inject` for server side injected parameters, ignored by OData metamodel.

As `@Inject` annotation can be used `javax.inject.Inject` or better `org.apache.olingo.jpa.cdi.Inject`
