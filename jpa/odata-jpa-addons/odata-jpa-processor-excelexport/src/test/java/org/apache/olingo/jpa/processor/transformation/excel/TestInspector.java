package org.apache.olingo.jpa.processor.transformation.excel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Helper class to inspect generated Excel sheet after creation
 *
 */
class TestInspector {

  private final Configuration usedConfiguration;
  private final Workbook workbook;

  public TestInspector(final Configuration usedConfiguration, final byte[] sheetData) throws IOException {
    this(usedConfiguration, new ByteArrayInputStream(sheetData));
  }

  public TestInspector(final Configuration usedConfiguration, final InputStream sheetData) throws IOException {
    this.usedConfiguration = usedConfiguration;
    workbook = new XSSFWorkbook(sheetData);
  }

  /**
   *
   * @return the column index or -1 if not found
   */
  public int determineColumnIndex(final String sheetName, final String excelColumnName) {
    if (!usedConfiguration.isCreateHeaderRow()) {
      throw new IllegalStateException("without header row no column can be identified");
    }
    final Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      throw new IllegalStateException("Sheet with name " + sheetName + " not present");
    }
    final Row row = sheet.getRow(0);
    final Iterator<Cell> it = row.cellIterator();
    while (it.hasNext()) {
      final Cell cell = it.next();
      if (cell.getStringCellValue() != null && excelColumnName.equals(cell.getStringCellValue())) {
        return cell.getColumnIndex();
      }
    }
    return -1;
  }

  /**
   *
   * @return TRUE if any header cell has an empty name
   */
  public boolean hasHeaderColumnWithoutName(final String sheetName) {
    if (!usedConfiguration.isCreateHeaderRow()) {
      throw new IllegalStateException("without header row no column can be identified");
    }
    final Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      throw new IllegalStateException("Sheet with name " + sheetName + " not present");
    }
    final Row row = sheet.getRow(0);
    final Iterator<Cell> it = row.cellIterator();
    while (it.hasNext()) {
      final Cell cell = it.next();
      if (cell.getStringCellValue() == null || cell.getStringCellValue().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * This can only be used if the header row is not suppressed in configuration.
   * @param excelColumnName The name of column in sheet to look for.
   *
   * @return TRUE if first row contains a cell with given name.
   */
  public boolean hasColumnOfName(final String sheetName, final String excelColumnName) throws IllegalStateException {
    final int colIndex = determineColumnIndex(sheetName, excelColumnName);
    return colIndex > -1;
  }

  public int determineNumberOfColumns(final String sheetName) throws IllegalStateException {
    final Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      throw new IllegalStateException("Sheet with name " + sheetName + " not present");
    }
    final Row row = sheet.getRow(0);
    return row.getPhysicalNumberOfCells();
  }

  public int determineNumberOfRows(final String sheetName) throws IllegalStateException {
    final Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      throw new IllegalStateException("Sheet with name " + sheetName + " not present");
    }
    return sheet.getPhysicalNumberOfRows();
  }

  public Number determineCellValueAsNumber(final String sheetName, final int rowIndex, final String excelColumnName)
      throws IllegalStateException {
    final int colIndex = determineColumnIndex(sheetName, excelColumnName);
    if (colIndex < 0) {
      throw new IllegalStateException("Column not found");
    }
    final Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      throw new IllegalStateException("Sheet with name " + sheetName + " not present");
    }
    final Row row = sheet.getRow(rowIndex);
    final Cell cell = row.getCell(colIndex);
    if (cell == null) {
      throw new IllegalStateException("Cell not existing at [" + rowIndex + "," + colIndex + "]");
    }
    return Double.valueOf(cell.getNumericCellValue());
  }
}
