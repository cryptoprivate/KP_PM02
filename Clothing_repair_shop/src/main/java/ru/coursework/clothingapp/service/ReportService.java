package ru.coursework.clothingapp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ru.coursework.clothingapp.model.Order;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public String generateOrdersReport(List<Order> orders, String title) {
        String dir = "reports";
        String fileName = "orders_" + System.currentTimeMillis() + ".pdf";
        String path = dir + "/" + fileName;

        try {
            new File(dir).mkdirs();
            createPdf(new File(path), title, orders);
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания PDF: " + e.getMessage(), e);
        }
    }

    private void createPdf(File file, String title, List<Order> orders) throws Exception {
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, new FileOutputStream(file));

        BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font font = new Font(bf, 10);
        Font titleFont = new Font(bf, 14, Font.BOLD);
        Font bold = new Font(bf, 10, Font.BOLD);

        doc.open();
        doc.add(new Paragraph(title, titleFont));
        doc.add(new Paragraph("Дата формирования: " + LocalDateTime.now().format(DATE_TIME_FORMAT), font));
        doc.add(new Paragraph("Всего заказов: " + orders.size(), font));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2, 3, 2, 3, 2, 2});

        addCell(table, "№", font, true);
        addCell(table, "Номер заказа", font, true);
        addCell(table, "Клиент", font, true);
        addCell(table, "Статус", font, true);
        addCell(table, "Мастер", font, true);
        addCell(table, "Дата приёма", font, true);
        addCell(table, "Сумма (руб.)", font, true);

        int num = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            addCell(table, String.valueOf(num++), font, false);
            addCell(table, o.getOrderNumber(), font, false);
            addCell(table, o.getClient().toString(), font, false);
            addCell(table, o.getStatus().getName(), font, false);
            addCell(table, o.getMaster() != null ? o.getMaster().toString() : "—", font, false);
            addCell(table, o.getAcceptDate().format(DATE_FORMAT), font, false);
            addCell(table, String.format("%.2f", o.getTotalCost()), font, false);
            total = total.add(o.getTotalCost());
        }

        PdfPCell labelCell = new PdfPCell(new Phrase("Итого:", bold));
        labelCell.setColspan(6);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell amtCell = new PdfPCell(new Phrase(String.format("%.2f", total), bold));
        amtCell.setPadding(5);
        table.addCell(amtCell);

        doc.add(table);
        doc.close();
    }

    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        table.addCell(cell);
    }
}
