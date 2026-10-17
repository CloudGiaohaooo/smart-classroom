package com.youda.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youda.classroom.cache.CacheKeys;
import com.youda.classroom.cache.RedisCacheService;
import com.youda.classroom.common.BusinessException;
import com.youda.classroom.common.ResultCode;
import com.youda.classroom.entity.CourseResource;
import com.youda.classroom.mapper.CourseResourceMapper;
import com.youda.classroom.security.SignedFileUrl;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.util.FileMd5;
import com.youda.classroom.vo.ResourceUploadVO;
import com.youda.classroom.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * 课程资源服务：课程资源上传模块（创新点 C：MD5 秒传去重）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final CourseResourceMapper resourceMapper;
    private final RedisCacheService cacheService;
    private final CourseOwnershipGuard ownershipGuard;
    private final SignedFileUrl signedFileUrl;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    /** 秒传去重开关（application.yml 可配，便于演示/对比去重效果） */
    @Value("${resource.dedup-enabled}")
    private boolean dedupEnabled;

    /** 允许上传的文件扩展名白名单（application.yml 可配，防上传 html/svg/js 等可执行文件形成存储型 XSS） */
    @Value("${resource.allowed-extensions}")
    private String allowedExtensions;

    /**
     * 上传课程资源。
     * 创新点 C：先计算文件 MD5，若已有相同 MD5 的资源则复用其物理文件（秒传，不重复落盘），
     * 仅新增一条元数据记录；否则落盘并记录 MD5。
     */
    public ResourceUploadVO upload(Long courseId, String resourceType, MultipartFile file) {
        // P0-B：只能往自己有操作权的课程上传资源（教师仅本人课程、管理员全量）
        ownershipGuard.assertCourseOwned(courseId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }

        // 0. 扩展名白名单校验 + 原始文件名清洗（防上传可执行文件形成存储型 XSS / 目录穿越）
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        if (!isAllowedExtension(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型，仅允许：" + allowedExtensions);
        }
        originalName = sanitizeFileName(originalName);

        // 1. 流式计算 MD5（边读边更新摘要，不把整个文件读入内存，避免大文件 OOM）
        String md5;
        try (InputStream in = file.getInputStream()) {
            md5 = FileMd5.md5Hex(in);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ERROR, "文件读取失败：" + e.getMessage());
        }

        // 2. 秒传去重：命中相同 MD5 则复用已有物理文件，只新增元数据
        CourseResource existing = null;
        if (dedupEnabled) {
            existing = resourceMapper.selectOne(new LambdaQueryWrapper<CourseResource>()
                    .eq(CourseResource::getFileMd5, md5)
                    .last("LIMIT 1"));
        }

        String fileUrl;
        boolean deduplicated;
        if (existing != null) {
            fileUrl = existing.getFileUrl();
            deduplicated = true;
        } else {
            // 3. 生成唯一文件名，避免重名覆盖；按课程分目录存放
            String uuidName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            String relativeDir = courseId + File.separator;
            File dir = new File(uploadDir, relativeDir).getAbsoluteFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BusinessException(ResultCode.ERROR, "创建上传目录失败：" + dir.getAbsolutePath());
            }
            File dest = new File(dir, uuidName);
            // 流式落盘（MultipartFile.transferTo 内部流式拷贝，不一次性读入内存）
            try {
                file.transferTo(dest);
            } catch (IOException e) {
                throw new BusinessException(ResultCode.ERROR, "文件保存失败：" + e.getMessage());
            }
            fileUrl = accessPrefix + "/" + relativeDir.replace('\\', '/') + uuidName;
            deduplicated = false;
        }

        // 4. 写入元数据（无论秒传与否，都为本课程新增一条可下载记录）
        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setResourceName(originalName);
        resource.setResourceType(StringUtils.hasText(resourceType) ? resourceType.toUpperCase() : "OTHER");
        resource.setFileUrl(fileUrl);
        resource.setFileMd5(md5);
        resource.setFileSize(file.getSize());
        resource.setUploaderId(UserContext.getUserId());
        resource.setUploadTime(LocalDateTime.now());
        resourceMapper.insert(resource);
        cacheService.evict(CacheKeys.STATS_OVERVIEW);

        ResourceUploadVO vo = new ResourceUploadVO();
        vo.setResourceId(resource.getId());
        vo.setFileMd5(md5);
        vo.setDeduplicated(deduplicated);
        // 响应返回带签名与有效期的下载 URL；数据库仍存未签名路径
        vo.setFileUrl(signedFileUrl.sign(fileUrl));
        return vo;
    }

    /**
     * 分页查询资源（联表带出课程名、上传人姓名）
     */
    public IPage<ResourceVO> page(long current, long size, Long courseId) {
        // 选课模型：学生只看自己已选修课程的资源，教师/管理员不受限（与课程/作业分页口径一致）
        Long studentId = null;
        if ("STUDENT".equals(UserContext.getRole())) {
            studentId = UserContext.getUserId();
        }
        IPage<ResourceVO> result = resourceMapper.selectResourcePage(new Page<>(current, size), courseId, studentId);
        // 下载走签名 URL（Phase 20）：响应中的 fileUrl 附加有效期签名，匿名无法直接下载
        if (result.getRecords() != null) {
            result.getRecords().forEach(r -> {
                if (StringUtils.hasText(r.getFileUrl())) {
                    r.setFileUrl(signedFileUrl.sign(r.getFileUrl()));
                }
            });
        }
        return result;
    }

    /**
     * 删除资源：同时删除数据库记录与磁盘文件。
     * 秒传去重后同一物理文件可能被多条记录引用，仅当无其他记录引用时才删除磁盘文件。
     */
    public void delete(Long id) {
        CourseResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        }
        // P0-B：仅管理员或该资源所属课程的授课教师可删除
        ownershipGuard.assertCourseOwned(resource.getCourseId());
        resourceMapper.deleteById(id);
        cacheService.evict(CacheKeys.STATS_OVERVIEW);
        if (StringUtils.hasText(resource.getFileUrl())) {
            Long refCount = resourceMapper.selectCount(new LambdaQueryWrapper<CourseResource>()
                    .eq(CourseResource::getFileUrl, resource.getFileUrl()));
            if (refCount == null || refCount == 0) {
                String relativePath = resource.getFileUrl().replace(accessPrefix + "/", "");
                File f = new File(uploadDir, relativePath);
                if (f.exists() && !f.delete()) {
                    log.warn("物理文件删除失败：{}", f.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 判断扩展名是否在允许上传的白名单内（小写比较）。
     */
    private boolean isAllowedExtension(String ext) {
        if (!StringUtils.hasText(ext)) {
            return false;
        }
        String lower = ext.toLowerCase(Locale.ROOT);
        // 逐项 trim，配置写成 "pdf, doc" 也不会误拒 doc
        return Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .anyMatch(lower::equals);
    }

    /**
     * 清洗原始文件名：去除路径分隔符与控制字符、限制长度，
     * 防止目录穿越与把文件名当作前端 XSS 输入源。
     */
    private String sanitizeFileName(String name) {
        if (!StringUtils.hasText(name)) {
            return "未命名资源";
        }
        String cleaned = name.replace('\\', '_').replace('/', '_').replaceAll("[\\p{Cntrl}]", "");
        // 与库表 resource_name VARCHAR(200) 对齐，超长截断避免插入时 Data too long 报 500
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
        }
        return cleaned.trim();
    }
}
