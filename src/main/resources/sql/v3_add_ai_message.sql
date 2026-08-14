-- ============================================
-- V3：AI助手消息记录表
-- ============================================

CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    course_id       BIGINT       NOT NULL COMMENT '课程ID',
    role            VARCHAR(16)  NOT NULL COMMENT '角色：user / assistant',
    content         TEXT         NOT NULL COMMENT '消息内容',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_course (user_id, course_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手对话记录表';
