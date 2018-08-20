# Bound actions
That are method calls bound to an entity class. On OData side are bound actions operations with the entity as first parameter. On Java side a bound action is a method called on the entity instance.
A bound action can be declared by:
* Annotate a method with `@EdmAction`
* That method must be located in a entity class (annotated with `@Entity`)

#Unbound actions
For a unbound action is dependency injection via method parameter injection supported.

* must fulfill the same requirements as a bound action
* must be a `public static` method

//TODO implement method parameter injection for bound and unbound actions
//TODO implemnt support to call bound + unbound actions on a DTO