package com.smartpm.service;

import com.smartpm.entity.User;
import com.smartpm.vo.LoginVO;

public interface UserService {

    /** 注册，返回注册成功的用户信息 */
    User register(String username, String password, String nickname, String identity);

    /** 登录，校验成功返回 token + 用户信息 */
    LoginVO login(String username, String password);

    /** 更新当前用户的专业身份 */
    void updateIdentity(Long userId, String identity);
}
