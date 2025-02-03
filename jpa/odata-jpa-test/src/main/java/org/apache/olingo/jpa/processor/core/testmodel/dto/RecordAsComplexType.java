package org.apache.olingo.jpa.processor.core.testmodel.dto;

import java.util.Map;

import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;

@ODataComplexType
public record RecordAsComplexType(String By, Map<String, String> parameters) {

}
