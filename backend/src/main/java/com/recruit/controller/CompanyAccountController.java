package com.recruit.controller;

import com.recruit.entity.Company;
import com.recruit.entity.SysUser;
import com.recruit.service.CompanyService;
import com.recruit.service.UserService;
import com.recruit.utils.AESUtil;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 企业子账号管理控制器
 */
@RestController
@RequestMapping("/companies/{companyId}/accounts")
public class CompanyAccountController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AESUtil aesUtil;

    /**
     * 获取企业下所有子账号（HR角色）
     */
    @GetMapping
    public Result<List<SysUser>> listAccounts(@PathVariable Long companyId) {
        checkCompanyMembership(companyId);
        Company company = companyService.getById(companyId);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }
        List<SysUser> accounts = userService.lambdaQuery()
                .eq(SysUser::getCompanyId, companyId)
                .eq(SysUser::getRole, 2)
                .list();
        // 脱敏处理
        for (SysUser user : accounts) {
            user.setPassword(null);
            // AES解密手机号
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                try {
                    user.setPhone(aesUtil.decrypt(user.getPhone()));
                } catch (Exception e) {
                    user.setPhone("[加密数据]");
                }
            }
        }
        return Result.success(accounts);
    }

    /**
     * 创建企业子账号
     */
    @PostMapping
    public Result<String> createAccount(@PathVariable Long companyId, @RequestBody SysUser user) {
        checkCompanyMembership(companyId);
        Company company = companyService.getById(companyId);
        if (company == null) {
            return Result.error(404, "企业不存在");
        }

        // 检查用户名唯一
        SysUser exist = userService.lambdaQuery()
                .eq(SysUser::getUsername, user.getUsername())
                .one();
        if (exist != null) {
            return Result.error("用户名已存在");
        }

        user.setCompanyId(companyId);
        user.setRole(2); // 强制 HR 角色
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userService.save(user);

        return Result.success("子账号创建成功");
    }

    /**
     * 更新子账号信息
     */
    @PutMapping("/{userId}")
    public Result<String> updateAccount(@PathVariable Long companyId,
                                        @PathVariable Long userId,
                                        @RequestBody SysUser user) {
        checkCompanyMembership(companyId);
        SysUser exist = userService.getById(userId);
        if (exist == null || !companyId.equals(exist.getCompanyId())) {
            return Result.error(404, "账号不存在");
        }

        if (user.getRealName() != null) exist.setRealName(user.getRealName());
        if (user.getPhone() != null) exist.setPhone(user.getPhone() != null && !user.getPhone().isEmpty() ? aesUtil.encrypt(user.getPhone()) : user.getPhone());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            exist.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getStatus() != null) exist.setStatus(user.getStatus());

        userService.updateById(exist);
        return Result.success("更新成功");
    }

    /**
     * 删除子账号（软删除）
     */
    @DeleteMapping("/{userId}")
    public Result<String> deleteAccount(@PathVariable Long companyId,
                                        @PathVariable Long userId) {
        checkCompanyMembership(companyId);
        SysUser exist = userService.getById(userId);
        if (exist == null || !companyId.equals(exist.getCompanyId())) {
            return Result.error(404, "账号不存在");
        }
        userService.removeById(userId);
        return Result.success("删除成功");
    }

    /**
     * 校验当前登录 HR 属于该企业（管理员放行）
     */
    private void checkCompanyMembership(Long companyId) {
        Integer role = getCurrentRole();
        if (Objects.equals(role, 3)) return;
        SysUser current = userService.getById(getCurrentUserId());
        if (current == null || !Objects.equals(current.getCompanyId(), companyId)) {
            throw new com.recruit.exception.BusinessException(403, "无权管理其他企业的账号");
        }
    }
}
