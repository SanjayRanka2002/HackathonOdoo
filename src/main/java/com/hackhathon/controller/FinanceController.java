package com.hackhathon.controller;

import com.hackhathon.entity.Expense;
import com.hackhathon.entity.FuelLog;
import com.hackhathon.entity.Vehicle;
import com.hackhathon.repository.ExpenseRepository;
import com.hackhathon.repository.FuelLogRepository;
import com.hackhathon.repository.VehicleRepository;
import com.hackhathon.service.AuditLogService;
import com.hackhathon.service.NotificationService;
import com.lowagie.text.Font;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/finance")
public class FinanceController {

    private final FuelLogRepository fuelLogRepository;
    private final ExpenseRepository expenseRepository;
    private final VehicleRepository vehicleRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public FinanceController(FuelLogRepository fuelLogRepository, 
                             ExpenseRepository expenseRepository, 
                             VehicleRepository vehicleRepository,
                             AuditLogService auditLogService,
                             NotificationService notificationService) {
        this.fuelLogRepository = fuelLogRepository;
        this.expenseRepository = expenseRepository;
        this.vehicleRepository = vehicleRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listFinance(Model model) {
        List<FuelLog> fuelLogs = fuelLogRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();
        
        double totalFuelCost = fuelLogs.stream().mapToDouble(FuelLog::getCost).sum();
        double totalExpenseCost = expenses.stream().mapToDouble(Expense::getAmount).sum();

        model.addAttribute("fuelLogs", fuelLogs);
        model.addAttribute("expenses", expenses);
        model.addAttribute("totalFuelCost", totalFuelCost);
        model.addAttribute("totalExpenseCost", totalExpenseCost);
        model.addAttribute("grandTotal", totalFuelCost + totalExpenseCost);

        return "finance/list";
    }

    @GetMapping("/fuel/new")
    public String showFuelForm(Model model) {
        model.addAttribute("fuelLog", new FuelLog());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "finance/fuel_form";
    }

    @PostMapping("/fuel/new")
    public String createFuelLog(@Valid @ModelAttribute("fuelLog") FuelLog fuelLog,
                                BindingResult result,
                                @RequestParam("vehicleId") Long vehicleId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicleRepository.findAll());
            return "finance/fuel_form";
        }

        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        if (vOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected vehicle not found");
            return "redirect:/finance/fuel/new";
        }

        fuelLog.setVehicle(vOpt.get());
        fuelLog.setFuelDate(LocalDateTime.now());
        fuelLogRepository.save(fuelLog);

        auditLogService.log("FUEL_LOG_CREATE", "Recorded fuel log for vehicle " + vOpt.get().getVehicleName() + ": " + fuelLog.getQuantity() + " Liters, Cost: ₹" + fuelLog.getCost());
        notificationService.createNotification("Fuel log recorded: " + vOpt.get().getVehicleName() + " (" + fuelLog.getQuantity() + "L)", "ADMIN");

        // Optional: Update vehicle odometer reading
        Vehicle vehicle = vOpt.get();
        if (fuelLog.getOdometerReading() != null && fuelLog.getOdometerReading() > vehicle.getCurrentOdometer()) {
            vehicle.setCurrentOdometer(fuelLog.getOdometerReading());
            vehicleRepository.save(vehicle);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Fuel log recorded successfully!");
        return "redirect:/finance";
    }

    @GetMapping("/expense/new")
    public String showExpenseForm(Model model) {
        model.addAttribute("expense", new Expense());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "finance/expense_form";
    }

    @PostMapping("/expense/new")
    public String createExpense(@Valid @ModelAttribute("expense") Expense expense,
                                BindingResult result,
                                @RequestParam("vehicleId") Long vehicleId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicleRepository.findAll());
            return "finance/expense_form";
        }

        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        if (vOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected vehicle not found");
            return "redirect:/finance/expense/new";
        }

        expense.setVehicle(vOpt.get());
        expense.setExpenseDate(LocalDate.now());
        expenseRepository.save(expense);

        auditLogService.log("EXPENSE_CREATE", "Recorded expense: " + expense.getCategory() + " for vehicle " + vOpt.get().getVehicleName() + ": ₹" + expense.getAmount());
        notificationService.createNotification("Expense logged: " + expense.getCategory() + " - ₹" + expense.getAmount(), "ADMIN");

        redirectAttributes.addFlashAttribute("successMessage", "Expense log recorded successfully!");
        return "redirect:/finance";
    }

    @GetMapping("/report/pdf")
    public void generatePDFReport(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=TransitOps_Financial_Report.pdf";
        response.setHeader(headerKey, headerValue);

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Title styling
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        titleFont.setSize(22);
        titleFont.setColor(new Color(99, 102, 241));
        Paragraph title = new Paragraph("TransitOps Cost & Expense Report", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Paragraph timestamp = new Paragraph("Generated on: " + LocalDateTime.now().toString());
        timestamp.setAlignment(Paragraph.ALIGN_RIGHT);
        timestamp.setSpacingAfter(30);
        document.add(timestamp);

        // Add summary paragraph
        Font termFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        termFont.setSize(12);
        Paragraph summaryHeader = new Paragraph("EXECUTIVE FINANCIAL SUMMARY\n\n", termFont);
        document.add(summaryHeader);

        List<FuelLog> fuelLogs = fuelLogRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();
        double totalFuelCost = fuelLogs.stream().mapToDouble(FuelLog::getCost).sum();
        double totalExpenseCost = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double grandTotal = totalFuelCost + totalExpenseCost;

        Paragraph stats = new Paragraph("Total Fuel Cost: ₹" + totalFuelCost + "\n" +
                                        "Total Ancillary Expenses: ₹" + totalExpenseCost + "\n" +
                                        "Grand Total Expenditures: ₹" + grandTotal + "\n\n\n");
        document.add(stats);

        // Add table
        Paragraph tableHeader = new Paragraph("ANCILLARY EXPENSES DETAILS", termFont);
        tableHeader.setSpacingAfter(10);
        document.add(tableHeader);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(17, 24, 39));
        cell.setPadding(8);
        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        font.setColor(Color.WHITE);

        cell.setPhrase(new Paragraph("Date", font));
        table.addCell(cell);
        cell.setPhrase(new Paragraph("Vehicle", font));
        table.addCell(cell);
        cell.setPhrase(new Paragraph("Category", font));
        table.addCell(cell);
        cell.setPhrase(new Paragraph("Amount (₹)", font));
        table.addCell(cell);

        for (Expense exp : expenses) {
            table.addCell(exp.getExpenseDate().toString());
            table.addCell(exp.getVehicle() != null ? exp.getVehicle().getVehicleName() : "N/A");
            table.addCell(exp.getCategory());
            table.addCell(String.valueOf(exp.getAmount()));
        }

        document.add(table);
        document.close();
    }

    @GetMapping("/report/excel")
    public void generateExcelReport(HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=TransitOps_Financials.xlsx";
        response.setHeader(headerKey, headerValue);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Expenses & Fuel Log");

        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Type");
        cell = row.createCell(1);
        cell.setCellValue("Date");
        cell = row.createCell(2);
        cell.setCellValue("Vehicle");
        cell = row.createCell(3);
        cell.setCellValue("Detail/Category");
        cell = row.createCell(4);
        cell.setCellValue("Cost/Amount (₹)");

        int rowNum = 1;
        List<FuelLog> fuelLogs = fuelLogRepository.findAll();
        for (FuelLog log : fuelLogs) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue("FUEL LOG");
            dataRow.createCell(1).setCellValue(log.getFuelDate().toString());
            dataRow.createCell(2).setCellValue(log.getVehicle() != null ? log.getVehicle().getVehicleName() : "N/A");
            dataRow.createCell(3).setCellValue("Quantity: " + log.getQuantity() + " Liters");
            dataRow.createCell(4).setCellValue(log.getCost());
        }

        List<Expense> expenses = expenseRepository.findAll();
        for (Expense exp : expenses) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue("EXPENSE");
            dataRow.createCell(1).setCellValue(exp.getExpenseDate().toString());
            dataRow.createCell(2).setCellValue(exp.getVehicle() != null ? exp.getVehicle().getVehicleName() : "N/A");
            dataRow.createCell(3).setCellValue(exp.getCategory() + " - " + exp.getDescription());
            dataRow.createCell(4).setCellValue(exp.getAmount());
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
