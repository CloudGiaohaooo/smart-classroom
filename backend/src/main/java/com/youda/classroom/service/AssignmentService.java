package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.dto.AssignmentDTO;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.entity.Submission;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.mapper.SubmissionMapper;
import com.youda.classroom.vo.AssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 作业服务：作业列表模块
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final RedisCacheService cacheService;
    private final CourseOwnershipGuard ownershipGuard;

    /**
     * 分页查询作业（联表带出课程名、提交数、已批阅数）
     */
    public IPage<AssignmentVO> page(long current, long size, Long courseId, String keyword) {
        return assignmentMapper.selectAssignmentPage(new Page<>(current, size), courseId, keyword);
    }

    public void create(AssignmentDTO dto) {
        // P0-B：只能在有操作权的课程下布置作业（教师仅本人课程、管理员全量）
        ownershipGuard.assertCourseOwned(dto.getCourseId());
        Assignment assignment = new Assignment();
        assignment.setCourseId(dto.getCourseId());
        assignment.setTitle(dto.getTitle());
        assignment.setContent(dto.getContent());
        assignment.setDeadline(dto.getDeadline());
        assignment.setTotalScore(dto.getTotalScore() == null ? new java.math.BigDecimal("100") : dto.getTotalScore());
        assignmentMapper.insert(assignment);
        cacheService.evict(StatsService.STATS_CACHE_KEY);
    }

    public void update(Long id, AssignmentDTO dto) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在");
        }
        // P0-B：先校验对「原课程」有操作权；若尝试迁移到其它课程，还需对新课程有操作权
        ownershipGuard.assertCourseOwned(assignment.getCourseId());
        if (dto.getCourseId() != null && !dto.getCourseId().equals(assignment.getCourseId())) {
            ownershipGuard.assertCourseOwned(dto.getCourseId());
        }
        // 显式字段拷贝，未传的可选字段不覆盖（修复 BeanUtils 全量拷贝把未传字段清空的问题）
        assignment.setCourseId(dto.getCourseId());
        assignment.setTitle(dto.getTitle());
        if (dto.getContent() != null) {
            assignment.setContent(dto.getContent());
        }
        if (dto.getDeadline() != null) {
            assignment.setDeadline(dto.getDeadline());
        }
        if (dto.getTotalScore() != null) {
            assignment.setTotalScore(dto.getTotalScore());
        }
        assignmentMapper.updateById(assignment);
        cacheService.evict(StatsService.STATS_CACHE_KEY);
    }

    public void delete(Long id) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在");
        }
        // P0-B：仅管理员或该作业所属课程的授课教师可删除
        ownershipGuard.assertCourseOwned(assignment.getCourseId());
        Long subCount = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getAssignmentId, id));
        if (subCount != null && subCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该作业下存在 " + subCount + " 条提交记录，请先处理后再删除");
        }
        assignmentMapper.deleteById(id);
        cacheService.evict(StatsService.STATS_CACHE_KEY);
    }
}
