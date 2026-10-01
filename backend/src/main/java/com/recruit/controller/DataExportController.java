package com.recruit.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.recruit.entity.*;
import com.recruit.service.*;
import com.recruit.utils.AESUtil;
import com.recruit.utils.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据导出控制器
 * 支持 xlsx / csv / pdf 格式的数据导出
 */
@RestController
@RequestMapping("/export")
public class DataExportController extends BaseController {

    /**
     * 单次导出最大行数：防止全表数据驻留内存（XLSX/PDF 仍需整表构建）
     */
    private static final int MAX_EXPORT_ROWS = 5000;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private JobService jobService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private AiParseLogService aiParseLogService;

    @Autowired
    private AESUtil aesUtil;

    @PostMapping
    public void export(@RequestBody ExportRequest request, HttpServletResponse response) throws Exception {
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 1) && !Objects.equals(role, 3)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"无导出权限\",\"data\":null}");
            return;
        }
        String type = request.getType();
        String format = request.getFormat() != null ? request.getFormat() : "xlsx";
        String filter = request.getFilter() != null ? request.getFilter().trim() : "";

        String contentType;
        String extension;
        switch (format) {
            case "pdf":
                contentType = "application/pdf";
                extension = "pdf";
                break;
            case "csv":
                contentType = "text/csv;charset=UTF-8";
                extension = "csv";
                break;
            default:
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = "xlsx";
        }

        response.setContentType(contentType);
        String filename = URLEncoder.encode("export_" + type + "_" + System.currentTimeMillis(), StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + "." + extension);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try (OutputStream os = response.getOutputStream()) {
            List<String[]> data = getExportData(type, filter);
            switch (format) {
                case "pdf":
                    writePdf(data, os);
                    break;
                case "csv":
                    os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                    writeCsv(data, os);
                    break;
                default:
                    writeXlsx(data, os);
            }
            os.flush();
        }
    }

    // ==================== CSV ====================

    private void writeCsv(List<String[]> rows, OutputStream os) throws Exception {
        for (String[] row : rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(",");
                String val = row[i] != null ? row[i].replace("\"", "\"\"") : "";
                sb.append("\"").append(val).append("\"");
            }
            sb.append("\n");
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    // ==================== XLSX (Apache POI) ====================

    private void writeXlsx(List<String[]> rows, OutputStream os) throws Exception {
        SXSSFWorkbook wb = new SXSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("导出数据");

        // 表头样式
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 数据行样式
        CellStyle dataStyle = wb.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        if (rows.isEmpty()) {
            org.apache.poi.ss.usermodel.Row r = sheet.createRow(0);
            r.createCell(0).setCellValue("暂无数据");
        } else {
            // 表头行
            String[] header = rows.get(0);
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(header[i]);
                cell.setCellStyle(headerStyle);
            }
            // 数据行
            for (int r = 1; r < rows.size(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r);
                String[] cols = rows.get(r);
                for (int c = 0; c < cols.length; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    cell.setCellValue(cols[c]);
                    cell.setCellStyle(dataStyle);
                }
            }
            // 自动调整列宽（最大 50）
            for (int i = 0; i < header.length; i++) {
                int maxWidth = 50 * 256;
                int contentWidth = 0;
                for (int r = 0; r < rows.size(); r++) {
                    String val = rows.get(r)[i];
                    if (val != null) {
                        int len = 0;
                        for (char ch : val.toCharArray()) {
                            len += ch > 0xFF ? 2 : 1;
                        }
                        contentWidth = Math.max(contentWidth, len * 256 + 200);
                    }
                }
                contentWidth = Math.min(contentWidth, maxWidth);
                if (contentWidth > sheet.getColumnWidth(i)) {
                    sheet.setColumnWidth(i, contentWidth);
                }
            }
        }

        wb.write(os);
        wb.close();
        wb.dispose();
    }

    // ==================== PDF (OpenPDF) ====================

    private void writePdf(List<String[]> rows, OutputStream os) throws Exception {
        // 加载中文字体（优先 Windows 系统字体）
        com.lowagie.text.Font headerFont = loadChineseFont(10, com.lowagie.text.Font.BOLD, new Color(255, 255, 255));
        com.lowagie.text.Font dataFont = loadChineseFont(9, com.lowagie.text.Font.NORMAL, null);
        com.lowagie.text.Font emptyFont = loadChineseFont(9, com.lowagie.text.Font.NORMAL, null);

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, os);
        document.open();

        if (rows.isEmpty()) {
            document.add(new Paragraph("暂无数据", emptyFont));
        } else {
            String[] header = rows.get(0);
            PdfPTable table = new PdfPTable(header.length);
            table.setWidthPercentage(100);

            // 表头
            for (String h : header) {
                PdfPCell cell = new PdfPCell(new Phrase(h != null ? h : "", headerFont));
                cell.setBackgroundColor(new Color(41, 98, 255));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // 数据行
            boolean alternate = false;
            for (int r = 1; r < rows.size(); r++) {
                String[] cols = rows.get(r);
                for (String val : cols) {
                    PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", dataFont));
                    cell.setPadding(4);
                    if (alternate) {
                        cell.setGrayFill(0.95f);
                    }
                    table.addCell(cell);
                }
                alternate = !alternate;
            }

            document.add(table);
        }

        document.close();
    }

    /**
     * 中文字体缓存：BaseFont 线程安全可共享，首次加载后复用，避免每次导出重读磁盘。
     * 全部候选路径都失败时置 failed 标记，后续直接走 Helvetica fallback。
     */
    private static volatile BaseFont cachedChineseBaseFont;
    private static volatile boolean chineseBaseFontFailed = false;

    /**
     * 加载中文字体，按常见系统字体路径尝试
     */
    private com.lowagie.text.Font loadChineseFont(float size, int style, Color color) throws Exception {
        BaseFont baseFont = getChineseBaseFont();
        if (baseFont != null) {
            return color != null
                    ? new com.lowagie.text.Font(baseFont, size, style, color)
                    : new com.lowagie.text.Font(baseFont, size, style);
        }
        // fallback：使用 Helvetica，但中文可能无法显示
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private BaseFont getChineseBaseFont() {
        if (cachedChineseBaseFont != null) return cachedChineseBaseFont;
        if (chineseBaseFontFailed) return null;
        synchronized (DataExportController.class) {
            if (cachedChineseBaseFont != null) return cachedChineseBaseFont;
            if (chineseBaseFontFailed) return null;
            String[] candidates = {
                    "C:\\Windows\\Fonts\\msyh.ttc,0",
                    "C:\\Windows\\Fonts\\msyhbd.ttc,0",
                    "C:\\Windows\\Fonts\\simhei.ttf",
                    "C:\\Windows\\Fonts\\simsun.ttc,0",
                    "C:\\Windows\\Fonts\\simsun.ttf",
                    "C:\\Windows\\Fonts\\arialuni.ttf"
            };
            for (String fontPath : candidates) {
                try {
                    cachedChineseBaseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return cachedChineseBaseFont;
                } catch (Exception ignored) {
                    // 尝试下一个字体
                }
            }
            chineseBaseFontFailed = true;
            return null;
        }
    }

    // ==================== 数据获取 ====================

    /**
     * 解密敏感字段（手机号/联系电话），为空或解密失败时返回原值
     */
    private String decryptSafely(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            return aesUtil.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }

    private List<String[]> getExportData(String type, String filter) {
        List<String[]> rows = new ArrayList<>();
        String lowerFilter = filter.toLowerCase();

        switch (type) {
            case "student":
                rows.add(new String[]{"ID", "用户名", "姓名", "角色", "手机号", "状态", "创建时间"});
                userService.lambdaQuery()
                        .eq(SysUser::getRole, 0)
                        .last("LIMIT " + MAX_EXPORT_ROWS)
                        .list()
                        .stream()
                        .filter(u -> filter.isEmpty()
                                || u.getRealName() != null && u.getRealName().toLowerCase().contains(lowerFilter)
                                || u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerFilter)
                                || decryptSafely(u.getPhone()).contains(filter))
                        .forEach(u -> rows.add(new String[]{
                                String.valueOf(u.getId()), u.getUsername(), u.getRealName(),
                                "学生", decryptSafely(u.getPhone()),
                                u.getStatus() == 1 ? "启用" : "禁用",
                                u.getCreateTime() != null ? u.getCreateTime().toString().replace("T", " ") : ""
                        }));
                break;

            case "company":
                rows.add(new String[]{"ID", "企业名称", "行业", "联系人", "联系电话", "状态", "合作等级", "创建时间"});
                companyService.lambdaQuery()
                        .last("LIMIT " + MAX_EXPORT_ROWS)
                        .list()
                        .stream()
                        .filter(c -> filter.isEmpty()
                                || c.getName() != null && c.getName().toLowerCase().contains(lowerFilter)
                                || c.getIndustry() != null && c.getIndustry().toLowerCase().contains(lowerFilter)
                                || c.getContactPerson() != null && c.getContactPerson().toLowerCase().contains(lowerFilter))
                        .forEach(c -> rows.add(new String[]{
                                String.valueOf(c.getId()), c.getName(),
                                c.getIndustry() != null ? c.getIndustry() : "",
                                c.getContactPerson() != null ? c.getContactPerson() : "",
                                decryptSafely(c.getContactPhone()),
                                c.getStatus() != null ? (c.getStatus() == 1 ? "通过" : c.getStatus() == 0 ? "待审核" : "拒绝") : "",
                                c.getCooperationLevel() != null ? String.valueOf(c.getCooperationLevel()) : "",
                                c.getCreateTime() != null ? c.getCreateTime().toString().replace("T", " ") : ""
                        }));
                break;

            case "job":
                rows.add(new String[]{"ID", "岗位名称", "企业ID", "薪资范围", "学历要求", "工作地点", "状态", "创建时间"});
                jobService.lambdaQuery()
                        .last("LIMIT " + MAX_EXPORT_ROWS)
                        .list()
                        .stream()
                        .filter(j -> filter.isEmpty()
                                || j.getTitle() != null && j.getTitle().toLowerCase().contains(lowerFilter)
                                || j.getEducation() != null && j.getEducation().toLowerCase().contains(lowerFilter)
                                || j.getLocation() != null && j.getLocation().toLowerCase().contains(lowerFilter))
                        .forEach(j -> rows.add(new String[]{
                                String.valueOf(j.getId()), j.getTitle(),
                                String.valueOf(j.getCompanyId()),
                                j.getSalaryRange() != null ? j.getSalaryRange() : "",
                                j.getEducation() != null ? j.getEducation() : "",
                                j.getLocation() != null ? j.getLocation() : "",
                                j.getStatus() != null ? (j.getStatus() == 1 ? "发布中" : "已关闭") : "",
                                j.getCreateTime() != null ? j.getCreateTime().toString().replace("T", " ") : ""
                        }));
                break;

            case "delivery":
                rows.add(new String[]{"ID", "学生ID", "岗位ID", "状态", "投递时间"});
                deliveryService.lambdaQuery()
                        .last("LIMIT " + MAX_EXPORT_ROWS)
                        .list()
                        .stream()
                        .filter(d -> filter.isEmpty()
                                || String.valueOf(d.getStudentId()).contains(filter)
                                || String.valueOf(d.getJobId()).contains(filter))
                        .forEach(d -> rows.add(new String[]{
                                String.valueOf(d.getId()), String.valueOf(d.getStudentId()),
                                String.valueOf(d.getJobId()),
                                d.getStatus() != null ? (d.getStatus() == 0 ? "待查看" : d.getStatus() == 1 ? "已查看" : d.getStatus() == 2 ? "面试中" : d.getStatus() == 3 ? "已录用" : "未录用") : "",
                                d.getCreateTime() != null ? d.getCreateTime().toString().replace("T", " ") : ""
                        }));
                break;

            case "resume":
                rows.add(new String[]{"ID", "学生ID", "学生姓名", "学历", "技能", "求职意向", "更新时间"});
                List<Resume> resumeList = resumeService.lambdaQuery()
                        .last("LIMIT " + MAX_EXPORT_ROWS)
                        .list()
                        .stream()
                        .filter(r -> filter.isEmpty()
                                || String.valueOf(r.getStudentId()).contains(filter)
                                || r.getEducation() != null && r.getEducation().toLowerCase().contains(lowerFilter)
                                || r.getJobTarget() != null && r.getJobTarget().toLowerCase().contains(lowerFilter))
                        .collect(Collectors.toList());
                // 批量取学生姓名，替代循环内逐条 getById
                List<Long> resumeStudentIds = resumeList.stream()
                        .map(Resume::getStudentId).filter(Objects::nonNull).distinct()
                        .collect(Collectors.toList());
                Map<Long, SysUser> studentMap = resumeStudentIds.isEmpty() ? Collections.emptyMap()
                        : userService.listByIds(resumeStudentIds).stream()
                                .collect(Collectors.toMap(SysUser::getId, u -> u));
                for (Resume r : resumeList) {
                    SysUser student = r.getStudentId() != null ? studentMap.get(r.getStudentId()) : null;
                    String studentName = student != null && student.getRealName() != null ? student.getRealName() : "";
                    rows.add(new String[]{
                            String.valueOf(r.getId()), String.valueOf(r.getStudentId()),
                            studentName,
                            r.getEducation() != null ? r.getEducation() : "",
                            r.getSkills() != null ? r.getSkills() : "",
                            r.getJobTarget() != null ? r.getJobTarget() : "",
                            r.getUpdateTime() != null ? r.getUpdateTime().toString().replace("T", " ") : ""
                    });
                }
                break;

            case "ai":
                rows.add(new String[]{"ID", "调用时间", "AI提供方", "任务类型", "耗时(ms)", "是否降级", "用户ID"});
                aiParseLogService.lambdaQuery()
                        .orderByDesc(AiParseLog::getCreateTime)
                        .last("LIMIT " + MAX_EXPORT_ROWS)
                        .list()
                        .forEach(l -> rows.add(new String[]{
                                String.valueOf(l.getId()),
                                l.getCreateTime() != null ? l.getCreateTime().toString().replace("T", " ") : "",
                                l.getProvider() != null ? l.getProvider() : "",
                                l.getTaskName() != null ? l.getTaskName() : "",
                                l.getLatencyMs() != null ? String.valueOf(l.getLatencyMs()) : "",
                                l.getMockFlag() != null && l.getMockFlag() == 1 ? "是" : "否",
                                l.getUserId() != null ? String.valueOf(l.getUserId()) : ""
                        }));
                break;

            default:
                rows.add(new String[]{"不支持的导出类型"});
        }

        return rows;
    }

    /**
     * 导出请求体
     */
    public static class ExportRequest {
        private String type;
        private String filter;
        private String format;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFilter() { return filter; }
        public void setFilter(String filter) { this.filter = filter; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
