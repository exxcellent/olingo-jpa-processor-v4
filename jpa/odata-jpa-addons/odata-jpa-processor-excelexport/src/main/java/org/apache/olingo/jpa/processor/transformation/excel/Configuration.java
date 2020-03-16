package org.apache.olingo.jpa.processor.transformation.excel;

import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;

public class Configuration {

  private final Map<String, String> mapEntityType2SheetName = new HashMap<>();
  private boolean createHeaderRow = true;
  private String formatDate = "yyyy-MM-DD";
  private String formatTime = "hh:mm:ss";
  private String formatDateTime = "yyyy-MM-DD hh:mm:ss";
  private String formatDecimal = "#,##0.000;[RED]-#,##0.000";// thousand-separator and 2 decimals, negative red
  private String formatInteger = "#0";
  private String fontName = null;

  public void setCreateHeaderRow(final boolean createHeaderRow) {
    this.createHeaderRow = createHeaderRow;
  }

  public boolean isCreateHeaderRow() {
    return createHeaderRow;
  }

  public final void setFormatDate(final String formatDate) {
    if (formatDate != null && formatDate.isEmpty()) {
      throw new IllegalArgumentException("format must have a value");
    }
    this.formatDate = formatDate;
  }

  public String getFormatDate() {
    return formatDate;
  }

  public final void setFormatDecimal(final String formatDecimal) {
    if (formatDecimal != null && formatDecimal.isEmpty()) {
      throw new IllegalArgumentException("format must have a value");
    }
    this.formatDecimal = formatDecimal;
  }

  public String getFormatDecimal() {
    return formatDecimal;
  }

  public final void setFormatInteger(final String formatInteger) {
    if (formatInteger != null && formatInteger.isEmpty()) {
      throw new IllegalArgumentException("format must have a value");
    }
    this.formatInteger = formatInteger;
  }

  public String getFormatInteger() {
    return formatInteger;
  }

  /**
   * Map the sheet defining entity (OData external name) to an sheet name.
   * @see JPAEntityType#getExternalName()
   */
  public final void assignSheetName(final String entityTypeName, final String sheetName) {
    mapEntityType2SheetName.put(entityTypeName, sheetName);
  }

  public String getSheetName(final JPAEntityType entity) {
    final String sheetName = mapEntityType2SheetName.get(entity.getExternalName());
    if (sheetName != null) {
      return sheetName;
    }
    return entity.getExternalName();
  }

  public final void setFontName(final String fontName) {
    this.fontName = fontName;
  }

  public String getFontName() {
    return fontName;
  }

  public void setFormatDateTime(final String formatDateTime) {
    if (formatDateTime != null && formatDateTime.isEmpty()) {
      throw new IllegalArgumentException("format must have a value");
    }
    this.formatDateTime = formatDateTime;
  }

  public String getFormatDateTime() {
    return formatDateTime;
  }

  public void setFormatTime(final String formatTime) {
    if (formatTime != null && formatTime.isEmpty()) {
      throw new IllegalArgumentException("format must have a value");
    }
    this.formatTime = formatTime;
  }

  public String getFormatTime() {
    return formatTime;
  }
}
