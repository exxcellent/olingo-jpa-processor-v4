[Overview](TableOfContent.md)

---
# Dependency injection
For DTO handlers and action methods there is a limited support for injection runtime values as defined in [JSR-330](https://jcp.org/en/jsr/detail?id=330). Currently only single objects without ambiguous type can be handled. Automatically available are (if called via `JPAODataGetHandler` in a servlet):
* javax.servlet.http.HttpServletRequest and javax.servlet.http.HttpServletResponse
* org.apache.olingo.jpa.processor.core.mapping.JPAAdapter and javax.persistence.EntityManager: covering the current transaction
* org.apache.olingo.jpa.metadata.api.JPAEdmProvider
* java.security.Principal: after authentication via `SecurityInceptor`

## Register values for later injection	
The injection support can be extended by custom injections (see example servlet for details):

```
final JPAODataServletHandler handler = new JPAODataServletHandler(mappingAdapter) {
	@Override
	protected void prepareRequestContext(final JPAODataRequestContext requestContext) {
		super.prepareRequestContext(requestContext);
		// example for custom dependency injection
		requestContext.getDependencyInjector().registerDependencyMapping(String.class, getServletName());
	}
};

```

or via

```
...
handler.getJPAODataContext().getDependencyInjector().registerDependencyMapping(String.class, getServletName());
```

## Got values injected
As `@Inject` annotation can be used `javax.inject.Inject` or better `org.apache.olingo.jpa.cdi.Inject`. Limitations are:
* `org.apache.olingo.jpa.cdi.Inject` and `javax.inject.Inject` for fields
* `org.apache.olingo.jpa.cdi.Inject` for method parameters