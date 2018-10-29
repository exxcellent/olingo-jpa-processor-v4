package org.apache.olingo.jpa.servlet.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataGetHandler;
import org.apache.olingo.jpa.processor.core.database.JPADefaultDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.AbstractJPAAdapter;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.mapping.ResourceLocalPersistenceAdapter;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.processor.Processor;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Example call: http://localhost:8080/odata/$metadata
 *
 * @author Ralf Zozmann
 *
 */
@WebServlet(name = "odata-servlet", loadOnStartup = 1, urlPatterns = { "/odata/*" })
@ServletSecurity(httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = { "Reader" }),
		@HttpMethodConstraint(value = "POST", rolesAllowed = { "Writer" }) })
public class ODataServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String JNDI_DATASOURCE = "java:comp/env/jdbc/testDS";

	@Override
	public void init() throws ServletException {
		super.init();
		// we have to prepare our own (test) database
		try {
			final DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/testDS");
			DataSourceHelper.initializeDatabase(ds);
		} catch (final NamingException ne) {
			throw new ServletException("Initialization of DataSource failed", ne);
		}

		// redirect logging to slf4j
		try {
			final InputStream is = getClass().getResourceAsStream("/java.util.logging.properties");
			LogManager.getLogManager().readConfiguration(is);
		} catch (SecurityException | IOException e) {
			throw new ServletException("Initialization of Logging failed", e);
		}
		SLF4JBridgeHandler.install();

		log("oData endpoint prepared");
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {

		JPAODataGetHandler handler = null;
		try {

			handler = createHandler();

			//			logSchema(handler.getJPAODataContext().getEdmProvider().getServiceDocument());

			handler.process(req, resp);
		} catch (final RuntimeException | ODataException e) {
			throw new ServletException(e);
		} finally {
			if (handler != null) {
				handler.dispose();
			}
		}

	}

	private JPAODataGetHandler createHandler() throws ODataException, ServletException {

		final Map<Object, Object> elProperties = new HashMap<>();
		elProperties.put("javax.persistence.nonJtaDataSource", JNDI_DATASOURCE);
		// force logging over slf4j for EclipseLink
		//		elProperties.put("eclipselink.logging.logger", "org.eclipse.persistence.logging.slf4j.SLF4JLogger");
		elProperties.put("eclipselink.logging.logger", "JavaLogger");

		final JPAAdapter mappingAdapter = new ResourceLocalPersistenceAdapter(Constant.PUNIT_NAME,
				elProperties,
				new JPADefaultDatabaseProcessor());
		((AbstractJPAAdapter) mappingAdapter).registerDTO(EnvironmentInfo.class);

		final JPAODataGetHandler handler = new JPAODataGetHandler(mappingAdapter) {
			/**
			 * Use anonymous instance to add an error processor logging errors while
			 * processing to the servlet container terminal...
			 */
			@Override
			protected Collection<Processor> collectProcessors(final HttpServletRequest request,
					final HttpServletResponse response, final EntityManager em) {
				final Collection<Processor> processors = super.collectProcessors(request, response, em);
				processors.add(new ExampleErrorProcessor());
				return processors;
			}

			@Override
			protected void prepareDependencyInjection(final DependencyInjector dpi) {
				super.prepareDependencyInjection(dpi);
				// example for custom dependency injection
				dpi.registerDependencyMapping(String.class, getServletName());
			}
		};
		return handler;
	}

	@SuppressWarnings("unused")
	private void logSchema(final IntermediateServiceDocument sd) throws ODataException {
		for (final CsdlSchema schema : sd.getEdmSchemas()) {

			log("Entities in schema " + schema.getNamespace() + ":");
			if (schema.getEntityTypes() == null || schema.getEntityTypes().isEmpty()) {
				log("  -");
			} else {
				final StringBuilder buffer = new StringBuilder();
				final List<CsdlEntityType> types = schema.getEntityTypes();
				for (int i=0;i<types.size();i++) {
					if(i>0) {
						buffer.append(", ");
					}
					buffer.append(types.get(i).getName());
				}
				log("  " + buffer.toString());
			}

			log("Actions in schema " + schema.getNamespace() + ":");
			if (schema.getActions() == null || schema.getActions().isEmpty()) {
				log("  -");
			} else {
				for (final CsdlAction action : schema.getActions()) {
					log("  "+action.getName() + "("
							+ (action.getParameters() == null ? "" : String.join(", ", action.getParameters().stream()
									.map(o -> o.getName() + ": " + (o.isCollection() ? "Collection<" : "") + o.getTypeFQN() + (o.isCollection() ? ">" : ""))
									.collect(Collectors.toList())))
							+ "): " + (action.getReturnType() != null ? (action.getReturnType().isCollection() ? "Collection<":"" ) +
									action.getReturnType().getTypeFQN() + (action.getReturnType().isCollection() ? ">" : "" ) : "void"));
				}
			}

			log("Functions in schema " + schema.getNamespace() + ":");
			if(schema.getFunctions() == null || schema.getFunctions().isEmpty()) {
				log("-");
			} else {
				for (final CsdlFunction function : schema.getFunctions()) {
					log("  "+function.getName() + "("
							+ (function.getParameters() == null ? "" : String.join(", ", function.getParameters().stream()
									.map(o -> o.getName()+": "+o.getTypeFQN()).collect(Collectors.toList())))
							+ "): " + (function.getReturnType() != null ? function.getReturnType().getTypeFQN() : "void"));
				}
			}
		}
	}

}
