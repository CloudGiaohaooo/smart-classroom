package com.youda.classroom.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回结果：token + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private UserVO user;
}
