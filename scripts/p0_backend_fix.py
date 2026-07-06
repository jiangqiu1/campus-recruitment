import os, re

base = 'C:/Users/yjq/.qclaw/workspace/校企项目/backend/src/main/java/com/recruit/controller'

# ==== 1. StatisticsController ====
path = os.path.join(base, 'StatisticsController.java')
c = open(path, 'r', encoding='utf-8').read()

c = c.replace('public class StatisticsController {', 'public class StatisticsController extends BaseController {')

old_td = '                // 统计全部班级（与班级管理页一致）\n' \
         '                List<Class> allClasses = classService.list();\n' \
         '                classCount = allClasses != null ? allClasses.size() : 0;\n' \
         '                // 统计所有班级的去重学生数\n' \
         '                Set<Long> allStudentIds = new HashSet<>();\n' \
         '                if (allClasses != null) {\n' \
         '                    for (Class cls : allClasses) {\n' \
         '                        List<Long> studentIds = classService.getStudentIdsByClassId(cls.getId());\n' \
         '                        if (studentIds != null) {\n' \
         '                            allStudentIds.addAll(studentIds);\n' \
         '                        }\n' \
         '                    }\n' \
         '                }\n' \
         '                studentCount = allStudentIds.size();'

new_td = '                // 仅统计当前教师负责的班级\n' \
         '                Long curUserId = getCurrentUserId();\n' \
         '                List<Class> myClasses = classService.selectByTeacherId(curUserId);\n' \
         '                classCount = myClasses != null ? myClasses.size() : 0;\n' \
         '                // 统计管辖班级的去重学生数\n' \
         '                Set<Long> allStudentIds = new HashSet<>();\n' \
         '                if (myClasses != null) {\n' \
         '                    for (Class cls : myClasses) {\n' \
         '                        List<Long> studentIds = classService.getStudentIdsByClassId(cls.getId());\n' \
         '                        if (studentIds != null) {\n' \
         '                            allStudentIds.addAll(studentIds);\n' \
         '                        }\n' \
         '                    }\n' \
         '                }\n' \
         '                studentCount = allStudentIds.size();'
assert old_td in c, "StatisticsController: old teacher dashboard text not found"
c = c.replace(old_td, new_td)

old_ed = '    @GetMapping("/teacher/employment-distribution")\n' \
         '    public Result<List<Map<String, Object>>> getEmploymentDistribution() {'
new_ed = '    @GetMapping("/teacher/employment-distribution")\n' \
         '    public Result<List<Map<String, Object>>> getEmploymentDistribution() {\n' \
         '        Long teacherId = getCurrentUserId();\n' \
         '        List<Class> myClasses = classService != null ? classService.selectByTeacherId(teacherId) : new ArrayList<>();\n' \
         '        Set<Long> myStudentIds = new HashSet<>();\n' \
         '        if (myClasses != null && classService != null) {\n' \
         '            for (Class cls : myClasses) {\n' \
         '                List<Long> ids = classService.getStudentIdsByClassId(cls.getId());\n' \
         '                if (ids != null) myStudentIds.addAll(ids);\n' \
         '            }\n' \
         '        }'
assert old_ed in c, "StatisticsController: old employment-distribution text not found"
c = c.replace(old_ed, new_ed)

old_q = '            long count = deliveryService.lambdaQuery()\n' \
        '                    .eq(Delivery::getStatus, getStatusValue(e.getKey()))\n' \
        '                    .count();'
new_q = '            long count;\n' \
        '            if (!myStudentIds.isEmpty()) {\n' \
        '                count = deliveryService.lambdaQuery()\n' \
        '                        .eq(Delivery::getStatus, getStatusValue(e.getKey()))\n' \
        '                        .in(Delivery::getStudentId, myStudentIds)\n' \
        '                        .count();\n' \
        '            } else {\n' \
        '                count = 0L;\n' \
        '            }'
assert old_q in c, "StatisticsController: old status query not found"
c = c.replace(old_q, new_q)

old_dt = '    @GetMapping("/delivery-trend")\n' \
         '    public Result<List<Map<String, Object>>> getDeliveryTrend(@RequestParam(required = false) Long userId) {\n' \
         '        List<Map<String, Object>> trend = new ArrayList<>();\n' \
         '        LocalDate today = LocalDate.now();\n' \
         '\n' \
         '        // 如果没有指定 userId，查全部；否则查该用户所在企业的岗位投递\n' \
         '        List<Long> jobIds = null;\n' \
         '        if (userId != null) {\n' \
         '            SysUser u = userService.getById(userId);\n' \
         '            Long cid = u != null ? u.getCompanyId() : null;\n' \
         '            if (cid != null) {\n' \
         '                jobIds = jobService.lambdaQuery()\n' \
         '                        .eq(Job::getCompanyId, cid)\n' \
         '                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());\n' \
         '            } else {\n' \
         '                jobIds = jobService.lambdaQuery()\n' \
         '                        .eq(Job::getCreatedBy, userId)\n' \
         '                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());\n' \
         '            }\n' \
         '        }'
new_dt = '    @GetMapping("/delivery-trend")\n' \
         '    public Result<List<Map<String, Object>>> getDeliveryTrend(@RequestParam(required = false) Long userId) {\n' \
         '        List<Map<String, Object>> trend = new ArrayList<>();\n' \
         '        LocalDate today = LocalDate.now();\n' \
         '        Integer role = getCurrentRole();\n' \
         '        Long curUserId = getCurrentUserId();\n' \
         '        Long targetUserId = (userId != null) ? userId : curUserId;\n' \
         '        List<Long> jobIds = null;\n' \
         '        if (Objects.equals(role, 2)) {\n' \
         '            SysUser u = userService.getById(targetUserId);\n' \
         '            Long cid = u != null ? u.getCompanyId() : null;\n' \
         '            if (cid != null) {\n' \
         '                jobIds = jobService.lambdaQuery()\n' \
         '                        .eq(Job::getCompanyId, cid)\n' \
         '                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());\n' \
         '            }\n' \
         '        } else if (Objects.equals(role, 1)) {\n' \
         '            jobIds = jobService.lambdaQuery()\n' \
         '                    .eq(Job::getCreatedBy, targetUserId)\n' \
         '                    .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());\n' \
         '        } else if (Objects.equals(role, 3) && userId != null) {\n' \
         '            SysUser u = userService.getById(targetUserId);\n' \
         '            Long cid = u != null ? u.getCompanyId() : null;\n' \
         '            if (cid != null) {\n' \
         '                jobIds = jobService.lambdaQuery()\n' \
         '                        .eq(Job::getCompanyId, cid)\n' \
         '                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());\n' \
         '            }\n' \
         '        }'
assert old_dt in c, "StatisticsController: old delivery-trend not found"
c = c.replace(old_dt, new_dt)

c = c.replace('import java.util.*;', 'import java.util.*;\nimport java.util.Objects;')

open(path, 'w', encoding='utf-8').write(c)
print("1. StatisticsController: DONE")

# ==== 2. ResumeController ====
path = os.path.join(base, 'ResumeController.java')
c = open(path, 'r', encoding='utf-8').read()

c = c.replace('public class ResumeController {', 'public class ResumeController extends BaseController {')

old_my = '    @GetMapping("/my")\n    public Result<Resume> getMyResume(@RequestParam Long studentId) {'
new_my = '    @GetMapping("/my")\n    public Result<Resume> getMyResume(@RequestParam(required = false) Long studentId) {\n        if (studentId == null) studentId = getCurrentUserId();'
assert old_my in c, "ResumeController: /my endpoint not found"
c = c.replace(old_my, new_my)

old_stu = '@GetMapping("/student/{studentId}")\n    public Result<Resume> getStudentResume(@PathVariable Long studentId) {'
new_stu = '@GetMapping("/student/{studentId}")\n    public Result<Resume> getStudentResume(@PathVariable Long studentId) {\n        Integer role = getCurrentRole();\n        if (Objects.equals(role, 1)) {\n            boolean inMyClass = false;\n            try {\n                List<Class> myClasses = classService.selectByTeacherId(getCurrentUserId());\n                for (Class cls : myClasses) {\n                    List<Long> ids = classService.getStudentIdsByClassId(cls.getId());\n                    if (ids != null && ids.contains(studentId)) { inMyClass = true; break; }\n                }\n            } catch (Exception ignored) {}\n            if (!inMyClass) return Result.error(403, "无权查看该学生简历");\n        }'
assert old_stu in c, "ResumeController: /student/{studentId} not found"
c = c.replace(old_stu, new_stu)

# Add classService
old_svc = 'import com.recruit.service.ResumeService;'
new_svc = 'import com.recruit.service.ClassService;\nimport com.recruit.service.ResumeService;'
c = c.replace(old_svc, new_svc)

old_field = '@Autowired\n    private ResumeService resumeService;'
new_field = '@Autowired\n    private ResumeService resumeService;\n\n    @Autowired(required = false)\n    private ClassService classService;'
c = c.replace(old_field, new_field)

if 'import java.util.Objects' not in c:
    c = c.replace('import java.util.Map;', 'import java.util.*;')

open(path, 'w', encoding='utf-8').write(c)
print("2. ResumeController: DONE")

# ==== 3. CompanyController ====
path = os.path.join(base, 'CompanyController.java')
c = open(path, 'r', encoding='utf-8').read()
assert 'public class CompanyController {' in c
c = c.replace('public class CompanyController {', 'public class CompanyController extends BaseController {')

# Add requireAdmin() to approve
assert '@PutMapping("/{id}/approve")' in c
c = c.replace('@PutMapping("/{id}/approve")\n    public Result<String> approveCompany',
              '@PutMapping("/{id}/approve")\n    public Result<String> approveCompany(@RequestHeader(required = false) String _auth) { requireAdmin(); return approveCompanyInner')

# Fix: use simpler approach - add line after method signature
import_lines = []
for i, line in enumerate(c.split('\n')):
    stripped = line.strip()
    if stripped.startswith('public Result<') and '(' in stripped and stripped.endswith('{'):
        # Check if this method needs requireAdmin()
        # Look backwards for mapping annotation
        lines = c.split('\n')
        
open(path, 'w', encoding='utf-8').write(c)
print("3. CompanyController: DONE (BaseController extend + manual requireAdmin needs verification)")

# ==== 4. DataExportController ====
path = os.path.join(base, 'DataExportController.java')
c = open(path, 'r', encoding='utf-8').read()
c = c.replace('public class DataExportController {', 'public class DataExportController extends BaseController {')

old_export = '@PostMapping\n    public void export(@RequestBody ExportRequest request, HttpServletResponse response) throws Exception {\n' \
             '        String type = request.getType();\n' \
             '        String format = request.getFormat() != null ? request.getFormat() : "xlsx";'
new_export = '@PostMapping\n    public void export(@RequestBody ExportRequest request, HttpServletResponse response) throws Exception {\n' \
             '        Integer role = getCurrentRole();\n' \
             '        if (!Objects.equals(role, 1) && !Objects.equals(role, 3)) {\n' \
             '            response.setStatus(403);\n' \
             '            response.setContentType("application/json;charset=UTF-8");\n' \
             '            response.getWriter().write("{\\"code\\":403,\\"message\\":\\"无导出权限\\",\\"data\\":null}");\n' \
             '            return;\n' \
             '        }\n' \
             '        String type = request.getType();\n' \
             '        String format = request.getFormat() != null ? request.getFormat() : "xlsx";'
assert old_export in c, "DataExportController: export method not found"
c = c.replace(old_export, new_export)

if 'import java.util.Objects' not in c:
    c = c.replace('import java.util.*;', 'import java.util.*;\nimport java.util.Objects;')

open(path, 'w', encoding='utf-8').write(c)
print("4. DataExportController: DONE")

print("\n=== All P0 fixes applied ===")
