package com.youda.classroom.vo;

import lombok.Data;

/**
 * 课程资源上传结果（创新点 C：MD5 秒传去重）。
 * 在上传响应中扩展返回 MD5 与去重标记，前端据此提示「秒传成功」。
 */
@Data
public class ResourceUploadVO {

    /** 资源主键 */
    private Long resourceId;

    /** 文件 MD5（32 位小写十六进制） */
    private String fileMd5;

    /** 是否命中秒传去重：true = 相同文件已存在，复用已有文件、未重复落盘 */
    private boolean deduplicated;

    /** 文件访问路径 */
    private String fileUrl;
}
