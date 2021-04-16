package org.apache.olingo.jpa.processor.transformation.excel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;

public class Configuration {

  private static class ColumnConfiguration {
    boolean suppressed = false;
    String customColumnName = null;
    int customColumIndex = -1;
  }

  private final Map<String, String> mapEntityType2SheetName = new HashMap<>();
  private boolean createHeaderRow = true;
  private String formatDate = "yyyy-MM-DD";
  private String formatTime = "hh:mm:ss";
  private String formatDateTime = "yyyy-MM-DD hh:mm:ss";
  private String formatDecimal = "#,##0.000;[RED]-#,##0.000";// thousand-separator and 3 decimals, negative red
  private String formatInteger = "#0";
  private String fontName = null;
  private final Map<String, Map<String, ColumnConfiguration>> mapEntityType2ColumnConfiguration = new HashMap<>();

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

  String getSheetName(final JPAEntityType entity) {
    final String sheetName = mapEntityType2SheetName.get(determineSheetKeyName(entity));
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

  private Map<String, ColumnConfiguration> findOrCreateSheetColumnConfigurations(final String entityTypeName) {
    Map<String, ColumnConfiguration> configs = mapEntityType2ColumnConfiguration.get(entityTypeName);
    if (configs == null) {
      configs = new HashMap<>();
      mapEntityType2ColumnConfiguration.put(entityTypeName, configs);
    }
    return configs;
  }

  private ColumnConfiguration findOrCreateColumnConfiguration(final String entityTypeName, final String dbColumnName) {
    final Map<String, ColumnConfiguration> configs = findOrCreateSheetColumnConfigurations(entityTypeName);
    ColumnConfiguration cc = configs.get(dbColumnName);
    if (cc == null) {
      cc = new ColumnConfiguration();
      configs.put(dbColumnName, cc);
    }
    return cc;
  }

  private ColumnConfiguration findColumnConfiguration(final JPAEntityType entity, final String dbColumnName) {
    final Map<String, ColumnConfiguration> configs = mapEntityType2ColumnConfiguration.get(determineSheetKeyName(
        entity));
    if (configs == null) {
      return null;
    }
    return configs.get(dbColumnName);
  }

  /**
   * Suppressing additional columns in Excel sheet output is useful for scenarios where the $select expression is not
   * enough as for key columns always selected and included in result set.
   *
   * @param dbColumnNames The name of column in Excel sheet to suppress. The name is normally identically with the DB
   * path name of attribute (see
   * {@link org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType#getPathList()
   * JPAStructuredType.getPathList()}).
   */
  public final void addSuppressedColumns(final String entityTypeName, final String... dbColumnNames) {
    if (dbColumnNames == null || dbColumnNames.length < 1) {
      throw new IllegalArgumentException("At least one column must be given");
    }
    for (final String cn : dbColumnNames) {
      final ColumnConfiguration cc = findOrCreateColumnConfiguration(entityTypeName, cn);
      if (cc.customColumnName != null) {
        throw new IllegalArgumentException("Inconsistent configuration: Column " + cn
            + " has already an assigned custom name");
      } else if (cc.customColumIndex > -1) {
        throw new IllegalArgumentException("Inconsistent configuration: Column " + cn
            + " has already an assigned column index");
      }
      cc.suppressed = true;
    }
  }

  boolean isSuppressedColumn(final JPAEntityType entity, final String dbColumnName) {
    final ColumnConfiguration cc = findColumnConfiguration(entity, dbColumnName);
    if (cc == null) {
      return false;
    }
    return cc.suppressed;
  }

  /**
   * Set a custom column name for the resulting column in Excel sheet.
   *
   * @param entityTypeName The entity determine the sheet to affect.
   * @param dbColumnName The attribute/column to rename in Excel sheet.
   * @param excelName The custom column name in final Excel sheet.
   */
  public final void assignColumnName(final String entityTypeName, final String dbColumnName, final String excelName) {
    if (!createHeaderRow) {
      throw new IllegalArgumentException("Inconsistent configuration: Generation of header row is disabled");
    }
    final ColumnConfiguration cc = findOrCreateColumnConfiguration(entityTypeName, dbColumnName);
    if (excelName != null && cc.suppressed) {
      throw new IllegalArgumentException("Inconsistent configuration: Column " + dbColumnName
          + " is already suppressed in Excel");
    }
    cc.customColumnName = excelName;
  }

  /**
   * This is convenience method to define a column order at once. Internally a call to this method is mapped to separate
   * calls to {@link #assignColumnIndex(String, String, int)}. The first column will get the index 0.
   */
  public final void assignColumnOrder(final String entityTypeName, final String... dbColumnNames) {
    if (dbColumnNames == null) {
      return;
    }
    for (int i = 0; i < dbColumnNames.length; i++) {
      assignColumnIndex(entityTypeName, dbColumnNames[i], i);
    }
  }

  /**
   * Be aware: if only one column get's an index: all other columns will be arranged <b>after</b> that column, because
   * the only (aka minimum) column index defines the first column.
   *
   * @param entityTypeName The entity determine the sheet to affect.
   * @param dbColumnName The attribute/column to rename in Excel sheet.
   * @param excelColumnIndex The column index of the affected DB column in final Excel sheet.
   */
  public final void assignColumnIndex(final String entityTypeName, final String dbColumnName,
      final int excelColumnIndex) {
    if (excelColumnIndex < 0) {
      throw new IllegalArgumentException("Inconsistent configuration: Column index must be > -1");
    }

    // validate
    final Map<String, ColumnConfiguration> configs = mapEntityType2ColumnConfiguration.get(entityTypeName);
    if (configs != null) {
      for (final ColumnConfiguration cc : configs.values()) {
        if (cc.customColumIndex == excelColumnIndex) {
          throw new IllegalArgumentException("Inconsistent configuration: Column index already set");
        }
      }
    }

    // assign
    final ColumnConfiguration cc = findOrCreateColumnConfiguration(entityTypeName, dbColumnName);
    if (cc.suppressed) {
      throw new IllegalArgumentException("Inconsistent configuration: Column " + dbColumnName
          + " is already suppressed in Excel");
    }
    cc.customColumIndex = excelColumnIndex;
  }

  /**
   *
   * @return The custom name or <code>null</code>
   */
  String getCustomColumnName(final JPAEntityType entity, final String dbColumnName) {
    final ColumnConfiguration cc = findColumnConfiguration(entity, dbColumnName);
    if (cc == null) {
      return null;
    }
    return cc.customColumnName;

  }

  /**
   *
   * @param entity The sheet defining entity
   * @return A map where the key is the DB column name and the value the assigned column index for final Excel sheet.
   * Unassigned columns are not present in map. The map is created as return value and not used internally.
   */
  Map<String, Integer> getCustomColumnIndexes(final JPAEntityType entity) {
    final Map<String, ColumnConfiguration> configs = mapEntityType2ColumnConfiguration.get(determineSheetKeyName(
        entity));
    if (configs == null) {
      return Collections.emptyMap();
    }
    final Map<String, Integer> result = new HashMap<>();
    for (final Map.Entry<String, ColumnConfiguration> entry : configs.entrySet()) {
      if (entry.getValue().suppressed) {
        continue;
      }
      if (entry.getValue().customColumIndex < 0) {
        continue;
      }
      result.put(entry.getKey(), Integer.valueOf(entry.getValue().customColumIndex));
    }
    return result;
  }

  private static String determineSheetKeyName(final JPAEntityType entity) {
    return entity.getExternalName();
  }

}
