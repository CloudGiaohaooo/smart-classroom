package com.youda.classroom.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.entity.SysPermission;
import com.youda.classroom.entity.SysRole;
import com.youda.classroom.entity.SysRolePermission;
import com.youda.classroom.entity.SysUserRole;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.PermissionMapper;
import com.youda.classroom.mapper.RoleMapper;
import com.youda.classroom.mapper.RolePermissionMapper;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RBAC 初始化器（Phase 2）：
 * 幂等地初始化「权限 / 角色 / 角色-权限 / 用户-角色」四类数据。
 * 相比 DataInitializer（仅在用户表为空时跑），本初始化器每次启动都检查缺失项并补齐，
 * 因此对「老库升级」同样有效：已存在用户但缺 RBAC 表数据的库也能自动映射。
 *
 * @Order(2) 保证在 DataInitializer（@Order(1)）之后执行，此时演示用户已就绪。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class RbacInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserMapper userMapper;

    /** 权限清单：编码 -> 名称 */
    private static final LinkedHashMap<String, String> PERMISSIONS = new LinkedHashMap<>() {{
        put("course:read", "查看课程");
        put("course:write", "管理课程");
        put("assignment:read", "查看作业");
        put("assignment:write", "管理作业");
        put("submission:read", "查看提交");
        put("submission:submit", "提交作业");
        put("submission:grade", "批阅作业");
        put("resource:read", "查看资源");
        put("resource:write", "管理资源");
        put("stats:read", "查看统计");
        put("user:read", "查看用户");
        put("enrollment:read", "查看选课");
        put("enrollment:write", "选课/退课");
    }};

    /** 角色 -> 权限编码列表（最小权限原则：学生只读+提交，教师可写+批阅，管理员全量） */
    private static final Map<String, List<String>> ROLE_PERMS = Map.of(
            "ADMIN", List.of("course:read", "course:write", "assignment:read", "assignment:write",
                    "submission:read", "submission:submit", "submission:grade",
                    "resource:read", "resource:write", "stats:read", "user:read", "enrollment:read"),
            "TEACHER", List.of("course:read", "course:write", "assignment:read", "assignment:write",
                    "submission:read", "submission:grade",
                    "resource:read", "resource:write", "stats:read", "user:read", "enrollment:read"),
            "STUDENT", List.of("course:read", "assignment:read", "submission:read", "submission:submit",
                    "resource:read", "enrollment:read", "enrollment:write")
    );

    @Override
    public void run(String... args) {
        Map<String, Long> permIds = ensurePermissions();
        Map<String, Long> roleIds = ensureRoles();
        ensureRolePermissions(roleIds, permIds);
        ensureUserRoles(roleIds);
        log.info("RBAC 初始化完成：权限 {} 项，角色 {} 个", permIds.size(), roleIds.size());
    }

    private Map<String, Long> ensurePermissions() {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : PERMISSIONS.entrySet()) {
            SysPermission p = permissionMapper.selectOne(
                    new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getPermCode, e.getKey()));
            if (p == null) {
                p = new SysPermission();
                p.setPermCode(e.getKey());
                p.setPermName(e.getValue());
                permissionMapper.insert(p);
            }
            ids.put(e.getKey(), p.getId());
        }
        return ids;
    }

    private Map<String, Long> ensureRoles() {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (String code : ROLE_PERMS.keySet()) {
            SysRole r = roleMapper.selectOne(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, code));
            if (r == null) {
                r = new SysRole();
                r.setRoleCode(code);
                r.setRoleName(roleNameOf(code));
                roleMapper.insert(r);
            }
            ids.put(code, r.getId());
        }
        return ids;
    }

    private void ensureRolePermissions(Map<String, Long> roleIds, Map<String, Long> permIds) {
        for (Map.Entry<String, List<String>> e : ROLE_PERMS.entrySet()) {
            Long roleId = roleIds.get(e.getKey());
            for (String perm : e.getValue()) {
                Long permId = permIds.get(perm);
                Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)
                        .eq(SysRolePermission::getPermissionId, permId));
                if (count == 0) {
                    SysRolePermission rp = new SysRolePermission();
                    rp.setRoleId(roleId);
                    rp.setPermissionId(permId);
                    rolePermissionMapper.insert(rp);
                }
            }
        }
    }

    private void ensureUserRoles(Map<String, Long> roleIds) {
        List<User> users = userMapper.selectList(null);
        for (User u : users) {
            Long roleId = roleIds.get(u.getRole());
            if (roleId == null) {
                log.warn("用户 {} 的角色 {} 未在 RBAC 中定义，跳过绑定", u.getUsername(), u.getRole());
                continue;
            }
            Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, u.getId())
                    .eq(SysUserRole::getRoleId, roleId));
            if (count == 0) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    private String roleNameOf(String code) {
        return switch (code) {
            case "ADMIN" -> "系统管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> code;
        };
    }
}
