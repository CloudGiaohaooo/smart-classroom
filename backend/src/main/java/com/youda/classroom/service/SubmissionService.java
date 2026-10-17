package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.cache.CacheKeys;
import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.dto.GradeDTO;
import com.youda.classroom.dto.SubmitDTO;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交与批阅服务：作业批阅反馈模块
 */
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final RedisCacheService cacheService;
    private final CourseOwnershipGuard ownershipGuard;
    private final EnrollmentService enrollmentService;

    /**
     * 学生提交作业
     */
    public void submit(SubmitDTO dto) {
        Assignment assignment = assignmentMapper.selectById(dto.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在");
        }
        // 选课门禁：学生只能提交自己已选修课程的作业，防止对任意作业提交
        if ("STUDENT".equals(UserContext.getRole())) {
            enrollmentService.assertEnrolled(assignment.getCourseId());
        }
        // 业务规则：超过截止时间不允许提交
        if (assignment.getDeadline() != null && assignment.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作业已过截止时间，无法提交");
        }

        Long currentUserId = UserContext.getUserId();
        // 防重复提交：同一学生同一作业只允许一条记录（与 schema.sql 的唯一索引双重保障）
        Long exists = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, dto.getAssignmentId())
                .eq(Submission::getStudentId, currentUserId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该作业已提交，请勿重复提交");
        }

        Submission submission = new Submission();
        submission.setAssignmentId(dto.getAssignmentId());
        submission.setStudentId(currentUserId);
        submission.setContent(dto.getContent());
        submission.setFileUrl(dto.getFileUrl());
        submission.setStatus("SUBMITTED");
        submission.setSubmitTime(LocalDateTime.now());
        try {
            submissionMapper.insert(submission);
        } catch (DuplicateKeyException e) {
            // 并发重复提交时唯一索引兜底：给出友好提示，而不是 500
            throw new BusinessException(ResultCode.BAD_REQUEST, "该作业已提交，请勿重复提交");
        }
        cacheService.evict(CacheKeys.STATS_OVERVIEW);
    }

    /**
     * 分页查询提交记录（联表带出作业标题、课程名、学生姓名）。
     *
     * 越权防护：STUDENT 已在 Controller 层强制 studentId=本人；此处 TEACHER 再按 teacher_id 过滤，
     * 仅返回自己授课课程下的提交；ADMIN 不附加过滤可看全量。
     */
    public IPage<SubmissionVO> page(long current, long size, Long assignmentId, Long studentId, String status) {
        Long teacherId = null;
        if ("TEACHER".equals(UserContext.getRole())) {
            teacherId = UserContext.getUserId();
        }
        return submissionMapper.selectSubmissionPage(new Page<>(current, size), assignmentId, studentId, status, teacherId);
    }

    /**
     * 当前学生的提交记录
     */
    public IPage<SubmissionVO> mySubmissions(long current, long size) {
        return submissionMapper.selectSubmissionPage(new Page<>(current, size), null, UserContext.getUserId(), null, null);
    }

    /**
     * 教师批阅作业（反馈 + 打分）。
     *
     * 使用 @Transactional 保证原子性：得分、反馈、状态、批阅时间要么全部成功、要么全部回滚，
     * 避免出现“分数已更新但状态未更新”的中间态。默认使用数据库 READ_COMMITTED 隔离级别。
     */
    @Transactional(rollbackFor = Exception.class)
    public void grade(Long id, GradeDTO dto) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提交记录不存在");
        }
        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            // 作业已被删除的“孤儿提交”不允许批阅，避免任意教师对无归属提交打分
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在，无法批阅");
        }
        // P0-B：教师只能批阅自己授课课程下的提交，防止跨课程改分
        ownershipGuard.assertCourseOwned(assignment.getCourseId());
        // 防御性兜底：满分字段为空（手动改库等边缘场景）时按默认 100 处理，避免 NPE
        BigDecimal total = assignment.getTotalScore() == null
                ? new BigDecimal("100") : assignment.getTotalScore();
        // 业务校验：得分不能超过满分
        if (dto.getScore().compareTo(total) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "得分不能超过满分 " + total);
        }

        submission.setScore(dto.getScore());
        submission.setFeedback(dto.getFeedback());
        submission.setStatus("GRADED");
        submission.setGradeTime(LocalDateTime.now());
        submissionMapper.updateById(submission);
        cacheService.evict(CacheKeys.STATS_OVERVIEW);
    }

}
