package com.youda.classroom.controller;

import com.youda.classroom.common.Result;
import com.youda.classroom.service.UserService;
import com.youda.classroom.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/students")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<List<UserVO>> students() {
        return Result.success(userService.listStudents());
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<List<UserVO>> teachers() {
        return Result.success(userService.listTeachers());
    }
}
