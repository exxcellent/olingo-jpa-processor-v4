# Example WAR
* The following explanation is based on the existing example project, that can be take from the Maven repository as _org.apache.olingo.jpa.examples:olingo-generic-servlet-example_

## 1. Define WAR module
* Create a Maven module with packaging _war_.
* Add the dependencies as listed in [Dependency handling](Intro.md)

## 2. Define servlet
* For details look into `org.apache.olingo.jpa.servlet.example.ODataServlet`
The template for an standalone servlet managing OData/REST requests can be used as:

```java
@WebServlet(name = "odata-servlet", loadOnStartup = 1, urlPatterns = { "/odata/*" })
@ServletSecurity(httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = { "Reader" }),
		@HttpMethodConstraint(value = "POST", rolesAllowed = { "Writer" }) })
public class ODataServlet extends HttpServlet {
	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {

		try {

			JPAODataServletHandler requestHandler = createHandler();
			requestHandler.process(req, resp);
		} catch (final RuntimeException | ODataException e) {
			throw new ServletException(e);
		}

	}

	private JPAODataServletHandler createHandler() throws ODataException {
		final Map<Object, Object> elProperties = new HashMap<>();
		elProperties.put("javax.persistence.nonJtaDataSource", JNDI_DATASOURCE);
		JPAAdapter mappingAdapter = new ResourceLocalPersistenceAdapter(Constant.PUNIT_NAME,	elProperties, new JPA_DefaultDatabaseProcessor());
		return new JPAODataServletHandler(mappingAdapter);
	}

}
```
The example uses container managed security. To realize a more detailed control over security you should read [Servlet security](ServletSecurity.md).