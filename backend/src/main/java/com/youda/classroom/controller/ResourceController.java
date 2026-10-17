package com.youda.classroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youda.classroom.annotation.BehaviorLog;
import com.youda.classroom.common.Result;
import com.youda.classroom.enums.BehaviorType;
import com.youda.classroom.service.ResourceService;
import com.youda.classroom.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 课程资源上传接口
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('resource:write')")
    public Result<Void> upload(@RequestParam("courseId") Long courseId,
                               @RequestParam(value = "resourceType", required = false) String resourceType,
                               @RequestParam("file") MultipartFile file) {
        resourceService.upload(courseId, resourceType, file);
        return Result.success("上传成功", null);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('resource:read')")
    @BehaviorLog(BehaviorType.VIEW_RESOURCE)
    public Result<IPage<ResourceVO>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Long courseId) {
        return Result.success(resourceService.page(current, size, courseId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('resource:write')")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.success();
    }
}
