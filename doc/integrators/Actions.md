[Overview](TableOfContent.md)

---
# Bound actions
That are method calls bound to an entity class. On OData side are bound actions operations with the entity as first parameter. On Java side a bound action is a method called on the entity instance. Dependency injection is supported for field and method parameter.

# Unbound actions
A java method is marked as unbound if the method is declared as `public static`. For a unbound action is dependency injection only via method parameter injection supported.

# Declare actions
A action can be declared by:
* Annotate a method with `@EdmAction`
* That method must be located in a entity class (annotated with `@Entity` or `@ODataDTO`)
* Method parameters must be annotated with `@EdmActionParameter` for OData related parameters or with `@Inject` for server side injected parameters (see [Dependency Injection](DependencyInjection.md)), ignored by OData metamodel.

# Upload files via Multipart/form-data
Most web-frameworks have functionality to upload files via a *multipart/form-data* mechanism to an backend. Such a behaviour at backend side is not supported the OData standard! But the OData-JPA-Adapter does support *multipart/form-data* requests in a limited way. To receive multi part request data you need to have an OData action with a special method signature. The action may be bound or unbound. Example:

```
@EdmAction
public static void uploadFile(@EdmActionParameter(name = "file") final java.io.InputStream stream,
                              @EdmActionParameter(name = "filename") final String fileName)
{
	...
} 
```
Every action parameter with it's name must match the *Content-Disposition* entry parameter 'name' in the form-data part. Such a name is mapped to the method parameter of the OData action with same name. That means also: you cannot upload a generic collection of files, you can upload (send) a number of *form-data* entries matching the number of action parameters.

Limitations:
The 'filename' of an *Content-Disposition* entry for binary data is not available as part of the specific parameter types. But the 'filename' may provided as separate/additional *form-data* entry represented as own action parameter (see separate action parameter in example method signature)...
Currently only the following data types are provided for *multipart/form-data* entries: 
* java.io.InputStream: for any binary data
* java.lang.String: For all data convertible into strings

**Attention**: Binary data are buffered internally as byte array, so this API is not usable for very large files. For such a use case use the standard conform OData streaming functionality (via media entity)