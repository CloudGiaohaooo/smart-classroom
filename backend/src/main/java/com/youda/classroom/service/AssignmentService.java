package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.dto.AssignmentDTO;
import com.youda.classroom.entity.Assignment;
import com.youda.classroom.mapper.AssignmentMapper;
import com.youda.classroom.vo.AssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 作业服务：作业列表模块
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;

    /**
     * 分页查询作业（联表带出课程名、提交数、已批阅数）
     */
    public IPage<AssignmentVO> page(long current, long size, Long courseId, String keyword) {
        return assignmentMapper.selectAssignmentPage(new Page<>(current, size), courseId, keyword);
    }

    public void create(AssignmentDTO dto) {
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(dto, assignment);
        if (assignment.getTotalScore() == null) {
            assignment.setTotalScore(new java.math.BigDecimal("100"));
        }
        assignmentMapper.insert(assignment);
    }

    public void update(Long id, AssignmentDTO dto) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作业不存在");
        }
        BeanUtils.copyProperties(dto, assignment);
        assignmentMapper.updateById(assignment);
    }

    public void delete(Long id) {
        assignmentMapper.deleteById(id);
    }
}
