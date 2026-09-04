package com.smartpm.service;

import com.smartpm.entity.User;
import com.smartpm.vo.AdminUserVO;
import com.smartpm.vo.LoginVO;

import java.util.List;

public interface UserService {

    /** 注册，返回注册成功的用户信息 */
    User register(String username, String password, String nickname, String identity);

    /** 登录，校验成功返回 token + 用户信息 */
    LoginVO login(String username, String password);

    /** 更新当前用户的专业身份 */
    void updateIdentity(Long userId, String identity);

    /** 判断用户是否具备系统管理员权限 */
    boolean isSystemAdmin(Long userId);

    /** 系统管理员：查询全部用户（不返回密码） */
    List<AdminUserVO> listUsersForAdmin();

    /** 系统管理员：修改账号状态 */
    void updateUserStatus(Long userId, String status);

    /** 系统管理员：修改系统权限 */
    void updateSystemRole(Long userId, String systemRole);

    /** 系统管理员：重置用户密码 */
    void resetUserPassword(Long userId, String password);
}
