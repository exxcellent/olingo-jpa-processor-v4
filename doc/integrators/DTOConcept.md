# Usage of non persistent OData objects as Data Transfer Object
It is possible to define simple Java POJO's as OData entity to use that entities in the OData domain model in interaction with an client. Such an POJO is not known to the JPA framework, all functionality around instances of such an 'view' object is delegated to managing code in responsibility of the POJO designer.
There are several requirements that can be matched by such an POJO:
* implement manually controlled persistence aspects
* the architecture does not allow direct access to an database via JPA and a intermediate layer is required
* not JPA or database related sources are used to work with domain model entities

Manually controlled POJO's will be called DTO (data transfer object) in this document.

## Define a DTO
There are some limitations to an DTO:
* Only native and simple data types are allowed, no complex types
* Navigation or any other relationship types to other entities are not allowed
* DTO's cannot be referenced from JPA entities
* The common JPA annotations to define an entity are not supported
* A DTO can't define OData actions or functions.
* Inheritance is not allowed for an DTO.
* The DTOmust have a public default constructor to create new instances.
* Properties must be defined as attributes, declarations from getter/setter methods are not supported.
* The DTO must be located in another package than the JPA model classes to target a separate name space.
* Allowed operations on a DTO are GET (read) and PUT (write)

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

1. Mark the PJO class with the @ODataDTO annotation and declare the handler.
1. Define a attribute of supported type, a id/key attribute can be defined via the common JPA annotation @Id.

## Use the DTO
At runtime the processor will detect calls to a DTO resource and delegates processing to the handler. Reading allows the creation of a entity collection (via GET), saving is currently supported for single resource (via PUT).
A DTO instance is automatically transformed from/into a OData entity instance like a normal JPA entity. The DTO is accessible like other OData entities with a appropriate URI.

## Dependency injection
For DTO's a limited support for dependency injection is available (see [JSR-330](https://jcp.org/en/jsr/detail?id=330) for annotations). Currently only single objects without ambiguous type can be handled.
Supported is the injection of some DTO call related context objects via field injection (using @Inject). Automatic available are:
* HttpServletRequest and HttpServletResponse (if called via `JPAODataGetHandler` in a servlet)
* JPAAdapter and EntityManager (covering the current transaction)
* JPAEdmProvider
	
The injection support can be extended by custom injections (see example servlet for details).

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