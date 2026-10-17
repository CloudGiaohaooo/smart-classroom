package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youda.classroom.entity.SysPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper
 */
public interface PermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 三表 JOIN：根据用户 ID 查出其拥有的全部权限编码。
     * 链路：sys_user_role(用户→角色) -> sys_role_permission(角色→权限) -> sys_permission(权限编码)。
     * 用于 Spring Security 过滤器把权限写入认证上下文（每次请求实时从库加载，权限变更即时生效）。
     */
    @Select("SELECT DISTINCT p.perm_code " +
            "FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);
}
