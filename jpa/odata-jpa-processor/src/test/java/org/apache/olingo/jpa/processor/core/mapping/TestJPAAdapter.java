package org.apache.olingo.jpa.processor.core.mapping;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.dto.TestDTOs;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescriptionKey;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Metamodel;

public class TestJPAAdapter extends TestBase {

  private class EvilJPAAdapter implements JPAAdapter {
    protected final Set<Class<?>> dtos = new LinkedHashSet<>();
    protected final Set<Class<?>> cts = new LinkedHashSet<>();

    @Override
    public EntityManager createEntityManager() throws RuntimeException {
      return null;
    }

    @Override
    public void beginTransaction(EntityManager em) throws RuntimeException {
    }

    @Override
    public void commitTransaction(EntityManager em) throws RuntimeException {
    }

    @Override
    public void cancelTransaction(EntityManager em) throws RuntimeException {}

    @Override
    public String getNamespace() {
      return Constant.PUNIT_NAME;
    }

    @Override
    public Metamodel getMetamodel() {
      return TestJPAAdapter.this.persistenceAdapter.getMetamodel();
    }

    @Override
    public Collection<Class<?>> getDTOEntityTypes() {
      return dtos;
    }

    @Override
    public Collection<Class<?>> getDTOComplexTypes() {
      return cts;
    }

    @Override
    public JPAODataDatabaseProcessor getDatabaseAccessor() {
      return TestJPAAdapter.this.persistenceAdapter.getDatabaseAccessor();
    }

    @Override
    public void dispose() {}
    
  }
  
  @Test
  public void testNonDTOThrowsError() throws IOException, ODataException, SQLException {
    // create own instance to avoid pollution of other tests
    final EvilJPAAdapter myPersistenceAdapter = new EvilJPAAdapter();
    myPersistenceAdapter.dtos.add(TestDTOs.class);
    // must throw an exception on further processing
    Assertions.assertThrows(ODataJPAModelException.class, () -> {
      final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
      final ServerCallSimulator helper = new ServerCallSimulator(myPersistenceAdapter, uriBuilder);
      helper.execute(HttpStatusCode.OK.getStatusCode());
    });
  }

  @Test
  public void testInvalidDTOClass() {
    ResourceLocalPersistenceAdapter adapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
    assertThrows(IllegalArgumentException.class, () -> adapter.registerDTOEntityType(Object.class));
  }

  @Test
  public void testInvalidComplextypeClass() {
    ResourceLocalPersistenceAdapter adapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
    assertThrows(IllegalArgumentException.class, () -> adapter.registerDTOComplexType(Object.class));
  }

  @Test
  public void testInvalidEntityClassAsDTOEntity() {
    ResourceLocalPersistenceAdapter adapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
    assertThrows(IllegalArgumentException.class, () -> adapter.registerDTOEntityType(Organization.class));
  }

  @Test
  public void testInvalidEmbeddableClassAsDTOComplexType() {
    ResourceLocalPersistenceAdapter adapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
    assertThrows(IllegalArgumentException.class, () -> adapter.registerDTOComplexType(AdministrativeDivisionDescriptionKey.class));
  }
  
}
