package com.youda.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.entity.CourseResource;
import com.youda.classroom.vo.ResourceVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程资源 Mapper
 */
public interface CourseResourceMapper extends BaseMapper<CourseResource> {

    /**
     * 多表查询：资源 LEFT JOIN 课程、用户，联表查出课程名与上传人姓名
     */
    @Select("<script>" +
            "SELECT r.id, r.course_id, r.resource_name, r.resource_type, r.file_url, r.file_size, r.uploader_id, r.upload_time, " +
            "       c.course_name, u.real_name AS uploader_name " +
            "FROM course_resource r " +
            "LEFT JOIN course c ON r.course_id = c.id " +
            "LEFT JOIN sys_user u ON r.uploader_id = u.id " +
            "<where>" +
            "  <if test='courseId != null'> AND r.course_id = #{courseId} </if>" +
            "</where>" +
            "ORDER BY r.id DESC" +
            "</script>")
    IPage<ResourceVO> selectResourcePage(Page<?> page, @Param("courseId") Long courseId);
}
