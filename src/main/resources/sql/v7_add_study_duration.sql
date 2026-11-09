-- ============================================================
-- v7: 学生端学习时长采集（功能3-学习数据统计真实链路）
-- 在 user_behavior 上追加 duration_seconds 字段：
--   - 仅 action='study_time' 的行为记录携带真实累计停留秒数
--   - 其他行为（view_kp/click_kp/complete_quiz/start_learning）默认 0，不影响既有统计
-- 执行方式：在 MySQL 中执行本脚本一次即可（幂等性由业务层保证，重复执行会报 Duplicate column，可忽略或先查 information_schema）
-- ============================================================
ALTER TABLE user_behavior
    ADD COLUMN duration_seconds INT NOT NULL DEFAULT 0 COMMENT '学习停留时长(累计秒)，仅 action=study_time 时有效，其余行为为0' AFTER action;
