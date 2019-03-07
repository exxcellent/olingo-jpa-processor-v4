[Overview](TableOfContent.md)

---
# 1. Container managed security
You can annotate the servlet with `@ServletSecurity` to define global roles for GET, POST and other request types. This is enough to separate users by read and write access.
# 2. Self managed security
Often the developer has to separate the access to different types of data (resources) and not every user may call all the OData actions...
## 2.1. Annotation based security for servlets
As default the `JPAODataServletHandler` will use the `AnnotationBasedSecurityInceptor` to check access rights for the resource-based-security. The security inceptor can be exchanged by another one or disabled via `handler.setSecurityInceptor(...);` (see example servlet).
The annotation based security inceptor will look for the presence of proper annotations at resource and (action) method level. Without annotation any security check is suppressed; means no limitation in access to the resource/action is in effect (aka no security).
### 2.1.1. resource security
To activate security checks for an specific resource (entity) simply place the annotation `@ODataEntityAccess` on a JPA entity:

```
@Entity
@ODataEntityAccess({ @AccessDefinition(method = HttpMethod.GET) })
public class Address {	
	...
}

```

For every allowed HTTP method an `@AccessDefinition` entry is required. With that basic definition as in the example above a call may read the resource (GET method) if the user is authenticated (the AUTHORIZATION header in request must be valid), but no authorization is required, so the user has access with login, but without any access role.

Extend the list of `@AccessDefinition`'s with all HTTP methods you need (PUT, DELETE, ...).

### 2.1.2. action security
The security for OData actions (implemented as Java methods) is similar to the resource security, but have to use the annotation `@ODataOperationAccess`.

```
@Entity
public class Address {
	...
	
	@EdmAction
	@ODataOperationAccess(rolesAllowed = { "access" })
	public static void action() {
		...
	}
```

For bound actions without annotation also the parent resource will be inspected to determine the effective access right: the entity is checked for a `@ODataEntityAccess` annotation with the HTTP method of action (normally POST).