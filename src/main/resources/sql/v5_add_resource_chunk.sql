-- ============================================
-- RAG 资料内容切块向量索引表
-- 知识学习系统 (learning_system)
-- ============================================

CREATE TABLE IF NOT EXISTS resource_chunk (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    course_id           INT          DEFAULT NULL COMMENT '关联课程ID',
    resource_id         INT          DEFAULT NULL COMMENT '关联课件资源ID(courseware_resource.id)',
    resource_title      VARCHAR(255) DEFAULT NULL COMMENT '资源标题(冗余)',
    knowledge_point_id  VARCHAR(64)  DEFAULT NULL COMMENT '关联知识点ID',
    chunk_index         INT          DEFAULT 0 COMMENT '切块序号',
    content             TEXT         COMMENT '切块正文',
    embedding           LONGTEXT     COMMENT '向量(JSON浮点数组)',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_course_id (course_id),
    INDEX idx_resource_id (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料内容切块向量索引表(RAG)';
