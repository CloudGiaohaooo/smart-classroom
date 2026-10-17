package com.youda.classroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youda.classroom.common.Result;
import com.youda.classroom.dto.AssignmentDTO;
import com.youda.classroom.service.AssignmentService;
import com.youda.classroom.vo.AssignmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作业列表接口
 */
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('assignment:read')")
    public Result<IPage<AssignmentVO>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long courseId,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(assignmentService.page(current, size, courseId, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('assignment:write')")
    public Result<Void> create(@Valid @RequestBody AssignmentDTO dto) {
        assignmentService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('assignment:write')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AssignmentDTO dto) {
        assignmentService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('assignment:write')")
    public Result<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return Result.success();
    }
}
