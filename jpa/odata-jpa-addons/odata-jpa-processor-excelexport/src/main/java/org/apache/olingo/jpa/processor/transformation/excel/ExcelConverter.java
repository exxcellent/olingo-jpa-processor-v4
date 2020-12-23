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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADescribedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
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

  /**
   * Helper class to transport state/process related information into methods...
   *
   */
  private static class WorkbookState {
    private final Workbook workbook;
    Map<JPAEntityType, SheetState> sheets = new HashMap<>();
    short shortBestFontHeight = XSSFFont.DEFAULT_FONT_SIZE;
    Map<EdmPrimitiveTypeKind, CellStyle> mapDatatypeCellStyle = new HashMap<>();
    Map<Integer, Integer> mapColumn2RecommendedWidth = new HashMap<>();

    public WorkbookState(final Workbook workbook) {
      this.workbook = workbook;
    }
  }

  private static class SheetState {
    private final WorkbookState workbookState;
    private final JPAEntityType jpaType;
    private final Sheet sheet;
    @SuppressWarnings("unused")
    boolean isNewCreated = false;
    Map<String, Integer> mapAlias2ColumnIndex = null;

    public SheetState(final WorkbookState ws, final JPAEntityType jpaType, final Sheet sheet) {
      this.workbookState = ws;
      this.jpaType = jpaType;
      this.sheet = sheet;
    }
  }

  private final static ValueConverter CONVERTER = new ValueConverter();
  private final Configuration configuration;

  public ExcelConverter(final Configuration configuration) {
    this.configuration = configuration != null ? configuration : new Configuration();
  }

  private WorkbookState createWorkbook() throws IOException {
    return new WorkbookState(new SXSSFWorkbook());
  }

  private SheetState findOrCreateSheet(final WorkbookState workbookState, final JPAEntityType jpaType) {
    SheetState sheetState = workbookState.sheets.get(jpaType);
    if (sheetState != null) {
      return sheetState;
    }
    final Workbook workbook = workbookState.workbook;
    final String sheetName = configuration.getSheetName(jpaType);
    final Sheet sheet = workbook.createSheet(sheetName);
    sheetState = new SheetState(workbookState, jpaType, sheet);
    sheetState.isNewCreated = true;
    workbookState.sheets.put(jpaType, sheetState);
    return sheetState;
  }

  private void createHeaderRow(final SheetState sheetState, final int rowNumber, final Tuple dbRow) {
    final Row headerRow = sheetState.sheet.createRow(rowNumber);
    final CellStyle cellStyle = sheetState.sheet.getWorkbook().createCellStyle();
    final org.apache.poi.ss.usermodel.Font font = sheetState.workbookState.workbook.createFont();
    font.setBold(true);
    cellStyle.setBorderBottom(BorderStyle.THIN);
    cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    font.setFontHeightInPoints(sheetState.workbookState.shortBestFontHeight);
    if (configuration.getFontName() != null) {
      font.setFontName(configuration.getFontName());
    }
    cellStyle.setFont(font);
    for (final TupleElement<?> dbCell : dbRow.getElements()) {
      final String dbAlias = dbCell.getAlias();
      if (configuration.isSuppressedColumn(sheetState.jpaType, dbAlias)) {
        continue;
      }
      final String customName = configuration.getCustomColumnName(sheetState.jpaType, dbAlias);
      final int headerColumnIndex = sheetState.mapAlias2ColumnIndex.get(dbAlias).intValue();
      final String excelName = customName != null ? customName : dbAlias;
      assignColumnWidth(sheetState.workbookState, excelName.length(), headerColumnIndex);
      final Cell cell = headerRow.createCell(headerColumnIndex);
      cell.setCellValue(excelName);
      cell.setCellStyle(cellStyle);
    }
  }

  private void assignValueCellStyle(final WorkbookState state, final Cell cell, final EdmPrimitiveTypeKind dataType) {
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

  public final ODataResponseContent produceExcel(final QueryEntityResult input,
      final RepresentationType representationType)
          throws IOException, ODataJPAModelException, ODataJPAConversionException {

    final WorkbookState state = createWorkbook();
    final Workbook workbook = state.workbook;
    final SheetState sheetState = findOrCreateSheet(state, input.getEntityType());

    // adapt font size for larger data sets
    final int size = input.getQueryResult().size();
    if (size > 500) {
      state.shortBestFontHeight = (short) (state.shortBestFontHeight - 1);
    } else if (size > 1000) {
      state.shortBestFontHeight = (short)(state.shortBestFontHeight-2);
    }

    int countRows = 0;
    int rowNumber = sheetState.sheet.getFirstRowNum() < 0 ? 0 : sheetState.sheet.getFirstRowNum();
    boolean firstRow = true;
    for (final Tuple dbRow : input.getQueryResult()) {
      if (firstRow) {
        // build column order
        sheetState.mapAlias2ColumnIndex = buildColumnOrder(sheetState.jpaType, dbRow.getElements());
        // header
        if (configuration.isCreateHeaderRow()) {
          createHeaderRow(sheetState, rowNumber++, dbRow);
        }
        firstRow = false;
      }
      // data
      final Row row = sheetState.sheet.createRow(rowNumber++);
      for (final TupleElement<?> dbCell : dbRow.getElements()) {
        final String dbAlias = dbCell.getAlias();
        if (configuration.isSuppressedColumn(sheetState.jpaType, dbAlias)) {
          continue;
        }
        final Object value = dbRow.get(dbAlias);
        final int columnIndex = sheetState.mapAlias2ColumnIndex.get(dbAlias).intValue();
        processCell(sheetState, row, columnIndex, value, dbAlias);
      }
      countRows++;
    }
    // adjust column width from precalculated width
    for (final Map.Entry<Integer, Integer> entry : state.mapColumn2RecommendedWidth.entrySet()) {
      sheetState.sheet.setColumnWidth(entry.getKey().intValue(), entry.getValue().intValue() * 256);
    }

    // write out...
    final ByteArrayOutputStream outResult = new ByteArrayOutputStream(1024 * 1024);
    workbook.write(outResult);
    workbook.close();
    final InputStream isResult = new ByteArrayInputStream(outResult.toByteArray());

    final ContentState contentState = determineContentState(countRows, representationType);
    return new ODataResponseContent(contentState, isResult);
  }

  private Map<String, Integer> buildColumnOrder(final JPAEntityType jpaType, final List<TupleElement<?>> unsortedList) {
    // use copy constructor to modify map
    final Map<String, Integer> mapConfigured = new HashMap<>(configuration.getCustomColumnIndexes(jpaType));
    // determine minimum column index for unassigned columns
    int startingIndexForColumns = 0;
    for (final Integer i : mapConfigured.values()) {
      if (i.intValue() > startingIndexForColumns) {
        startingIndexForColumns = i.intValue() + 1;
      }
    }
    final Map<String, Integer> mapResult = new HashMap<>();
    for (final TupleElement<?> dbCell : unsortedList) {
      final Integer cI = mapConfigured.remove(dbCell.getAlias());
      if (cI != null) {
        mapResult.put(dbCell.getAlias(), cI);
      } else {
        mapResult.put(dbCell.getAlias(), Integer.valueOf(startingIndexForColumns++));
      }
    }
    if (!mapConfigured.isEmpty()) {
      LOG.warning("Assignments for column indexes contains unprocessed definitions: " + String.join(", ",
          mapConfigured.values().toArray(new String[mapConfigured.size()])));
    }
    if (mapResult.size() != unsortedList.size()) {
      throw new IllegalStateException("Column index map is not affecting the correct number of columns");
    }
    return mapResult;
  }

  private void processCell(final SheetState sheetState, final Row row, final int columnIndex,
      final Object value,
      final String dbAlias) throws ODataJPAModelException, ODataJPAConversionException {

    final JPASelector selector = sheetState.jpaType.getPath(dbAlias);
    final JPAAttribute<?> targetAttribute = selector.getLeaf();

    final ExcelCell excelCell = determineCellContent(sheetState.jpaType, dbAlias, targetAttribute, value);
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
        assignColumnWidth(sheetState.workbookState, Double.toString(dV).length(), columnIndex);
        break;
      case Date:
      case DateTimeOffset:
      case TimeOfDay:
        processTimeRelatedCell(sheetState.workbookState, cell, columnIndex, excelCell.getCellRepresentation(),
            odataValue);
        break;
      default:
        // String like...
        cell.setCellValue(odataValue.toString());
        assignColumnWidth(sheetState.workbookState, odataValue.toString().length(), columnIndex);
      }
    }
    assignValueCellStyle(sheetState.workbookState, cell, excelCell.getCellRepresentation());

  }

  private void processTimeRelatedCell(final WorkbookState state, final Cell cell, final int columnIndex,
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

  /**
   * @param entityType The root entity type defining the current working sheet the currently processed cell belongs to.
   * @param attributePath The database alias often representing a path through nested complex attributes to the
   * <i>targetAttribute</i>.
   * @param targetAttribute The meta model attribute the <i>value</i> is related to.
   * @param value The value from data base (after {@link javax.persistence.AttributeConverter attribute converter}) that
   * must be prepared for Excel sheet output.
   *
   * @return The combination of resulting value for Excel sheet (after any conversion) and data type assignment.
   */
  protected ExcelCell determineCellContent(final JPAEntityType entityType, final String attributePath,
      final JPAAttribute<?> targetAttribute, final Object value) {
    final EdmPrimitiveTypeKind kindOfCell = determineCellRepresentation(entityType, attributePath, targetAttribute);
    Object odataValue;
    if (JPADescribedElement.class.isInstance(targetAttribute)) {
      try {
        odataValue = CONVERTER.convertJPA2ODataPrimitiveValue((JPADescribedElement) targetAttribute, value);
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

  /**
   * @return The primitive data type of column to use for Excel cell formatting.
   * @see #determineCellContent(JPAEntityType, String, JPAAttribute, Object)
   */
  protected EdmPrimitiveTypeKind determineCellRepresentation(final JPAEntityType entityType, final String attributePath,
      final JPAAttribute<?> targetAttribute) {
    if (JPADescribedElement.class.isInstance(targetAttribute)) {
      try {
        return TypeMapping.convertToEdmSimpleType((JPADescribedElement) targetAttribute);
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

  private void assignColumnWidth(final WorkbookState state, final int valueLength, final int columnIndex) {
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
