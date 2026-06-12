-- ============================================
-- 第二周新增表 DDL
-- 视频播放进度 + 时间戳笔记
-- ============================================

-- 视频播放进度表
CREATE TABLE IF NOT EXISTS video_progress (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         INT         NOT NULL COMMENT '用户ID',
    video_id        INT         NOT NULL COMMENT '视频资源ID（关联 video_resource.id）',
    position        DOUBLE      DEFAULT 0 COMMENT '播放位置（秒）',
    progress        DOUBLE      DEFAULT 0 COMMENT '播放进度（百分比 0-100）',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_video (user_id, video_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频播放进度表';

-- 时间戳笔记表
CREATE TABLE IF NOT EXISTS video_notes (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id             INT          NOT NULL COMMENT '用户ID',
    video_id            INT          NOT NULL COMMENT '视频资源ID（关联 video_resource.id）',
    knowledge_point_id  VARCHAR(64)  DEFAULT NULL COMMENT '关联知识点ID（Neo4j 节点ID）',
    timestamp           DOUBLE       NOT NULL COMMENT '时间戳（秒）',
    content             TEXT         NOT NULL COMMENT '笔记内容',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_video (user_id, video_id),
    INDEX idx_user_kp (user_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间戳笔记表';
