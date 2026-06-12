# 数据库系统原理 (BNU-1002842007) 课程素材导入说明

## 素材来源

- **平台**: icourse163.org
- **课程ID**: BNU-1002842007
- **本地素材根目录**: `E:\work\2026\数据库系统原理\`
- **素材结构**:
  ```
  数据库系统原理/
  ├── 测试/    (53个txt文件)
  ├── 视频/    (74个mp4文件，按章节/实验主题分多个子目录)
  └── 课件/    (148个PDF文件)
  ```

## 数据统计

| 表名 | 记录数 | 说明 |
|------|--------|------|
| question | 274 | 随堂测验题（选择题+判断题），含7道填空题/不完整题（无答案标记） |
| video_resource | 74 | 章节教学视频 + 实验辅导视频（MP4） |
| courseware_resource | 148 | PDF课件（含章节课件、实验指导、报告模板、评分标准等） |

### 题目类型分布

| 类型 | 数量 | type字段值 |
|------|------|-----------|
| 选择题 | 42 | single |
| 判断题 | 232 | judge |
| 填空题(无答案) | 7 | single (空选项) |

## SQL文件清单

| 文件 | 用途 |
|------|------|
| `resource_ddl.sql` | 建表DDL：video_resource / courseware_resource |
| `import_questions.sql` | 试题数据导入（INSERT INTO question） |
| `import_videos.sql` | 视频资源导入（INSERT INTO video_resource） |
| `import_courseware.sql` | 课件资源导入（INSERT INTO courseware_resource） |

## 导入步骤

### 第一步：建表

```sql
source E:\work\knowledge-learning-backend\src\main\resources\sql\resource_ddl.sql;
```

> question 表已存在，无需重复创建。

### 第二步：导入数据

```sql
-- 按任意顺序执行以下三个脚本
source E:\work\knowledge-learning-backend\src\main\resources\sql\import_questions.sql;
source E:\work\knowledge-learning-backend\src\main\resources\sql\import_videos.sql;
source E:\work\knowledge-learning-backend\src\main\resources\sql\import_courseware.sql;
```

### 第三步：关联课程

```sql
-- 假设课程表中 BNU-1002842007 对应的 course_id 为 X
UPDATE question           SET course_id = X WHERE course_id IS NULL;
UPDATE video_resource     SET course_id = X WHERE course_id IS NULL;
UPDATE courseware_resource SET course_id = X WHERE course_id IS NULL;
```

## 字段说明

### question 表

| 字段 | 当前值 | 说明 |
|------|--------|------|
| course_id | NULL | 待关联课程后填充 |
| knowledge_point_id | NULL | 待关联知识图谱节点后填充 |
| type | single / judge | 选择题 / 判断题 |
| content | 题干文本 | — |
| options | JSON数组 | 如 `["A. 选项1","B. 选项2"]` |
| answer | A/B/C/D等 | 正确答案选项字母 |
| analysis | 【所属章节：xxx】 | 章节信息暂存于此，关联知识点后可移至 knowledge_point_id |

### video_resource 表

| 字段 | 当前值 | 说明 |
|------|--------|------|
| course_id | NULL | 待关联课程 |
| knowledge_point_id | NULL | 待关联知识点 |
| title | 文件名（下划线替换为空格） | 可读标题 |
| file_path | 相对于素材根目录的路径 | 如 `视频\第一章 绪论  (上)\第一节 数据库_1.1 数据库_blurred.mp4` |
| duration | NULL | 待后期补充 |

### courseware_resource 表

| 字段 | 当前值 | 说明 |
|------|--------|------|
| course_id | NULL | 待关联课程 |
| knowledge_point_id | NULL | 待关联知识点 |
| title | 文件名（无后缀） | — |
| file_path | 相对于素材根目录的路径 | 如 `课件\1、认识Oracle_认识Oracle.pdf` |
| file_type | pdf | 固定值 |

## 后续事项

### 1. 知识点关联

试题的 `analysis` 字段中已标注 `【所属章节：xxx】`，可作为手动或自动关联知识点的依据。章节信息示例：

- `第一节 数据库` → 第一章第一节
- `第二节 数据库系统` → 第一章第二节
- `第三节 简单查询` → 第三章第三节
- `第1讲 数据结构的基础概念` → 补充知识

### 2. 视频时长

`video_resource.duration` 字段当前为 NULL，可通过 `ffprobe` 批量获取：

```powershell
Get-ChildItem "E:\work\2026\数据库系统原理\视频" -Recurse -Filter "*.mp4" | ForEach-Object {
    $duration = & ffprobe -v error -show_entries format=duration -of csv=p=0 $_.FullName
    # 更新对应记录的 duration
}
```

### 3. 填空题处理

7道填空题（无选项且无答案标记）已在 question 表中保留，options 为空数组，answer 为空字符串。这些题目原文件即为填空格式，需要手动补充答案后再使用。

### 4. Java 实体类

已在项目中新建：

- `domain/entity/VideoResource.java`
- `domain/entity/CoursewareResource.java`
- `repository/mysql/VideoResourceMapper.java`
- `repository/mysql/CoursewareResourceMapper.java`

遵循 MyBatis-Plus 规范，可直接通过 Mapper 进行 CRUD 操作。
