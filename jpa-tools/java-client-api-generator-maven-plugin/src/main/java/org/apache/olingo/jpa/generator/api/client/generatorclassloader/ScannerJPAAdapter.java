package org.apache.olingo.jpa.generator.api.client.generatorclassloader;

import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.olingo.jpa.processor.core.database.JPA_DefaultDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.AbstractJPAAdapter;

public class ScannerJPAAdapter extends AbstractJPAAdapter {

  public ScannerJPAAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties) {
    super(pUnit, mapEntityManagerProperties, new JPA_DefaultDatabaseProcessor());
  }

  @Override
  public void beginTransaction(final EntityManager em)
      throws RuntimeException {
    throw new UnsupportedOperationException("Should not be called!");
  }

  @Override
  public void commitTransaction(final EntityManager em) throws RuntimeException {
    throw new UnsupportedOperationException("Should not be called!");
  }

  @Override
  public void cancelTransaction(final EntityManager em) throws RuntimeException {
    throw new UnsupportedOperationException("Should not be called!");
  };

}
