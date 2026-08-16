-- 讨论区主表
CREATE TABLE IF NOT EXISTS discussion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT,
    video_id INT,
    knowledge_point_id VARCHAR(255),
    user_id INT NOT NULL,
    title VARCHAR(500),
    content TEXT NOT NULL,
    reply_count INT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 讨论回复表
CREATE TABLE IF NOT EXISTS discussion_reply (
    id INT AUTO_INCREMENT PRIMARY KEY,
    discussion_id INT NOT NULL,
    user_id INT NOT NULL,
    reply_to_id INT,
    content TEXT NOT NULL,
    create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习计划表
CREATE TABLE IF NOT EXISTS study_plan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    course_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    daily_hours DECIMAL(3,1) NOT NULL DEFAULT 1.0,
    create_time DATETIME,
    update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 课程编号字段
ALTER TABLE course ADD COLUMN IF NOT EXISTS course_code VARCHAR(20) AFTER id;
UPDATE course SET course_code = CONCAT('cs', 1000 + id) WHERE course_code IS NULL;
