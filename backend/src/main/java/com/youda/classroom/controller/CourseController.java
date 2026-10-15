package com.youda.classroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youda.classroom.common.Result;
import com.youda.classroom.dto.CourseDTO;
import com.youda.classroom.entity.Course;
import com.youda.classroom.service.CourseService;
import com.youda.classroom.vo.CourseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程管理接口（RESTful 风格）
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<IPage<CourseVO>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long teacherId) {
        return Result.success(courseService.page(current, size, keyword, teacherId));
    }

    @GetMapping("/all")
    public Result<List<Course>> all() {
        return Result.success(courseService.listAll());
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CourseDTO dto) {
        courseService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseDTO dto) {
        courseService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success();
    }
}
