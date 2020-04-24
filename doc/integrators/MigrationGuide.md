[Overview](TableOfContent.md)

---
# 0.36.x &#8594; 0.37.x
## `org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler`:
* The customization method `protected void prepareRequestContext(final JPAODataRequestContext requestContext)` has a changed signature: `protected void prepareRequestContext(final ModifiableJPAODataRequestContext requestContext)` offering the same functionality as before.

# 0.34.x &#8594; 0.35.x
## Global and Request context
The formerly used `JPAODataSessionContextAccess` as context for global (runtime static) and request (dynamic) aspects was splitted into two separate context concepts: `JPAODataGlobalContext` and `JPAODataRequestContext`. The request context provides also the informations embedded into the global context. So normally you can simply replace `JPAODataSessionContextAccess` by `JPAODataRequestContext`. Both classes are not longer located in the `org.apache.olingo.jpa.processor.core.api` package, but in `org.apache.olingo.jpa.processor`.

## More refactorings
* org.apache.olingo.jpa.processor.DependencyInjector
* Some deprecated legacy processors were removed, the functionality of adapter should not be affected

## `org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler`:
* The customization method `protected Collection<Processor> collectProcessors(final HttpServletRequest request, final HttpServletResponse response, final EntityManager em)` to register own processors has now a changed signature: `protected Collection<Processor> collectProcessors(final JPAODataRequestContext requestContext)`
* The customization method `protected void prepareDependencyInjection(final DependencyInjector dpi)` was changed into a more generic customization method `protected void prepareRequestContext(final JPAODataRequestContext requestContext)` supporting custom dependency injection and more.