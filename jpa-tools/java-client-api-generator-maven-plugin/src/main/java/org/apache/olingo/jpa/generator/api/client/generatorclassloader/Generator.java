package org.apache.olingo.jpa.generator.api.client.generatorclassloader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateModelGenerator;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;

//Attention: Do not use any other class than from system class loader or inside this package!
//We cannot use classes from Maven, because we an complete separated class loading to workaround that shit java.util.ServiceLoader
//not inspecting our newly classpath added jar's for the PersistenceProvider
public class Generator {

  private LogWrapper log = new LogWrapper(null);
  private File generationDirectory = null;
  private boolean generateProtocolCode = true;

  /**
   *
   * @param log An instance of {@link org.apache.maven.plugin.logging.Log}
   */
  public void configure(final Object log, final File generationDirectory, final boolean generateProtocolCode) {
    this.log = new LogWrapper(log);
    this.generationDirectory = generationDirectory;
    this.generateProtocolCode = generateProtocolCode;
  }

  public void generate(final String persistenceUnit) throws RuntimeException {

    if (generationDirectory == null) {
      throw new IllegalStateException("Call configure() before generate() to set generation target directory");
    }
    final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    try {
      // workaround SPI cache to force reloading using our classpath, because ServiceLoader
      // is already loaded by system class loader...
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

      checkJPAProviderPresence();

      final ScannerJPAAdapter jpaAdapter = new ScannerJPAAdapter(persistenceUnit,
          buildEntityManagerFactoryProperties());
      try {
        final JPAEdmProvider edmProvider = new JPAEdmProvider(jpaAdapter.getNamespace(), jpaAdapter.getMetamodel());
        final IntermediateServiceDocument isd = edmProvider.getServiceDocument();
        final IntermediateModelGenerator accessor = new IntermediateModelGenerator(isd, log, generationDirectory);
        accessor.generateEnumAPI();
        accessor.generateTypeAPI(generateProtocolCode);
      } catch (final ODataException e) {
        throw new RuntimeException(e);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    } finally {
      // restore context class loader
      Thread.currentThread().setContextClassLoader(ccl);
    }
  }

  private static Map<String, Object> buildEntityManagerFactoryProperties() {
    final Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:test" + Long.toString(System.currentTimeMillis())
    + ";DB_CLOSE_DELAY=-1");
    properties.put("javax.persistence.jdbc.user", "sa");
    properties.put("javax.persistence.jdbc.password", "");
    properties.put("javax.persistence.jdbc.driver", "org.h2.Driver");
    return properties;
  }

  private void checkJPAProviderPresence() throws RuntimeException {
    final PersistenceProviderResolver resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
    final List<PersistenceProvider> providers = resolver.getPersistenceProviders();
    if (!providers.isEmpty()) {
      return;
    }
    throw new RuntimeException(
        "No persistence provider present! You need to have an configured JPA provider for code generation...");
  }
}
