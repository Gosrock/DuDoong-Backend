package band.gosrock.excel;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ExcelOrderHelper {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public Workbook execute(List<ExcelOrderDto> excelOrders){
        Workbook workbook = new XSSFWorkbook();

        Field[] declaredFields = ExcelOrderDto.class.getDeclaredFields();

        List<String> fieldNames = Arrays.stream(declaredFields).map(Field::getName).toList();
        Sheet sheet = workbook.createSheet("orderList");
//        sheet.setColumnWidth(0, 6000);
//        sheet.setColumnWidth(1, 4000);
        // create header
        Row header = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        headerStyle.setFont(font);

        Cell headerCell0 = header.createCell(0);
        headerCell0.setCellValue("주문번호");
        headerCell0.setCellStyle(headerStyle);

        Cell headerCell1 = header.createCell(1);
        headerCell1.setCellValue("주문 방식");
        headerCell1.setCellStyle(headerStyle);

        Cell headerCell2 = header.createCell(2);
        headerCell2.setCellValue("주문 상태");
        headerCell2.setCellStyle(headerStyle);

        Cell headerCell3 = header.createCell(3);
        headerCell3.setCellValue("주문 이름");
        headerCell3.setCellStyle(headerStyle);

        Cell headerCell4 = header.createCell(4);
        headerCell4.setCellValue("주문자 아이디");
        headerCell4.setCellStyle(headerStyle);

        Cell headerCell5 = header.createCell(5);
        headerCell5.setCellValue("총액");
        headerCell5.setCellStyle(headerStyle);

        Cell headerCell6 = header.createCell(6);
        headerCell6.setCellValue("티켓 수량");
        headerCell6.setCellStyle(headerStyle);

        Cell headerCell7 = header.createCell(7);
        headerCell7.setCellValue("생성 일시");
        headerCell7.setCellStyle(headerStyle);

        Cell headerCell8 = header.createCell(8);
        headerCell8.setCellValue("환불 일시");
        headerCell8.setCellStyle(headerStyle);

        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        IntStream.range(0, excelOrders.size())
            .forEach(idx ->
                {
                    Row row = sheet.createRow(idx + 1);
                    ExcelOrderDto excelOrderDto = excelOrders.get(idx);
                    row.createCell(0).setCellValue(excelOrderDto.getOrderNo());

                    row.createCell(1).setCellValue(excelOrderDto.getOrderMethod().getKr());

                    row.createCell(2).setCellValue(excelOrderDto.getOrderStatus().getKr());

                    row.createCell(3).setCellValue(excelOrderDto.getOrderName());

                    row.createCell(4).setCellValue(excelOrderDto.getUserId());

                    row.createCell(5).setCellValue(excelOrderDto.getAmount().toString());

                    row.createCell(6).setCellValue(excelOrderDto.getQuantity());

                    row.createCell(7).setCellValue(excelOrderDto.getCreatedAt().format(formatter));

                    LocalDateTime refundAt = excelOrderDto.getRefundAt();
                    row.createCell(8).setCellValue(refundAt != null ? refundAt.format(formatter) : null);
                }
            )
        ;
        return workbook;
    }
}
