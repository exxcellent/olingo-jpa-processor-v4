package org.apache.olingo.jpa.servlet.example;

import java.util.Arrays;
import java.util.UUID;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.jpa.processor.core.api.QueryResponseCustomizer;
import org.apache.olingo.jpa.processor.core.testmodel.Person;

public class ExampleQueryResponseCustomizer implements QueryResponseCustomizer {

  @Override
  public EntityCollection customizeResult(Edm edm, Class<?> entityType, EntityCollection result) {
    if(entityType == Person.class && !result.getEntities().isEmpty()) {
      result.getEntities().forEach(p -> {
        Property pNewComplex = new Property();
        pNewComplex.setName("personExtendByAdditionalAttribute");
        pNewComplex.setType("org.apache.olingo.jpa.ChangeInformation");
        ComplexValue cNew =  new ComplexValue();
        Property pComplexNested = new Property();
        pComplexNested.setName("By"); //the property is not dynamic -> use existing property name, but type is already known
        pComplexNested.setValue(ValueType.PRIMITIVE, UUID.randomUUID().toString());
        cNew.getValue().add(pComplexNested);
        pNewComplex.setValue(ValueType.COMPLEX, Arrays.asList(cNew));
        p.addProperty(pNewComplex);
      });
    }
    //as default: to not touch the result
    return null;
  }

}
