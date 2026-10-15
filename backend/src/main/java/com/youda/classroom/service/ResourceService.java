package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.entity.CourseResource;
import com.youda.classroom.mapper.CourseResourceMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 课程资源服务：课程资源上传模块
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final CourseResourceMapper resourceMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    /**
     * 上传课程资源：文件落盘到本地目录，同时在数据库记录元数据
     */
    public void upload(Long courseId, String resourceType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }

        // 1. 生成唯一文件名，避免重名覆盖；按课程分目录存放
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        String uuidName = UUID.randomUUID().toString().replace("-", "") + (ext != null ? "." + ext : "");
        String relativeDir = courseId + File.separator;
        File dir = new File(uploadDir, relativeDir).getAbsoluteFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, uuidName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ERROR, "文件保存失败：" + e.getMessage());
        }

        // 2. 写入数据库
        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setResourceName(StringUtils.hasText(originalName) ? originalName : "未命名资源");
        resource.setResourceType(StringUtils.hasText(resourceType) ? resourceType.toUpperCase() : "OTHER");
        resource.setFileUrl(accessPrefix + "/" + relativeDir.replace('\\', '/') + uuidName);
        resource.setFileSize(file.getSize());
        resource.setUploaderId(UserContext.getUserId());
        resource.setUploadTime(LocalDateTime.now());
        resourceMapper.insert(resource);
    }

    /**
     * 分页查询资源（联表带出课程名、上传人姓名）
     */
    public IPage<ResourceVO> page(long current, long size, Long courseId) {
        return resourceMapper.selectResourcePage(new Page<>(current, size), courseId);
    }

    /**
     * 删除资源：同时删除数据库记录与磁盘文件
     */
    public void delete(Long id) {
        CourseResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        }
        resourceMapper.deleteById(id);
        if (StringUtils.hasText(resource.getFileUrl())) {
            String relativePath = resource.getFileUrl().replace(accessPrefix + "/", "");
            File f = new File(uploadDir, relativePath);
            if (f.exists()) {
                f.delete();
            }
        }
    }
}
