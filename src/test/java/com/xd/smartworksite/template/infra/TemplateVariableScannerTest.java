package com.xd.smartworksite.template.infra;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateVariableScannerTest {

    private final TemplateVariableScanner scanner = new TemplateVariableScanner();

    @Test
    void scansDocxInDocumentOrderAndRecognizesVariablesSplitAcrossRuns() throws Exception {
        byte[] documentBytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("项目：{{ var_");
            paragraph.createRun().setText("project_name }}");
            XWPFTable table = document.createTable(1, 1);
            table.getRow(0).getCell(0).setText("日期：{{var_report_date}}");
            document.createParagraph().createRun().setText("重复：{{ var_project_name }}");
            document.write(outputStream);
            documentBytes = outputStream.toByteArray();
        }

        assertThat(scanner.scan("template.docx", new ByteArrayInputStream(documentBytes)))
                .containsExactly("var_project_name", "var_report_date");
    }

    @Test
    void scansExcelBySheetRowAndCellOrder() throws Exception {
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row firstRow = workbook.createSheet("第一页").createRow(0);
            firstRow.createCell(0).setCellValue("{{ var_first }}");
            firstRow.createCell(1).setCellValue("{{var_second}}");
            workbook.createSheet("第二页").createRow(0).createCell(0).setCellValue("{{ var_third }}");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        assertThat(scanner.scan("template.xlsx", new ByteArrayInputStream(workbookBytes)))
                .containsExactly("var_first", "var_second", "var_third");
    }

    @Test
    void scansOnlyNormalizedVarPlaceholdersAndDeduplicatesThem() throws Exception {
        String content = "{{ var_one }} ${var_old} {{ name }} {{ VAR_UPPER }} {{var_one}} {{var_two}}";

        assertThat(scanner.scan("template.md", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))))
                .containsExactly("var_one", "var_two");
    }

    @Test
    void rejectsPdfTemplates() {
        assertThatThrownBy(() -> scanner.scan("template.pdf", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported template format");
    }
}
