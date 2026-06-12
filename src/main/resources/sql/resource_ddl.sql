-- ============================================
-- 课程资源表 DDL
-- 数据库系统原理 (BNU-1002842007)
-- ============================================

-- 视频资源表
CREATE TABLE IF NOT EXISTS video_resource (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    course_id       INT          DEFAULT NULL COMMENT '关联课程ID',
    knowledge_point_id VARCHAR(64) DEFAULT NULL COMMENT '关联知识点ID',
    title           VARCHAR(255) NOT NULL    COMMENT '视频标题',
    file_path       VARCHAR(500) NOT NULL    COMMENT '文件相对路径（相对于素材根目录）',
    duration        INT          DEFAULT NULL COMMENT '时长（秒）',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_course_id (course_id),
    INDEX idx_knowledge_point_id (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频资源表';

-- 课件资源表
CREATE TABLE IF NOT EXISTS courseware_resource (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    course_id       INT          DEFAULT NULL COMMENT '关联课程ID',
    knowledge_point_id VARCHAR(64) DEFAULT NULL COMMENT '关联知识点ID',
    title           VARCHAR(255) NOT NULL    COMMENT '课件标题',
    file_path       VARCHAR(500) NOT NULL    COMMENT '文件相对路径（相对于素材根目录）',
    file_type       VARCHAR(20)  NOT NULL DEFAULT 'pdf' COMMENT '文件类型（如 pdf）',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_course_id (course_id),
    INDEX idx_knowledge_point_id (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件资源表';
