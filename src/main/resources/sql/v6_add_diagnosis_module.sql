-- ============================================
-- 第六周新增表 DDL：主观题判分 + 诊断链路
-- 四张新表：
--   question_knowledge   试题-知识点关联表
--   ai_grading_record    AI 判分记录表
--   study_session        学习会话表
--   knowledge_study_log  知识点学习日志表
-- 字段扩展：
--   question             + question_type / answer_text
--   homework_submission  + auto_score / final_score / grade_status
--   video_resource       + knowledge_id / video_url / preview_url
-- ============================================

-- ---------- 一、新表 ----------

-- 试题-知识点关联表（多对多：一道题可关联多个知识点）
CREATE TABLE IF NOT EXISTS question_knowledge (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    question_id         INT          NOT NULL COMMENT '试题ID（关联 question.id）',
    knowledge_point_id  VARCHAR(64)  NOT NULL COMMENT '知识点ID（Neo4j 节点ID）',
    relation_type       VARCHAR(32)  DEFAULT 'primary' COMMENT '关联类型：primary / secondary',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_question_kp (question_id, knowledge_point_id),
    INDEX idx_kp (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题-知识点关联表';

-- AI 判分记录表（主观题逐次判分结果）
CREATE TABLE IF NOT EXISTS ai_grading_record (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    submission_id       INT          NOT NULL COMMENT '作业提交ID（关联 homework_submission.id）',
    user_id             INT          NOT NULL COMMENT '用户ID',
    question_id         INT          NOT NULL COMMENT '试题ID（关联 question.id）',
    question_type       VARCHAR(32)  DEFAULT 'subjective' COMMENT '题型：subjective / objective',
    user_answer         TEXT         COMMENT '用户作答内容',
    standard_answer     TEXT         COMMENT '参考答案（来自 question.answer_text）',
    auto_score          INT          DEFAULT NULL COMMENT 'AI 自动评分 0-100',
    final_score         INT          DEFAULT NULL COMMENT '最终得分（人工复核后）',
    grade_status        VARCHAR(16)  DEFAULT 'pending' COMMENT '判分状态：pending / graded / confirmed',
    grading_detail      JSON         DEFAULT NULL COMMENT 'AI 判分详情（维度得分等）',
    model_name          VARCHAR(64)  DEFAULT NULL COMMENT '判分模型标识',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_submission (submission_id),
    INDEX idx_user (user_id),
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 判分记录表';

-- 学习会话表（一次进入学习模块的完整会话）
CREATE TABLE IF NOT EXISTS study_session (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id             INT          NOT NULL COMMENT '用户ID',
    course_id           INT          DEFAULT NULL COMMENT '课程ID',
    start_time          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '会话开始时间',
    end_time            DATETIME     DEFAULT NULL COMMENT '会话结束时间',
    duration_seconds    INT          DEFAULT 0 COMMENT '会话时长（秒）',
    source              VARCHAR(32)  DEFAULT NULL COMMENT '会话来源：graph / quiz / video / dashboard',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_start (user_id, start_time),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习会话表';

-- 知识点学习日志表（会话内每个知识点的停留与作答记录）
CREATE TABLE IF NOT EXISTS knowledge_study_log (
    id                  INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id          INT          NOT NULL COMMENT '学习会话ID（关联 study_session.id）',
    user_id             INT          NOT NULL COMMENT '用户ID',
    knowledge_point_id  VARCHAR(64)  NOT NULL COMMENT '知识点ID（Neo4j 节点ID）',
    question_id         INT          DEFAULT NULL COMMENT '关联试题ID（若在本知识点答题）',
    answer_correct      TINYINT(1)   DEFAULT NULL COMMENT '是否答对：1 对 / 0 错 / NULL 未答题',
    study_duration      INT          DEFAULT 0 COMMENT '本知识点停留时长（秒）',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session (session_id),
    INDEX idx_user_kp (user_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点学习日志表';

-- ---------- 二、字段扩展 ----------

-- question：新增题型细分与参考答案文本
ALTER TABLE question
    ADD COLUMN question_type VARCHAR(32) DEFAULT 'choice' COMMENT '题型细分：choice / judge / fill / subjective'
        AFTER type,
    ADD COLUMN answer_text TEXT COMMENT '参考答案（主观题使用，客观题可为空）'
        AFTER answer;

-- homework_submission：新增 AI 判分相关字段
ALTER TABLE homework_submission
    ADD COLUMN auto_score INT DEFAULT NULL COMMENT 'AI 自动评分 0-100'
        AFTER score,
    ADD COLUMN final_score INT DEFAULT NULL COMMENT '最终得分（人工复核后）'
        AFTER auto_score,
    ADD COLUMN grade_status VARCHAR(16) DEFAULT 'pending' COMMENT '判分状态：pending / graded / confirmed'
        AFTER final_score;

-- video_resource：新增图谱关联与播放地址字段
ALTER TABLE video_resource
    ADD COLUMN knowledge_id VARCHAR(64) DEFAULT NULL COMMENT 'Neo4j 知识点节点ID'
        AFTER knowledge_point_id,
    ADD COLUMN video_url VARCHAR(500) DEFAULT NULL COMMENT '视频播放地址（可空，为空时用 file_path 拼接）'
        AFTER file_path,
    ADD COLUMN preview_url VARCHAR(500) DEFAULT NULL COMMENT '视频预览图地址（图谱节点缩略图）'
        AFTER video_url;
