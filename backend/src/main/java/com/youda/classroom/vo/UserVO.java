package com.youda.classroom.vo;

import lombok.Data;

/**
 * 用户信息视图对象（不包含密码等敏感字段）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String phone;

    private String email;

    private String avatar;
}
