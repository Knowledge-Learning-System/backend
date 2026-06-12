-- 第一部分：题库与诊断模块 DDL
-- 执行前请确保 course 表已新增 source 字段：ALTER TABLE course ADD COLUMN source VARCHAR(64);

-- 题库表
CREATE TABLE IF NOT EXISTS question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL COMMENT '课程ID',
    knowledge_point_id VARCHAR(64) COMMENT 'Neo4j知识点节点id',
    type VARCHAR(32) NOT NULL DEFAULT 'single' COMMENT 'single / multiple',
    content VARCHAR(1024) NOT NULL COMMENT '题目内容',
    options TEXT COMMENT 'JSON: ["A.xxx","B.xxx"]',
    answer VARCHAR(32) NOT NULL COMMENT '正确答案',
    analysis VARCHAR(1024) COMMENT '解析',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course_kp (course_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 答题记录表
CREATE TABLE IF NOT EXISTS user_answer_record (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL COMMENT 'Neo4j知识点节点id',
    user_answer VARCHAR(32) NOT NULL,
    is_correct TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_kp (user_id, knowledge_point_id),
    INDEX idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 掌握度表
CREATE TABLE IF NOT EXISTS knowledge_mastery (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL COMMENT 'Neo4j知识点节点id',
    course_id INT NOT NULL,
    mastery_level INT DEFAULT 0 COMMENT '掌握度 0-100',
    total_attempts INT DEFAULT 0,
    correct_attempts INT DEFAULT 0,
    last_attempt_time DATETIME,
    UNIQUE KEY uk_user_kp (user_id, knowledge_point_id),
    INDEX idx_user_course (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户行为表
CREATE TABLE IF NOT EXISTS user_behavior (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    knowledge_point_id VARCHAR(64) COMMENT '知识点id，非交互场景可为空',
    action VARCHAR(32) NOT NULL COMMENT 'view_kp / click_kp / complete_quiz / start_learning',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_action (user_id, action, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
