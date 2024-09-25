[Overview](TableOfContent.md)

---
# 1.0.0/2.0.0 &#8594; 1.1.0/2.1.0
## Namespace for maven artifacts changed
The _groupId_ for all maven artifacts has changed from `org.apache.olingo.jpa` to `de.exxcellent.odata.jpa`. So the dependencies to use the library has to be adaptedlike that example:
<table class="conversions-table">
	<tr valign="top" align="center"><th>Old</th><th>&nbsp;</th><th>New</th></tr>
	<tr valign="top"><td>
	        &lt;dependency&gt;</br>
			  &nbsp;&nbsp;&lt;groupId&gt;<b>org.apache.olingo.jpa</b>&lt;/groupId&gt;</br>
			  &nbsp;&nbsp;&lt;artifactId&gt;odata-jpa-annotation&lt;/artifactId&gt;</br>
			  &nbsp;&nbsp;&lt;version&gt;...&lt;/version&gt;</br>
		    &lt;/dependency&gt;
	   </td><td valign="middle">&#8594;</td><td>
	        &lt;dependency&gt;</br>
			  &nbsp;&nbsp;&lt;groupId&gt;<b>de.exxcellent.odata.jpa</b>&lt;/groupId&gt;</br>
			  &nbsp;&nbsp;&lt;artifactId&gt;odata-jpa-annotation&lt;/artifactId&gt;</br>
			  &nbsp;&nbsp;&lt;version&gt;...&lt;/version&gt;</br>
		    &lt;/dependency&gt;	   
	   </td></tr>
</table>

## Breaking change for `org.apache.olingo.jpa.processor.core.api.QueryCustomizer`
The `QueryCustomizer` introduced with 1.0.0/2.0.0 has a API change and must be adapted.

# 0.36.x &#8594; 0.37.x
## `...jpa.processor.core.api.JPAODataServletHandler`:
* The customization method `protected void prepareRequestContext(final JPAODataRequestContext requestContext)` has a changed signature: `protected void prepareRequestContext(final ModifiableJPAODataRequestContext requestContext)` offering the same functionality as before.

# 0.34.x &#8594; 0.35.x
## Global and Request context
The formerly used `JPAODataSessionContextAccess` as context for global (runtime static) and request (dynamic) aspects was splitted into two separate context concepts: `JPAODataGlobalContext` and `JPAODataRequestContext`. The request context provides also the informations embedded into the global context. So normally you can simply replace `JPAODataSessionContextAccess` by `JPAODataRequestContext`. Both classes are not longer located in the `org.apache.olingo.jpa.processor.core.api` package, but in `org.apache.olingo.jpa.processor`.

## More refactorings
* org.apache.olingo.jpa.processor.DependencyInjector
* Some deprecated legacy processors were removed, the functionality of adapter should not be affected

## `...jpa.processor.core.api.JPAODataServletHandler`:
* The customization method `protected Collection<Processor> collectProcessors(final HttpServletRequest request, final HttpServletResponse response, final EntityManager em)` to register own processors has now a changed signature: `protected Collection<Processor> collectProcessors(final JPAODataRequestContext requestContext)`
* The customization method `protected void prepareDependencyInjection(final DependencyInjector dpi)` was changed into a more generic customization method `protected void prepareRequestContext(final JPAODataRequestContext requestContext)` supporting custom dependency injection and more.