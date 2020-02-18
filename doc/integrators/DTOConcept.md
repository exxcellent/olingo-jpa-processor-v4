[Overview](TableOfContent.md)

---
# 1. Usage of non persistent OData objects as Data Transfer Object
It is possible to define simple Java POJO's as OData entity to use that entities in the OData domain model in interaction with an client. Such an POJO is not known to the JPA framework, all functionality around instances of such an 'view' object is delegated to managing code in responsibility of the POJO designer.
There are several requirements that can be matched by such an POJO:
* implement manually controlled persistence aspects
* the architecture does not allow direct access to an database via JPA and a intermediate layer is required
* not JPA or database related sources are used to work with domain model entities

Manually controlled POJO's will be called DTO (data transfer object) in this document.

## 1.1. Define a DTO
There are some limitations to an DTO:
* Only native and simple data types are allowed, no complex types
* Navigation or any other relationship types to other entities are not allowed
* DTO's cannot be referenced from JPA entities
* The common JPA annotations to define an entity are not supported
* A DTO can define OData actions or functions. If only actions will be called then the _handler_ declaration may be omitted.
* Inheritance is not allowed for an DTO.
* The DTOmust have a public default constructor to create new instances.
* Properties must be defined as attributes, declarations from getter/setter methods are not supported.
* The DTO must be located in another package than the JPA model classes to target a separate name space.
* Allowed operations on a DTO are GET (read) and PUT (write), handled by the _handler_. 

Example:

```
@ODataDTO(handler=MyAddressFactory.class)
public class Address {
	@Id
	private String id;
	
	private String name;
	private String street;
	private String municipality;
	
	public String getStreet() {
		return street;
	}
	...
}

```

1. Mark the POJO class with the @ODataDTO annotation and declare the handler.
1. Define a attribute of supported type, a id/key attribute can be defined via the common JPA annotation @Id.

## 1.2. Use the DTO
At runtime the processor will detect calls to an DTO resource and delegate processing to the handler. 'Reading' allows the creation of a entity collection (via GET), saving is currently supported for single resource (via PUT).
A DTO instance is automatically transformed from/into a OData entity instance like a normal JPA entity. The DTO is accessible like other OData entities with a appropriate URI.

# 2. Dependency injection
For DTO's a limited support for dependency injection is available (see [JSR-330](https://jcp.org/en/jsr/detail?id=330) for annotations). Supported is the injection of some DTO call related context objects via field injection (using @Inject). Available types are listed in [Dependency Injection](DependencyInjection.md).

```
public static class MyAddressFactory implements ODataDTOHandler<Address> {

	@Inject
	private HttpServletRequest request;

	@Override
	public Collection<Address> read(final UriInfoResource requestedResource) throws RuntimeException {
		...
		String userName = request.getUserPrincipal().getName();
		...
	};

	@Override
	public void write(final UriInfoResource requestedResource, final Address dto) throws RuntimeException {
		...
	}

}

```