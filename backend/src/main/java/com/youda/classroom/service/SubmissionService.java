package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 学生提交作业
     */
    public void submit(SubmitDTO dto) {
        Assignment assignment = assignmentMapper.selectById(dto.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在");
        }
        // 业务规则：超过截止时间不允许提交
        if (assignment.getDeadline() != null && assignment.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作业已过截止时间，无法提交");
        }

        Submission submission = new Submission();
        submission.setAssignmentId(dto.getAssignmentId());
        submission.setStudentId(UserContext.getUserId());
        submission.setContent(dto.getContent());
        submission.setFileUrl(dto.getFileUrl());
        submission.setStatus("SUBMITTED");
        submission.setSubmitTime(LocalDateTime.now());
        submissionMapper.insert(submission);
        cacheService.evict(StatsService.STATS_CACHE_KEY);
    }

    /**
     * 分页查询提交记录（联表带出作业标题、课程名、学生姓名）
     */
    public IPage<SubmissionVO> page(long current, long size, Long assignmentId, Long studentId, String status) {
        return submissionMapper.selectSubmissionPage(new Page<>(current, size), assignmentId, studentId, status);
    }

    /**
     * 当前学生的提交记录
     */
    public IPage<SubmissionVO> mySubmissions(long current, long size) {
        return submissionMapper.selectSubmissionPage(new Page<>(current, size), null, UserContext.getUserId(), null);
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
        // 防御性兜底：满分字段为空（手动改库等边缘场景）时按默认 100 处理，避免 NPE
        BigDecimal total = (assignment == null || assignment.getTotalScore() == null)
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
        cacheService.evict(StatsService.STATS_CACHE_KEY);
    }

}
