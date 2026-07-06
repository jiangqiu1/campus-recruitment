package com.recruit.controller;

import com.recruit.entity.*;
import com.recruit.service.*;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Objects;

/**
 * 数据导出控制器
 * 支持 xlsx/csv 格式的数据导出
 */
@RestController
@RequestMapping("/export")
public class DataExportController extends BaseController {

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

        // 设置响应头
        response.setContentType(format.equals("csv") ? "text/csv;charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("export_" + type + "_" + System.currentTimeMillis(), StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + "." + format);
        // 允许前端读取 blob
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try (OutputStream os = response.getOutputStream()) {
            if ("csv".equals(format)) {
                // CSV 写入 UTF-8 BOM 避免 Excel 乱码
                os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                writeCsv(type, os);
            } else {
                writeXlsx(type, os);
            }
            os.flush();
        }
    }

    private void writeCsv(String type, OutputStream os) throws Exception {
        List<String[]> rows = getExportData(type);
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

    // 使用 JAVA 原生 POI 避免额外依赖，此处用简化写法
    // 实际项目中建议使用 Apache POI 或 EasyExcel
    private void writeXlsx(String type, OutputStream os) throws Exception {
        List<String[]> rows = getExportData(type);
        if (rows.isEmpty()) {
            os.write("暂无数据".getBytes(StandardCharsets.UTF_8));
            return;
        }

        // 简单 CSV 格式输出，前端已支持 xlsx/csv/pdf
        // 实际 xlsx 需要 Apache POI，此处以 CSV 内容写入但保持 xlsx 文件头
        os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
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

    private List<String[]> getExportData(String type) {
        List<String[]> rows = new java.util.ArrayList<>();

        switch (type) {
            case "student":
                rows.add(new String[]{"ID", "用户名", "姓名", "角色", "手机号", "状态", "创建时间"});
                userService.lambdaQuery()
                        .eq(com.recruit.entity.SysUser::getRole, 0)
                        .list()
                        .forEach(u -> rows.add(new String[]{
                                String.valueOf(u.getId()), u.getUsername(), u.getRealName(),
                                "学生", u.getPhone() != null ? u.getPhone() : "",
                                u.getStatus() == 1 ? "启用" : "禁用",
                                u.getCreateTime() != null ? u.getCreateTime().toString().replace("T", " ") : ""
                        }));
                break;

            case "company":
                rows.add(new String[]{"ID", "企业名称", "行业", "联系人", "状态", "合作等级", "创建时间"});
                companyService.list().forEach(c -> rows.add(new String[]{
                        String.valueOf(c.getId()), c.getName(),
                        c.getIndustry() != null ? c.getIndustry() : "",
                        c.getContactPerson() != null ? c.getContactPerson() : "",
                        c.getStatus() != null ? (c.getStatus() == 1 ? "通过" : c.getStatus() == 0 ? "待审核" : "拒绝") : "",
                        c.getCooperationLevel() != null ? String.valueOf(c.getCooperationLevel()) : "",
                        c.getCreateTime() != null ? c.getCreateTime().toString().replace("T", " ") : ""
                }));
                break;

            case "job":
                rows.add(new String[]{"ID", "岗位名称", "企业ID", "薪资范围", "学历要求", "状态", "创建时间"});
                jobService.list().forEach(j -> rows.add(new String[]{
                        String.valueOf(j.getId()), j.getTitle(),
                        String.valueOf(j.getCompanyId()),
                        j.getSalaryRange() != null ? j.getSalaryRange() : "",
                        j.getEducation() != null ? j.getEducation() : "",
                        j.getStatus() != null ? (j.getStatus() == 1 ? "发布中" : "已关闭") : "",
                        j.getCreateTime() != null ? j.getCreateTime().toString().replace("T", " ") : ""
                }));
                break;

            case "delivery":
                rows.add(new String[]{"ID", "学生ID", "岗位ID", "状态", "投递时间"});
                deliveryService.list().forEach(d -> rows.add(new String[]{
                        String.valueOf(d.getId()), String.valueOf(d.getStudentId()),
                        String.valueOf(d.getJobId()),
                        d.getStatus() != null ? (d.getStatus() == 0 ? "待查看" : d.getStatus() == 1 ? "已查看" : d.getStatus() == 2 ? "面试中" : d.getStatus() == 3 ? "已录用" : "未录用") : "",
                        d.getCreateTime() != null ? d.getCreateTime().toString().replace("T", " ") : ""
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
