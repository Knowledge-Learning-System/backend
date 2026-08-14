-- 学生管理模块：user 表补充 nickname（姓名）字段
-- 用于教师端学生列表展示与学生 Excel 批量导入（学号→username、姓名→nickname）
USE learning_system;

ALTER TABLE `user`
    ADD COLUMN `nickname` VARCHAR(50) DEFAULT NULL COMMENT '姓名/昵称' AFTER `avatar`;
