package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.EntityManagerFactory;

import org.junit.BeforeClass;

public abstract class TestMappingRoot extends org.apache.olingo.jpa.processor.core.test.AbstractTest {

	protected static EntityManagerFactory emf;
	protected static final String BUPA_CANONICAL_NAME = "org.apache.olingo.jpa.processor.core.testmodel.BusinessPartner";
	protected static final String ADDR_CANONICAL_NAME = "org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData";
	protected static final String COMM_CANONICAL_NAME = "org.apache.olingo.jpa.processor.core.testmodel.CommunicationData";
	protected static final String ADMIN_CANONICAL_NAME =
			"org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision";

	@BeforeClass
	public static void setupClass() {
		emf = createEntityManagerFactory(TestDatabaseType.HSQLDB);
	}

}