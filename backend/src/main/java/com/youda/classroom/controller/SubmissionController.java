package com.youda.classroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youda.classroom.common.Result;
import com.youda.classroom.dto.GradeDTO;
import com.youda.classroom.dto.SubmitDTO;
import com.youda.classroom.service.GradingAssistService;
import com.youda.classroom.service.SubmissionService;
import com.youda.classroom.vo.GradingAssistVO;
import com.youda.classroom.vo.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作业提交与批阅接口
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GradingAssistService gradingAssistService;

    @PostMapping
    @PreAuthorize("hasAuthority('submission:submit')")
    public Result<Void> submit(@Valid @RequestBody SubmitDTO dto) {
        submissionService.submit(dto);
        return Result.success("提交成功", null);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('submission:read')")
    public Result<IPage<SubmissionVO>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long assignmentId,
                                            @RequestParam(required = false) Long studentId,
                                            @RequestParam(required = false) String status) {
        return Result.success(submissionService.page(current, size, assignmentId, studentId, status));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('submission:read')")
    public Result<IPage<SubmissionVO>> my(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.success(submissionService.mySubmissions(current, size));
    }

    @PutMapping("/{id}/grade")
    @PreAuthorize("hasAuthority('submission:grade')")
    public Result<Void> grade(@PathVariable Long id, @Valid @RequestBody GradeDTO dto) {
        submissionService.grade(id, dto);
        return Result.success("批阅成功", null);
    }

    /**
     * 批阅辅助（创新点 A）：返回某条提交的「相似提交」与「推荐评语」，供教师批阅页参考
     */
    @GetMapping("/{id}/grading-assist")
    @PreAuthorize("hasAuthority('submission:grade')")
    public Result<GradingAssistVO> gradingAssist(@PathVariable Long id) {
        return Result.success(gradingAssistService.assist(id));
    }
}
