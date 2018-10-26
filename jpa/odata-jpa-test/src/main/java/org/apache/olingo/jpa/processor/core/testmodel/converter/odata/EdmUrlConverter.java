package org.apache.olingo.jpa.processor.core.testmodel.converter.odata;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * OData converter implementation to handle URL's as simple strings in the OData
 * world.
 *
 * @author Ralf Zozmann
 *
 */
public class EdmUrlConverter implements ODataAttributeConverter<URL, String> {
	@Override
	public EdmPrimitiveTypeKind getODataType() {
		return EdmPrimitiveTypeKind.String;
	}

	@Override
	public URL convertToJPAEntity(final String oDataValue) {
		if (oDataValue == null) {
			return null;
		}
		try {
			return new URL(oDataValue);
		} catch (final MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String convertToOData(final URL jpaValue) {
		if (jpaValue == null) {
			return null;
		}
		return jpaValue.toExternalForm();
	}
}
