package org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner;

import javax.persistence.MappedSuperclass;

import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;

//The data access conditioner must be found also for subclasses so we define this useless super class to test it
@ODataEntity(handlerDataAccessConditioner = GenericBusinessPartnerDataAccessConditioner.class)
@MappedSuperclass
public abstract class AbstractGenericBusinessPartner {
  // no custom code
}
