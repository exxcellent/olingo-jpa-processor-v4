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
TBD