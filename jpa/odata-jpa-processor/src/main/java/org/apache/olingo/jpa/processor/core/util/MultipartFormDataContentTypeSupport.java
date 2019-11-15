package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.serializer.CustomContentTypeSupport;
import org.apache.olingo.server.api.serializer.RepresentationType;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class MultipartFormDataContentTypeSupport implements CustomContentTypeSupport {

	@Override
	public List<ContentType> modifySupportedContentTypes(final List<ContentType> defaultContentTypes, final RepresentationType type) {
		final List<ContentType> list = new ArrayList<>(defaultContentTypes);
		if (type == RepresentationType.ACTION_PARAMETERS && !contains(defaultContentTypes, ContentType.MULTIPART_FORM_DATA)) {
			// for actions we allow multipart/form-data to enable file upload capability
			list.add(ContentType.MULTIPART_FORM_DATA);
		}
		return list;
	}

	private boolean contains(final List<ContentType> defaultContentTypes, final ContentType requestedType) {
		for (final ContentType inList : defaultContentTypes) {
			if (inList == requestedType) {
				return true;
			}
		}
		return false;
	}
}
