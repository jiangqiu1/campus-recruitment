package com.recruit.controller;

import com.recruit.entity.Company;
import com.recruit.entity.SysUser;
import com.recruit.service.CompanyService;
import com.recruit.service.UserMessageService;
import com.recruit.service.UserService;
import com.recruit.utils.AESUtil;
import com.recruit.utils.Result;
import com.recruit.annotation.LogOperation;
import com.recruit.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 企业管理控制器
 * 管理员和教师可以访问
 */
@RestController
@RequestMapping("/companies")
public class CompanyController extends BaseController {
    
    @Autowired
    private CompanyService companyService;
    
    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMessageService userMessageService;
    
    /**
     * 获取企业列表（可选按状态筛选）
     * 
     * @param status 企业状态（0=待审核, 1=通过, 2=拒绝，不传则返回全部）
     * @return 企业列表
     */
    @GetMapping
    public Result<List<Company>> getAllCompanies(@RequestParam(required = false) Integer status) {
        List<Company> companies;
        if (status != null) {
            companies = companyService.lambdaQuery().eq(Company::getStatus, status).list();
        } else {
            companies = companyService.list();
        }
        
        // AES解密敏感字段（联系电话）
        for (Company company : companies) {
            if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
                try {
                    company.setContactPhone(aesUtil.decrypt(company.getContactPhone()));
                } catch (Exception e) {
                    company.setContactPhone("[加密数据]");
                }
            }
        }
        
        return Result.success(companies);
    }

    /**
     * 审核通过企业
     *
     * @param id 企业ID
     * @return 操作结果
     */
    @LogOperation("审核通过企业")
    @PutMapping("/{id}/approve")
    public Result<String> approveCompany(@PathVariable Long id) {
        requireAdmin();
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        company.setStatus(1);
        companyService.updateById(company);
        // 通知该企业的 HR
        notifyCompanyHr(id, "企业审核通过", "您名下的企业「" + company.getName() + "」已通过审核，现在可以发布岗位了");
        return Result.success("企业审核通过");
    }

    /**
     * 审核拒绝企业
     *
     * @param id 企业ID
     * @return 操作结果
     */
    @LogOperation("审核拒绝企业")
    @PutMapping("/{id}/reject")
    public Result<String> rejectCompany(@PathVariable Long id) {
        requireAdmin();
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        company.setStatus(2);
        companyService.updateById(company);
        // 通知该企业的 HR
        notifyCompanyHr(id, "企业审核未通过", "您名下的企业「" + company.getName() + "」未通过审核，请联系管理员了解详情");
        return Result.success("企业审核拒绝");
    }
    
    /**
     * 根据ID获取企业
     * 
     * @param id 企业ID
     * @return 企业实体
     */
    @GetMapping("/{id}")
    public Result<Company> getCompanyById(@PathVariable Long id) {
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        
        // AES解密敏感字段
        if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
            company.setContactPhone(aesUtil.decrypt(company.getContactPhone()));
        }
        
        return Result.success(company);
    }
    
    /**
     * 根据合作等级查询企业
     * 
     * @param cooperationLevel 合作等级（0=潜在，1=合作中，2=核心，3=已流失）
     * @return 企业列表
     */
    @GetMapping("/by-cooperation-level/{cooperationLevel}")
    public Result<List<Company>> getCompaniesByCooperationLevel(@PathVariable Integer cooperationLevel) {
        List<Company> companies = companyService.selectByCooperationLevel(cooperationLevel);
        
        // AES解密敏感字段
        for (Company company : companies) {
            if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
                company.setContactPhone(aesUtil.decrypt(company.getContactPhone()));
            }
        }
        
        return Result.success(companies);
    }
    
    /**
     * 根据行业查询企业
     * 
     * @param industry 行业
     * @return 企业列表
     */
    @GetMapping("/by-industry/{industry}")
    public Result<List<Company>> getCompaniesByIndustry(@PathVariable String industry) {
        List<Company> companies = companyService.selectByIndustry(industry);
        
        // AES解密敏感字段
        for (Company company : companies) {
            if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
                company.setContactPhone(aesUtil.decrypt(company.getContactPhone()));
            }
        }
        
        return Result.success(companies);
    }
    
    /**
     * 创建企业
     * 教师和HR也可以创建（合作企业入驻）
     */
    @PostMapping
    public Result<String> createCompany(@RequestBody Company company) {
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 3) && !Objects.equals(role, 1) && !Objects.equals(role, 2)) {
            return Result.error(403, "无权限创建企业");
        }
        // AES加密敏感字段（联系电话）
        if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
            company.setContactPhone(aesUtil.encrypt(company.getContactPhone()));
        }
        
        // 教师和HR创建的企业默认合作等级为1
        if (!Objects.equals(role, 3)) {
            company.setCooperationLevel(1);
        }
        
        companyService.save(company);
        // 通知管理员有新的企业入驻待审核
        try {
            userService.lambdaQuery()
                    .eq(SysUser::getRole, 3)
                    .list()
                    .forEach(admin -> userMessageService.sendMessage(
                            admin.getId(),
                            "新企业入驻待审核",
                            "企业「" + company.getName() + "」已完成注册，请前往审核",
                            "audit", company.getId()));
        } catch (Exception e) { /* ignore */ }
        return Result.success("企业创建成功");
    }
    
    /**
     * 更新企业
     * 
     * @param id 企业ID
     * @param company 企业实体（包含要更新的字段）
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<String> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        // HR 可以修改自己公司的信息，管理员可以修改所有公司
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        if (!Objects.equals(role, 3)) {
            SysUser currentUser = userService.getById(userId);
            if (currentUser == null || !Objects.equals(currentUser.getCompanyId(), id)) {
                throw new BusinessException(403, "无权限修改此企业信息");
            }
        }
        Company existCompany = companyService.getById(id);
        if (existCompany == null) {
            return Result.error(404, "企业不存在");
        }
        
        // 过滤空字符串字段，避免覆盖已有数据
        if (company.getName() != null && company.getName().trim().isEmpty()) {
            company.setName(null);
        }
        if (company.getIndustry() != null && company.getIndustry().trim().isEmpty()) {
            company.setIndustry(null);
        }
        if (company.getSize() != null && company.getSize().trim().isEmpty()) {
            company.setSize(null);
        }
        if (company.getCity() != null && company.getCity().trim().isEmpty()) {
            company.setCity(null);
        }
        if (company.getAddress() != null && company.getAddress().trim().isEmpty()) {
            company.setAddress(null);
        }
        if (company.getDescription() != null && company.getDescription().trim().isEmpty()) {
            company.setDescription(null);
        }
        if (company.getContactPerson() != null && company.getContactPerson().trim().isEmpty()) {
            company.setContactPerson(null);
        }
        if (company.getContactEmail() != null && company.getContactEmail().trim().isEmpty()) {
            company.setContactEmail(null);
        }
        
        // AES加密敏感字段（联系电话）
        if (company.getContactPhone() != null) {
            if (company.getContactPhone().trim().isEmpty()) {
                company.setContactPhone(null);
            } else {
                company.setContactPhone(aesUtil.encrypt(company.getContactPhone()));
            }
        }
        
        company.setId(id);
        boolean success = companyService.updateById(company);
        if (!success) {
            return Result.error("企业信息更新失败，请重试");
        }
        
        return Result.success("企业更新成功");
    }
    
    /**
     * 删除企业（软删除）
     * 
     * @param id 企业ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteCompany(@PathVariable Long id) {
        requireAdmin();
        Company company = companyService.getById(id);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        
        companyService.removeById(company.getId());
        return Result.success("企业删除成功");
    }
    
    /**
     * 更新企业合作等级
     * 
     * @param id 企业ID
     * @param params 包含cooperationLevel的参数
     * @return 更新结果
     */
    @PutMapping("/{id}/cooperation-level")
    public Result<String> updateCooperationLevel(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> params) {
        requireAdmin();
        Integer cooperationLevel = params.get("cooperationLevel");
        if (cooperationLevel == null || cooperationLevel < 0 || cooperationLevel > 3) {
            return Result.error("cooperationLevel参数错误（应为0-3）");
        }
        
        boolean success = companyService.updateCooperationLevel(id, cooperationLevel);
        if (!success) {
            return Result.error("更新失败");
        }
        
        return Result.success("合作等级更新成功");
    }
    
    /**
     * 搜索企业（根据名称模糊搜索）
     * 
     * @param keyword 关键词
     * @return 企业列表
     */
    @GetMapping("/search")
    public Result<List<Company>> searchCompanies(@RequestParam String keyword) {
        List<Company> companies = companyService.lambdaQuery()
                .like(Company::getName, keyword)
                .or()
                .like(Company::getShortName, keyword)
                .list();
        
        // AES解密敏感字段
        for (Company company : companies) {
            if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
                company.setContactPhone(aesUtil.decrypt(company.getContactPhone()));
            }
        }
        
        return Result.success(companies);
    }

    /**
     * 通知企业所属的 HR 用户
     */
    private void notifyCompanyHr(Long companyId, String title, String content) {
        try {
            userService.lambdaQuery()
                    .eq(SysUser::getCompanyId, companyId)
                    .eq(SysUser::getRole, 2)
                    .list()
                    .forEach(hr -> userMessageService.sendMessage(
                            hr.getId(), title, content, "audit", companyId));
        } catch (Exception e) { /* ignore */ }
    }
}
