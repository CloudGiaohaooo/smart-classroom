package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.entity.User;
import com.youda.classroom.mapper.UserMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 获取当前登录用户信息
     */
    public UserVO getCurrentUser() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    /**
     * 查询全部学生（供教师批阅时筛选）
     */
    public List<UserVO> listStudents() {
        return userMapper.selectList(
                        new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT"))
                .stream().map(u -> {
                    UserVO vo = new UserVO();
                    vo.setId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setRealName(u.getRealName());
                    vo.setRole(u.getRole());
                    return vo;
                }).toList();
    }

    /**
     * 查询全部教师（供课程新增时选择授课教师）
     */
    public List<UserVO> listTeachers() {
        return userMapper.selectList(
                        new LambdaQueryWrapper<User>().eq(User::getRole, "TEACHER"))
                .stream().map(u -> {
                    UserVO vo = new UserVO();
                    vo.setId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setRealName(u.getRealName());
                    vo.setRole(u.getRole());
                    return vo;
                }).toList();
    }
}
