package com.youda.classroom.service;

import com.youda.classroom.common.BusinessException;
import com.youda.classroom.entity.CourseResource;
import com.youda.classroom.mapper.CourseResourceMapper;
import com.youda.classroom.security.UserContext;
import com.youda.classroom.vo.ResourceUploadVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资源上传服务单测（创新点 C 核心逻辑）：
 * 用 Mockito 隔离 Mapper、临时目录承接落盘，直接验证「MD5 去重」「引用计数删除」两条分支，
 * 与 FileMd5Test（算法）和 ResourceControllerTest（HTTP 层）共同构成三层测试闭环。
 */
class ResourceServiceTest {

    @TempDir
    Path tempDir;

    private CourseResourceMapper resourceMapper;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceMapper = mock(CourseResourceMapper.class);
        resourceService = new ResourceService(resourceMapper);
        ReflectionTestUtils.setField(resourceService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resourceService, "accessPrefix", "/files");
        ReflectionTestUtils.setField(resourceService, "dedupEnabled", true);
        ReflectionTestUtils.setField(resourceService, "allowedExtensions",
                "pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,zip,png,jpg,jpeg,gif");
        UserContext.set(new UserContext.CurrentUser(1L, "teacher", "TEACHER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "a.pdf", "application/pdf",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadNewFileWritesDiskAndPersistsMd5() throws Exception {
        when(resourceMapper.selectOne(any())).thenReturn(null);

        ResourceUploadVO vo = resourceService.upload(1L, "PDF", file("hello"));

        assertFalse(vo.isDeduplicated());
        assertEquals("5d41402abc4b2a76b9719d911017c592", vo.getFileMd5()); // MD5("hello")
        // 落盘：courseId=1 目录下恰好一个文件
        List<Path> files = Files.list(tempDir.resolve("1")).toList();
        assertEquals(1, files.size());
        // 元数据写入并携带 MD5
        ArgumentCaptor<CourseResource> captor = ArgumentCaptor.forClass(CourseResource.class);
        verify(resourceMapper).insert(captor.capture());
        assertEquals("5d41402abc4b2a76b9719d911017c592", captor.getValue().getFileMd5());
        assertTrue(captor.getValue().getFileUrl().startsWith("/files/1/"));
    }

    @Test
    void uploadSameMd5ReusesExistingFileWithoutDiskWrite() throws Exception {
        CourseResource existing = new CourseResource();
        existing.setFileUrl("/files/1/existing.pdf");
        when(resourceMapper.selectOne(any())).thenReturn(existing);

        ResourceUploadVO vo = resourceService.upload(2L, "PDF", file("hello"));

        assertTrue(vo.isDeduplicated());
        assertEquals("/files/1/existing.pdf", vo.getFileUrl());
        verify(resourceMapper).insert(any(CourseResource.class));
        // 未重复落盘：课程 2 的目录根本未创建
        assertFalse(Files.exists(tempDir.resolve("2")), "秒传命中时不应再落盘新文件");
    }

    @Test
    void deleteKeepsSharedFileWhenOtherRecordsReferenceIt() throws Exception {
        CourseResource res = new CourseResource();
        res.setId(10L);
        res.setFileUrl("/files/1/a.pdf");
        when(resourceMapper.selectById(10L)).thenReturn(res);
        when(resourceMapper.selectCount(any())).thenReturn(2L); // 仍有 2 条记录引用

        Files.createDirectories(tempDir.resolve("1"));
        Files.write(tempDir.resolve("1").resolve("a.pdf"), "hello".getBytes(StandardCharsets.UTF_8));

        resourceService.delete(10L);

        verify(resourceMapper).deleteById(10L);
        assertTrue(Files.exists(tempDir.resolve("1").resolve("a.pdf")), "仍有引用时物理文件应保留");
    }

    @Test
    void deleteRemovesFileWhenNoOtherRecordsReferenceIt() throws Exception {
        CourseResource res = new CourseResource();
        res.setId(11L);
        res.setFileUrl("/files/1/b.pdf");
        when(resourceMapper.selectById(11L)).thenReturn(res);
        when(resourceMapper.selectCount(any())).thenReturn(0L); // 无其它引用

        Files.createDirectories(tempDir.resolve("1"));
        Files.write(tempDir.resolve("1").resolve("b.pdf"), "hello".getBytes(StandardCharsets.UTF_8));

        resourceService.delete(11L);

        verify(resourceMapper).deleteById(11L);
        assertFalse(Files.exists(tempDir.resolve("1").resolve("b.pdf")), "无引用时应删除物理文件");
    }

    @Test
    void uploadEmptyFileThrowsBusinessException() {
        when(resourceMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> resourceService.upload(1L, "PDF", file("")));
    }

    @Test
    void uploadDisallowedExtensionThrowsBusinessException() {
        MockMultipartFile html = new MockMultipartFile("file", "evil.html", "text/html",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        assertThrows(BusinessException.class,
                () -> resourceService.upload(1L, "OTHER", html));
    }
}
