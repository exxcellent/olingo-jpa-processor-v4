package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.serializer.CustomContentTypeSupport;
import org.apache.olingo.server.api.serializer.RepresentationType;

/**
 *
 * @author Ralf Zozmann
 *
 */
public final class ExtensibleContentTypeSupport implements CustomContentTypeSupport {

  private final Map<RepresentationType, ContentType> mappings = new HashMap<>();

  @Override
  public List<ContentType> modifySupportedContentTypes(final List<ContentType> defaultContentTypes, final RepresentationType type) {
    final List<ContentType> list = new ArrayList<>(defaultContentTypes);
    if (type == RepresentationType.ACTION_PARAMETERS && !contains(defaultContentTypes, ContentType.MULTIPART_FORM_DATA)) {
      // for actions we allow multipart/form-data to enable file upload capability
      list.add(ContentType.MULTIPART_FORM_DATA);
    }
    // add custom content types
    for (final Map.Entry<RepresentationType, ContentType> entry : mappings.entrySet()) {
      if (type != entry.getKey()) {
        continue;
      }
      if (contains(defaultContentTypes, entry.getValue())) {
        continue;
      }
      list.add(entry.getValue());
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

  /**
   *
   * @param contentType The content type to accept.
   * @param representationTypes The representation types supported for content type. maybe <code>null</code> for ALL
   * {@link RepresentationType}'s.
   */
  public void activateCustomContentType(final ContentType contentType,
      final RepresentationType... representationTypes) {
    if (contentType == null) {
      throw new IllegalArgumentException("content type required");
    }
    RepresentationType[] types;
    if (representationTypes == null || representationTypes.length == 0) {
      types = RepresentationType.values();
    } else {
      types = representationTypes;
    }
    for (final RepresentationType rt : types) {
      if (rt == null) {
        throw new IllegalArgumentException("representation must not be null");
      }
      mappings.put(rt, contentType);
    }
  }
}
