package org.apache.olingo.jpa.processor.core.exception;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAMessageKey;

public class ODataJPAConversionException extends ODataJPAProcessException {

  private static final long serialVersionUID = -1488005354148192788L;

  public static enum MessageKeys implements ODataJPAMessageKey {
    ATTRIBUTE_MUST_NOT_BE_NULL,
    ATTRIBUTE_ALREADY_CONVERTED,
    GENERATED_KEY_ATTRIBUTE_IS_NOT_SUPPORTED,
    BINDING_LINK_NOT_RESOLVED,
    RUNTIME_PROBLEM;

    @Override
    public String getKey() {
      return name();
    }

  }

  private static final String BUNDEL_NAME = "exceptions-conversion-i18n";

  public ODataJPAConversionException(final MessageKeys messageKey, final String... params) {
    this(HttpStatusCode.INTERNAL_SERVER_ERROR, messageKey, params);
  }

  public ODataJPAConversionException(final HttpStatusCode statusCode, final MessageKeys messageKey, final String... params) {
    this(statusCode, null, messageKey, params);
  }

  public ODataJPAConversionException(final Throwable cause, final MessageKeys messageKey,
      final String... params) {
    this(HttpStatusCode.INTERNAL_SERVER_ERROR, cause, messageKey, params);
  }

  protected ODataJPAConversionException(final HttpStatusCode statusCode, final Throwable cause, final MessageKeys messageKey,
      final String... params) {
    super(messageKey.getKey(), statusCode, cause, params);
  }

  @Override
  protected String getBundleName() {
    return BUNDEL_NAME;
  }

}
