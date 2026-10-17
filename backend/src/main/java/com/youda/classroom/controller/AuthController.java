package com.youda.classroom.controller;

import com.youda.classroom.annotation.BehaviorLog;
import com.youda.classroom.common.Result;
import com.youda.classroom.dto.LoginDTO;
import com.youda.classroom.enums.BehaviorType;
import com.youda.classroom.service.AuthService;
import com.youda.classroom.service.UserService;
import com.youda.classroom.vo.LoginVO;
import com.youda.classroom.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @BehaviorLog(BehaviorType.LOGIN)
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success("登录成功", authService.login(dto, clientIp(request)));
    }

    /** 取客户端真实 IP：优先 X-Forwarded-For（Nginx 反代场景），否则回退 RemoteAddr */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUser());
    }
}
