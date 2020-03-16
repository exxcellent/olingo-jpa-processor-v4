package org.apache.olingo.jpa.processor.transformation.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TypeMapping;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.ValueConverter;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent.ContentState;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;

public class ExcelConverter {

  private static final Logger LOG = Logger.getLogger(ExcelConverter.class.getName());

  public static final class ExcelCell {
    private final EdmPrimitiveTypeKind cellRepresentation;
    private final Object value;

    /**
     *
     * @param cellRepresentation Corresponding to the <i>cellRepresentation</i> type the <i>value</i> match the type.
     * Example: {@link EdmPrimitiveTypeKind#Guid GUID} must have an value of type {@link java.util.UUID UUID}.
     * @param value
     */
    public ExcelCell(final EdmPrimitiveTypeKind cellRepresentation, final Object value) {
      this.cellRepresentation = cellRepresentation;
      this.value = value;
    }

    public EdmPrimitiveTypeKind getCellRepresentation() {
      return cellRepresentation;
    }

    public Object getValue() {
      return value;
    }
  }

  private static class State {
    private final Workbook workbook;
    Set<String> newCreatedSheets = new HashSet<>();
    short shortBestFontHeight = XSSFFont.DEFAULT_FONT_SIZE;
    Map<EdmPrimitiveTypeKind, CellStyle> mapDatatypeCellStyle = new HashMap<>();
    Map<Integer, Integer> mapColumn2RecommendedWidth = new HashMap<>();

    public State(final Workbook workbook) {
      this.workbook = workbook;
    }
  }

  private final static ValueConverter CONVERTER = new ValueConverter();
  private final Configuration configuration;

  public ExcelConverter(final Configuration configuration) {
    this.configuration = configuration != null ? configuration : new Configuration();
  }

  private State createWorkbook() throws IOException {
    return new State(new SXSSFWorkbook());
  }

  /**
   * Strategy:<br/>
   * <ol>
   * <li>Try to find existing sheet with given name</li>
   * <li>In all other cases a new sheet is created</li>
   * </ol>
   */
  private Sheet findOrCreateSheet(final State state, final String sheetName) {
    final Workbook workbook = state.workbook;
    Sheet sheet = workbook.getSheet(sheetName);
    if (sheet != null) {
      return sheet;
    }
    final int howManySheetsWeWillProduce = Math.max(1, 1);// currently always 1
    final int numberOfWorkbookSheets = workbook.getNumberOfSheets();
    if (howManySheetsWeWillProduce == 1 && numberOfWorkbookSheets == 1) {
      return workbook.getSheetAt(0);
    }
    sheet = workbook.createSheet(sheetName);
    state.newCreatedSheets.add(sheetName);
    return sheet;
  }

  private void createHeaderRow(final State state, final Sheet sheet, final int rowNumber, final Tuple dbRow) {
    int headerColumnNumber = 0;
    final Row headerRow = sheet.createRow(rowNumber);
    final CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
    final org.apache.poi.ss.usermodel.Font font = sheet.getWorkbook().createFont();
    font.setBold(true);
    cellStyle.setBorderBottom(BorderStyle.THIN);
    cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    font.setFontHeightInPoints(state.shortBestFontHeight);
    if (configuration.getFontName() != null) {
      font.setFontName(configuration.getFontName());
    }
    cellStyle.setFont(font);
    for (final TupleElement<?> dbCell : dbRow.getElements()) {
      final String attName = dbCell.getAlias();
      assignColumnWidth(state, attName.length(), headerColumnNumber);
      final Cell cell = headerRow.createCell(headerColumnNumber++);
      cell.setCellValue(attName);
      cell.setCellStyle(cellStyle);
    }
  }

  private void assignValueCellStyle(final State state, final Cell cell, final EdmPrimitiveTypeKind dataType) {
    CellStyle style = state.mapDatatypeCellStyle.get(dataType);
    if (style == null) {
      style = state.workbook.createCellStyle();

      switch (dataType) {
      case Boolean:
        break;
      case Byte:
      case Int16:
      case Int32:
      case Int64:
        if (configuration.getFormatInteger() != null) {
          final DataFormat dataFormat = state.workbook.createDataFormat();
          final short df = dataFormat.getFormat(configuration.getFormatInteger());
          style.setDataFormat(df);
        }
        break;
      case Decimal:
      case Double:
        if (configuration.getFormatDecimal() != null) {
          final DataFormat dataFormat = state.workbook.createDataFormat();
          final short df = dataFormat.getFormat(configuration.getFormatDecimal());
          style.setDataFormat(df);
        }
        break;
      case Date:
        if (configuration.getFormatDate() != null) {
          final DataFormat dataFormat = state.workbook.createDataFormat();
          final short df = dataFormat.getFormat(configuration.getFormatDate());
          style.setDataFormat(df);
        }
        break;
      case DateTimeOffset:
        if (configuration.getFormatDateTime() != null) {
          final DataFormat dataFormat = state.workbook.createDataFormat();
          final short df = dataFormat.getFormat(configuration.getFormatDateTime());
          style.setDataFormat(df);
        }
        break;
      case TimeOfDay:
        if (configuration.getFormatTime() != null) {
          final DataFormat dataFormat = state.workbook.createDataFormat();
          final short df = dataFormat.getFormat(configuration.getFormatTime());
          style.setDataFormat(df);
        }
        break;
      default:
        break;
      }
      final org.apache.poi.ss.usermodel.Font font = state.workbook.createFont();
      font.setFontHeightInPoints(state.shortBestFontHeight);
      if (configuration.getFontName() != null) {
        font.setFontName(configuration.getFontName());
      }
      style.setFont(font);
      state.mapDatatypeCellStyle.put(dataType, style);
    }
    cell.setCellStyle(style);
  }

  public ODataResponseContent produceExcel(final QueryEntityResult input, final RepresentationType representationType)
      throws IOException, ODataJPAModelException, ODataJPAConversionException {

    final JPAEntityType jpaType = input.getEntityType();

    final State state = createWorkbook();
    final Workbook workbook = state.workbook;
    final Sheet sheet = findOrCreateSheet(state, configuration.getSheetName(jpaType));
    final int size = input.getQueryResult().size();
    if (size > 500) {
      state.shortBestFontHeight = (short) (state.shortBestFontHeight - 1);
    } else if (size > 1000) {
      state.shortBestFontHeight = (short)(state.shortBestFontHeight-2);
    }

    int countRows = 0;
    int rowNumber = sheet.getFirstRowNum() < 0 ? 0 : sheet.getFirstRowNum();
    boolean firstRow = true;
    for (final Tuple dbRow : input.getQueryResult()) {
      // header
      if (firstRow && configuration.isCreateHeaderRow()) {
        createHeaderRow(state, sheet, rowNumber++, dbRow);
        firstRow = false;
      }
      // data
      final Row row = sheet.createRow(rowNumber++);
      int columnIndex = 0;
      for (final TupleElement<?> dbCell : dbRow.getElements()) {
        final Object value = dbRow.get(dbCell.getAlias());
        processCell(state, jpaType, row, columnIndex, value, dbCell.getAlias());
        columnIndex++;
      }
      countRows++;
    }
    // adjust column width
    for (final Map.Entry<Integer, Integer> entry : state.mapColumn2RecommendedWidth.entrySet()) {
      sheet.setColumnWidth(entry.getKey().intValue(), entry.getValue().intValue() * 256);
    }

    final ByteArrayOutputStream outResult = new ByteArrayOutputStream(1024 * 1024);
    workbook.write(outResult);
    workbook.close();
    final InputStream isResult = new ByteArrayInputStream(outResult.toByteArray());

    final ContentState contentState = determineContentState(countRows, representationType);
    return new ODataResponseContent(contentState, isResult);
  }

  private void processCell(final State state, final JPAEntityType jpaType, final Row row, final int columnIndex,
      final Object value,
      final String dbAlias) throws ODataJPAModelException, ODataJPAConversionException {

    final JPASelector selector = jpaType.getPath(dbAlias);
    final JPAAttribute<?> targetAttribute = selector.getLeaf();

    final ExcelCell excelCell = determineCellContent(targetAttribute, value);
    final Object odataValue = excelCell.getValue();

    final Cell cell = row.createCell(columnIndex);
    if (odataValue == null) {
      cell.setBlank();
    } else {
      switch (excelCell.getCellRepresentation()) {
      case Boolean:
        cell.setCellValue(Boolean.class.cast(odataValue).booleanValue());
        break;
      case Byte:
      case Int16:
      case Int32:
      case Int64:
      case Decimal:
      case Double:
        // any number (decimal or integer)
        final double dV = Number.class.cast(odataValue).doubleValue();
        cell.setCellValue(dV);
        assignColumnWidth(state, Double.toString(dV).length(), columnIndex);
        break;
      case Date:
      case DateTimeOffset:
      case TimeOfDay:
        processTimeRelatedCell(state, cell, columnIndex, excelCell.getCellRepresentation(), odataValue);
        break;
      default:
        // String like...
        cell.setCellValue(odataValue.toString());
        assignColumnWidth(state, odataValue.toString().length(), columnIndex);
      }
    }
    assignValueCellStyle(state, cell, excelCell.getCellRepresentation());

  }

  private void processTimeRelatedCell(final State state, final Cell cell, final int columnIndex,
      final EdmPrimitiveTypeKind dataType, final Object odataValue) {
    if (odataValue instanceof Date) {
      cell.setCellValue((Date) odataValue);
    } else if (odataValue instanceof Calendar) {
      if (dataType == EdmPrimitiveTypeKind.TimeOfDay) {
        final Calendar cal = (Calendar) odataValue;
        // POI does not work correct for Calendars without date part (especially the year is a problem), so we have to
        // convert into a fractional value for direct setting
        final double fraction = (((cal.get(Calendar.HOUR_OF_DAY) * 60.0
            + cal.get(Calendar.MINUTE)) * 60.0 + cal.get(Calendar.SECOND)) * 1000.0 + cal.get(Calendar.MILLISECOND))
            / DateUtil.DAY_MILLISECONDS;
        cell.setCellValue(fraction);
      } else {
        cell.setCellValue((Calendar) odataValue);
      }
    } else if (odataValue instanceof LocalDate) {
      cell.setCellValue((LocalDate) odataValue);
    } else if (odataValue instanceof LocalDateTime) {
      cell.setCellValue((LocalDateTime) odataValue);
    } else if (odataValue instanceof ZonedDateTime) {
      cell.setCellValue(((ZonedDateTime) odataValue).toLocalDateTime());
    }
    int length;
    switch (dataType) {
    case Date:
      length = configuration.getFormatDate() != null && configuration.getFormatDate().length() > 1 ? configuration
          .getFormatDate().length() : 8;
          break;
    case DateTimeOffset:
      length = configuration.getFormatDateTime() != null && configuration.getFormatDateTime().length() > 1
      ? configuration.getFormatDateTime().length() : 18;
      break;
    case TimeOfDay:
      length = configuration.getFormatTime() != null && configuration.getFormatTime().length() > 1 ? configuration
          .getFormatTime().length() : 8;
          break;
    default:
      throw new IllegalArgumentException("not a time related value");
    }
    assignColumnWidth(state, length, columnIndex);
  }

  protected ExcelCell determineCellContent(final JPAAttribute<?> targetAttribute, final Object value) {
    final EdmPrimitiveTypeKind kindOfCell = determineCellRepresentation(targetAttribute);
    Object odataValue;
    if (JPATypedElement.class.isInstance(targetAttribute)) {
      try {
        odataValue = CONVERTER.convertJPA2ODataPrimitiveValue((JPATypedElement) targetAttribute, value);
      } catch (ODataJPAConversionException | ODataJPAModelException e) {
        // do not convert
        LOG.log(Level.FINER, "Problem converting attribute value for Excel export: " + targetAttribute
            .getInternalName(), e);
        odataValue = value;
      }
    } else {
      // do not convert
      odataValue = value;
    }
    return new ExcelCell(kindOfCell, odataValue);
  }

  protected EdmPrimitiveTypeKind determineCellRepresentation(final JPAAttribute<?> targetAttribute) {
    if (JPATypedElement.class.isInstance(targetAttribute)) {
      try {
        return TypeMapping.convertToEdmSimpleType((JPATypedElement) targetAttribute);
      } catch (final ODataJPAModelException e) {
        // ignore -> use default
        return EdmPrimitiveTypeKind.String;
      }
    } else {
      LOG.log(Level.WARNING, "Unexpected attribute type " + targetAttribute.getClass().getSimpleName() + " for "
          + targetAttribute.getInternalName());
      return EdmPrimitiveTypeKind.String;
    }
  }

  private void assignColumnWidth(final State state, final int valueLength, final int columnIndex) {
    if (valueLength < 1) {
      return;
    }
    final Integer widthKey = Integer.valueOf(columnIndex);
    final Integer recWidth = state.mapColumn2RecommendedWidth.get(widthKey);
    final int existingWidth = recWidth != null ? recWidth.intValue() : 0;
    final int newWidth = Math.min(255, valueLength + 1);
    final int max = Math.max(existingWidth, newWidth);
    if (max == existingWidth || max < 1) {
      return;
    }
    // set new
    state.mapColumn2RecommendedWidth.put(widthKey, Integer.valueOf(max));
  }

  private ContentState determineContentState(final int rowCount, final RepresentationType representationType) {
    switch (representationType) {
    case COLLECTION_COMPLEX:
    case COLLECTION_ENTITY:
    case COLLECTION_PRIMITIVE:
    case COLLECTION_REFERENCE:
      if (rowCount == 0) {
        return ContentState.EMPTY_COLLECTION;
      }
      return ContentState.PRESENT;
    default:
      // single
      if (rowCount == 0) {
        return ContentState.NULL;
      }
      return ContentState.PRESENT;
    }
  }
}
