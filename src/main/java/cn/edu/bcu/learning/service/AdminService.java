package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.DailyStudyRecord;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.Question;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.entity.UserCourse;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import cn.edu.bcu.learning.repository.mysql.CoursewareResourceMapper;
import cn.edu.bcu.learning.repository.mysql.DailyStudyRecordMapper;
import cn.edu.bcu.learning.repository.mysql.DiscussionMapper;
import cn.edu.bcu.learning.repository.mysql.HomeworkMapper;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.QuestionMapper;
import cn.edu.bcu.learning.repository.mysql.UserAnswerRecordMapper;
import cn.edu.bcu.learning.repository.mysql.UserCourseMapper;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import cn.edu.bcu.learning.repository.mysql.VideoResourceMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端数据服务：仅提供管理员视角的只读统计与用户管理能力。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Set<String> VALID_ROLES = Set.of("student", "teacher", "admin");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final UserCourseMapper userCourseMapper;
    private final VideoResourceMapper videoResourceMapper;
    private final CoursewareResourceMapper coursewareResourceMapper;
    private final QuestionMapper questionMapper;
    private final HomeworkMapper homeworkMapper;
    private final DiscussionMapper discussionMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final DailyStudyRecordMapper dailyStudyRecordMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final DataSource dataSource;
    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 系统数据概览：用户、课程、资源、教学互动等总量统计。
     */
    public Map<String, Object> overview() {
        List<User> users = userMapper.selectList(null);
        long adminCount = countRole(users, "admin");
        long teacherCount = countRole(users, "teacher");
        long studentCount = countRole(users, "student");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userTotal", users.size());
        data.put("adminCount", adminCount);
        data.put("teacherCount", teacherCount);
        data.put("studentCount", studentCount);
        data.put("courseTotal", courseMapper.selectCount(null));
        data.put("enrollTotal", userCourseMapper.selectCount(null));
        data.put("videoTotal", videoResourceMapper.selectCount(null));
        data.put("coursewareTotal", coursewareResourceMapper.selectCount(null));
        data.put("questionTotal", questionMapper.selectCount(null));
        data.put("homeworkTotal", homeworkMapper.selectCount(null));
        data.put("discussionTotal", discussionMapper.selectCount(null));

        Map<String, Long> roleStat = new LinkedHashMap<>();
        roleStat.put("student", studentCount);
        roleStat.put("teacher", teacherCount);
        roleStat.put("admin", adminCount);
        data.put("roleStat", roleStat);
        return data;
    }

    /**
     * 用户列表：支持按角色、关键词（账号/昵称）筛选与分页。
     */
    public Map<String, Object> listUsers(String role, String keyword, Integer page, Integer size) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        List<User> allUsers = userMapper.selectList(null);
        Map<Integer, Long> courseCountMap = countCoursesByUser();

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (User user : allUsers) {
            if (StringUtils.hasText(role) && !role.equalsIgnoreCase(user.getRole())) {
                continue;
            }
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim().toLowerCase();
                String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase();
                String nickname = user.getNickname() == null ? "" : user.getNickname().toLowerCase();
                if (!username.contains(kw) && !nickname.contains(kw)) {
                    continue;
                }
            }
            filtered.add(toUserRow(user, courseCountMap.getOrDefault(user.getId(), 0L)));
        }

        int from = Math.min((currentPage - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<Map<String, Object>> records = filtered.subList(from, to);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", filtered.size());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("records", new ArrayList<>(records));
        data.put("roleStat", roleTotal(allUsers));
        return data;
    }

    /**
     * 课程总览：课程基础信息 + 授课教师 + 选课人数。
     */
    public List<Map<String, Object>> listCourses() {
        List<Course> courses = courseMapper.selectList(new QueryWrapper<Course>().orderByAsc("id"));
        Map<Integer, String> teacherNameMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> buildUserName(u), (a, b) -> a));
        Map<Integer, Long> enrollMap = userCourseMapper.selectList(null).stream()
                .filter(uc -> uc.getCourseId() != null)
                .collect(Collectors.groupingBy(UserCourse::getCourseId, Collectors.counting()));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", course.getId());
            row.put("name", course.getName());
            row.put("courseCode", course.getCourseCode());
            row.put("source", course.getSource());
            row.put("status", course.getStatus());
            row.put("description", course.getDescription());
            row.put("teacherId", course.getTeacherId());
            row.put("teacherName", course.getTeacherId() == null
                    ? null : teacherNameMap.get(course.getTeacherId()));
            row.put("studentCount", enrollMap.getOrDefault(course.getId(), 0L));
            row.put("createTime", formatTime(course.getCreateTime()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 重置指定用户密码。
     */
    public void resetPassword(Integer userId, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于 6 位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 调整用户角色（不允许修改自己的角色，避免管理员失去入口）。
     */
    public void updateRole(Integer userId, String role, Integer operatorId) {
        if (userId == null || !StringUtils.hasText(role)) {
            throw new IllegalArgumentException("参数不完整");
        }
        if (!VALID_ROLES.contains(role)) {
            throw new IllegalArgumentException("角色仅支持 student / teacher / admin");
        }
        if (userId.equals(operatorId)) {
            throw new IllegalArgumentException("不能修改当前登录账号自身的角色");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 知识图谱总览：按课程 source 统计 Neo4j 图谱规模与结构明细。
     */
    public Map<String, Object> graphOverview(Integer courseId) {
        List<Course> courses = courseMapper.selectList(new QueryWrapper<Course>().orderByAsc("id"));
        List<Map<String, Object>> courseList = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("source", c.getSource());
            courseList.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courses", courseList);

        Course target = null;
        if (courseId != null) {
            target = courses.stream().filter(c -> courseId.equals(c.getId())).findFirst().orElse(null);
        }
        if (target == null) {
            target = courses.stream().filter(c -> StringUtils.hasText(c.getSource())).findFirst().orElse(null);
        }
        if (target == null || !StringUtils.hasText(target.getSource())) {
            data.put("courseId", null);
            data.put("courseName", null);
            data.put("source", null);
            data.put("kpCount", 0);
            data.put("subTopicCount", 0);
            data.put("areaCount", 0);
            data.put("prerequisiteCount", 0);
            data.put("levelStat", new LinkedHashMap<String, Long>());
            data.put("areas", new ArrayList<>());
            data.put("subTopics", new ArrayList<>());
            data.put("knowledgePoints", new ArrayList<>());
            return data;
        }

        String source = target.getSource();
        Collection<Map<String, Object>> kpRows = knowledgeGraphRepository.findAllKpBasicBySource(source);
        Collection<Map<String, Object>> subTopicRows = knowledgeGraphRepository.findSubTopicsBySourceOnly(source);
        Collection<Map<String, Object>> areaRows = knowledgeGraphRepository.findAreasBySource(source);
        List<String[]> prereqRows = knowledgeGraphRepository.findAllPrereqEdges(source);

        Map<String, Long> levelStat = new LinkedHashMap<>();
        List<Map<String, Object>> kpList = new ArrayList<>();
        for (Map<String, Object> row : kpRows) {
            String levelKey = row.get("level") == null ? "0" : String.valueOf(row.get("level"));
            levelStat.merge(levelKey, 1L, Long::sum);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.get("id"));
            m.put("name", row.get("name"));
            m.put("level", row.get("level"));
            m.put("order", row.get("order"));
            kpList.add(m);
        }

        data.put("courseId", target.getId());
        data.put("courseName", target.getName());
        data.put("source", source);
        data.put("kpCount", kpRows.size());
        data.put("subTopicCount", subTopicRows.size());
        data.put("areaCount", areaRows.size());
        data.put("prerequisiteCount", prereqRows.size());
        data.put("levelStat", levelStat);
        data.put("areas", new ArrayList<>(areaRows));
        data.put("subTopics", new ArrayList<>(subTopicRows));
        data.put("knowledgePoints", kpList);
        return data;
    }

    /**
     * 资源管理：视频与课件资源统一列表，支持类型 / 课程 / 关键词筛选与分页。
     */
    public Map<String, Object> listResources(String type, Integer courseId, String keyword,
                                             Integer page, Integer size) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;

        Map<Integer, String> courseNameMap = new HashMap<>();
        for (Course c : courseMapper.selectList(null)) {
            courseNameMap.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        boolean wantVideo = !StringUtils.hasText(type) || "video".equalsIgnoreCase(type);
        boolean wantCourseware = !StringUtils.hasText(type) || "courseware".equalsIgnoreCase(type);

        if (wantVideo) {
            QueryWrapper<VideoResource> qw = new QueryWrapper<>();
            if (courseId != null) {
                qw.eq("course_id", courseId);
            }
            for (VideoResource v : videoResourceMapper.selectList(qw)) {
                if (kw != null && !matches(kw, v.getTitle())) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", v.getId());
                m.put("type", "video");
                m.put("typeName", "视频");
                m.put("title", v.getTitle());
                m.put("courseId", v.getCourseId());
                m.put("courseName", courseNameMap.get(v.getCourseId()));
                m.put("knowledgePointId", v.getKnowledgePointId());
                m.put("fileType", "video");
                m.put("duration", v.getDuration());
                m.put("createTime", formatTime(v.getCreateTime()));
                rows.add(m);
            }
        }
        if (wantCourseware) {
            QueryWrapper<CoursewareResource> qw = new QueryWrapper<>();
            if (courseId != null) {
                qw.eq("course_id", courseId);
            }
            for (CoursewareResource c : coursewareResourceMapper.selectList(qw)) {
                if (kw != null && !matches(kw, c.getTitle())) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("type", "courseware");
                m.put("typeName", "课件");
                m.put("title", c.getTitle());
                m.put("courseId", c.getCourseId());
                m.put("courseName", courseNameMap.get(c.getCourseId()));
                m.put("knowledgePointId", c.getKnowledgePointId());
                m.put("fileType", c.getFileType());
                m.put("duration", null);
                m.put("createTime", formatTime(c.getCreateTime()));
                rows.add(m);
            }
        }

        rows.sort((a, b) -> String.valueOf(b.get("createTime")).compareTo(String.valueOf(a.get("createTime"))));

        int from = Math.min((currentPage - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());

        Map<String, Long> typeStat = new LinkedHashMap<>();
        typeStat.put("video", videoResourceMapper.selectCount(null));
        typeStat.put("courseware", coursewareResourceMapper.selectCount(null));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", rows.size());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("records", new ArrayList<>(rows.subList(from, to)));
        data.put("typeStat", typeStat);
        data.put("courses", courseMapper.selectList(null).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    return m;
                })
                .collect(Collectors.toList()));
        return data;
    }

    /**
     * 学情分析：掌握度分布、答题正确率、学习活跃度及课程维度对比。
     */
    public Map<String, Object> learningAnalysis() {
        List<KnowledgeMastery> masteryList = knowledgeMasteryMapper.selectList(null).stream()
                .filter(m -> m.getUserId() != null && m.getUserId() > 0)
                .collect(Collectors.toList());
        List<UserAnswerRecord> answerList = userAnswerRecordMapper.selectList(null).stream()
                .filter(a -> a.getUserId() != null && a.getUserId() > 0)
                .collect(Collectors.toList());
        List<DailyStudyRecord> studyList = dailyStudyRecordMapper.selectList(null).stream()
                .filter(r -> r.getUserId() != null && r.getUserId() > 0)
                .collect(Collectors.toList());

        Map<String, Long> masteryDist = new LinkedHashMap<>();
        masteryDist.put("excellent", 0L);
        masteryDist.put("good", 0L);
        masteryDist.put("pass", 0L);
        masteryDist.put("weak", 0L);
        // 仅统计产生过学习行为（掌握度 > 0 或有作答）的记录，避免未学习记录稀释均值与分布
        List<KnowledgeMastery> learnedList = masteryList.stream()
                .filter(m -> (m.getMasteryLevel() != null && m.getMasteryLevel() > 0)
                        || (m.getTotalAttempts() != null && m.getTotalAttempts() > 0))
                .collect(Collectors.toList());
        double masterySum = 0;
        for (KnowledgeMastery m : learnedList) {
            int level = m.getMasteryLevel() == null ? 0 : m.getMasteryLevel();
            masterySum += level;
            if (level >= 85) {
                masteryDist.merge("excellent", 1L, Long::sum);
            } else if (level >= 70) {
                masteryDist.merge("good", 1L, Long::sum);
            } else if (level >= 60) {
                masteryDist.merge("pass", 1L, Long::sum);
            } else {
                masteryDist.merge("weak", 1L, Long::sum);
            }
        }

        long correctCount = answerList.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        Set<Integer> activeStudents = new java.util.HashSet<>();
        masteryList.forEach(m -> activeStudents.add(m.getUserId()));
        answerList.forEach(a -> activeStudents.add(a.getUserId()));
        studyList.forEach(r -> activeStudents.add(r.getUserId()));

        Set<String> studyDayKeys = new java.util.HashSet<>();
        studyList.forEach(r -> studyDayKeys.add(r.getUserId() + "_" + r.getStudyDate()));

        Map<Integer, List<KnowledgeMastery>> masteryByCourse = masteryList.stream()
                .filter(m -> m.getCourseId() != null)
                .collect(Collectors.groupingBy(KnowledgeMastery::getCourseId));
        Map<Integer, List<UserAnswerRecord>> answerByCourse = new HashMap<>();
        Map<Integer, Integer> questionCourseMap = new HashMap<>();
        for (Question q : questionMapper.selectList(null)) {
            if (q.getId() != null && q.getCourseId() != null) {
                questionCourseMap.put(q.getId(), q.getCourseId());
            }
        }
        for (UserAnswerRecord a : answerList) {
            Integer cid = a.getQuestionId() == null ? null : questionCourseMap.get(a.getQuestionId());
            if (cid != null) {
                answerByCourse.computeIfAbsent(cid, k -> new ArrayList<>()).add(a);
            }
        }

        List<Map<String, Object>> courseAnalysis = new ArrayList<>();
        for (Course course : courseMapper.selectList(new QueryWrapper<Course>().orderByAsc("id"))) {
            List<KnowledgeMastery> courseMastery = masteryByCourse.getOrDefault(course.getId(), List.of()).stream()
                    .filter(m -> (m.getMasteryLevel() != null && m.getMasteryLevel() > 0)
                            || (m.getTotalAttempts() != null && m.getTotalAttempts() > 0))
                    .collect(Collectors.toList());
            double avg = courseMastery.stream()
                    .mapToInt(m -> m.getMasteryLevel() == null ? 0 : m.getMasteryLevel())
                    .average().orElse(0);
            List<UserAnswerRecord> courseAnswers = answerByCourse.getOrDefault(course.getId(), List.of());
            long courseCorrect = courseAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", course.getId());
            row.put("courseName", course.getName());
            row.put("masteryRecords", courseMastery.size());
            row.put("masteryAvg", Math.round(avg * 10) / 10.0);
            row.put("answerTotal", courseAnswers.size());
            row.put("correctRate", courseAnswers.isEmpty() ? 0 : Math.round(courseCorrect * 1000.0 / courseAnswers.size()) / 10.0);
            courseAnalysis.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("masteryRecords", masteryList.size());
        data.put("learnedRecords", learnedList.size());
        data.put("untouchedRecords", masteryList.size() - learnedList.size());
        data.put("masteryAvg", learnedList.isEmpty() ? 0 : Math.round(masterySum / learnedList.size() * 10) / 10.0);
        data.put("masteryDist", masteryDist);
        data.put("answerTotal", answerList.size());
        data.put("correctTotal", correctCount);
        data.put("accuracy", answerList.isEmpty() ? 0 : Math.round(correctCount * 1000.0 / answerList.size()) / 10.0);
        data.put("studyRecords", studyList.size());
        data.put("studyDays", studyDayKeys.size());
        data.put("activeStudents", activeStudents.size());
        data.put("courseAnalysis", courseAnalysis);
        return data;
    }

    /**
     * 系统信息：运行环境、JVM 内存、数据库版本与核心表数据量。
     */
    public Map<String, Object> systemInfo() {
        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeProfiles", String.join(",", environment.getActiveProfiles()));
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("osName", System.getProperty("os.name"));
        data.put("osArch", System.getProperty("os.arch"));
        data.put("cpuCores", rt.availableProcessors());
        data.put("heapMaxMb", totalMem / 1024 / 1024);
        data.put("heapUsedMb", (totalMem - freeMem) / 1024 / 1024);
        data.put("uptimeHours", Math.round(uptimeMs / 36000.0) / 100.0);
        data.put("startTime", LocalDateTime.now().minusSeconds(uptimeMs / 1000).format(TIME_FMT));
        data.put("serverTime", LocalDateTime.now().format(TIME_FMT));

        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            db.put("product", meta.getDatabaseProductName());
            db.put("version", meta.getDatabaseProductVersion());
            db.put("url", maskUrl(meta.getURL()));
        } catch (Exception e) {
            db.put("error", e.getMessage());
        }
        data.put("database", db);

        Map<String, Object> tableRows = new LinkedHashMap<>();
        tableRows.put("user", userMapper.selectCount(null));
        tableRows.put("course", courseMapper.selectCount(null));
        tableRows.put("user_course", userCourseMapper.selectCount(null));
        tableRows.put("video_resource", videoResourceMapper.selectCount(null));
        tableRows.put("courseware_resource", coursewareResourceMapper.selectCount(null));
        tableRows.put("question", questionMapper.selectCount(null));
        tableRows.put("homework", homeworkMapper.selectCount(null));
        tableRows.put("discussion", discussionMapper.selectCount(null));
        tableRows.put("knowledge_mastery", knowledgeMasteryMapper.selectCount(null));
        tableRows.put("user_answer_record", userAnswerRecordMapper.selectCount(null));
        tableRows.put("daily_study_record", dailyStudyRecordMapper.selectCount(null));
        data.put("tableRows", tableRows);
        return data;
    }

    /**
     * 推荐策略配置读取。
     */
    public Map<String, Object> recommendStrategy() {
        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT strategy_key, strategy_name, weight, enabled, remark, update_time "
                        + "FROM recommend_strategy ORDER BY id",
                (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", rs.getString("strategy_key"));
                    m.put("name", rs.getString("strategy_name"));
                    m.put("weight", rs.getInt("weight"));
                    m.put("enabled", rs.getInt("enabled") == 1);
                    m.put("remark", rs.getString("remark"));
                    m.put("updateTime", formatTime(rs.getTimestamp("update_time") == null
                            ? null : rs.getTimestamp("update_time").toLocalDateTime()));
                    return m;
                });

        int totalWeight = 0;
        int enabledCount = 0;
        for (Map<String, Object> item : items) {
            if (Boolean.TRUE.equals(item.get("enabled"))) {
                totalWeight += (Integer) item.get("weight");
                enabledCount++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("totalWeight", totalWeight);
        data.put("enabledCount", enabledCount);
        data.put("balanced", totalWeight == 100);
        return data;
    }

    /**
     * 推荐策略配置更新：逐项校验权重范围后写库。
     */
    public void updateRecommendStrategy(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("策略配置不能为空");
        }
        for (Map<String, Object> item : items) {
            Object keyObj = item.get("key");
            if (keyObj == null || !StringUtils.hasText(String.valueOf(keyObj))) {
                throw new IllegalArgumentException("策略标识不能为空");
            }
            String key = String.valueOf(keyObj);
            Object weightObj = item.get("weight");
            int weight;
            try {
                weight = weightObj == null ? 0 : Integer.parseInt(String.valueOf(weightObj));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("权重必须为整数：" + key);
            }
            if (weight < 0 || weight > 100) {
                throw new IllegalArgumentException("权重需在 0-100 之间：" + key);
            }
            Object enabledObj = item.get("enabled");
            boolean enabled = enabledObj == null || Boolean.parseBoolean(String.valueOf(enabledObj));
            int updated = jdbcTemplate.update(
                    "UPDATE recommend_strategy SET weight = ?, enabled = ?, update_time = ? WHERE strategy_key = ?",
                    weight, enabled ? 1 : 0, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), key);
            if (updated == 0) {
                throw new IllegalArgumentException("未知策略项：" + key);
            }
        }
    }

    private String maskUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("(?i)(password=)[^&;]*", "$1***");
    }

    private boolean matches(String keyword, String title) {
        return title != null && title.toLowerCase().contains(keyword);
    }

    private Map<String, Object> toUserRow(User user, Long courseCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("nickname", user.getNickname());
        row.put("role", user.getRole());
        row.put("avatar", user.getAvatar());
        row.put("currentCourseId", user.getCurrentCourseId());
        row.put("courseCount", courseCount);
        row.put("createTime", formatTime(user.getCreateTime()));
        return row;
    }

    private Map<Integer, Long> countCoursesByUser() {
        Map<Integer, Long> map = new HashMap<>();
        for (UserCourse uc : userCourseMapper.selectList(null)) {
            if (uc.getUserId() != null) {
                map.merge(uc.getUserId(), 1L, Long::sum);
            }
        }
        return map;
    }

    private Map<String, Long> roleTotal(List<User> users) {
        Map<String, Long> stat = new LinkedHashMap<>();
        stat.put("student", countRole(users, "student"));
        stat.put("teacher", countRole(users, "teacher"));
        stat.put("admin", countRole(users, "admin"));
        return stat;
    }

    private long countRole(List<User> users, String role) {
        return users.stream().filter(u -> role.equalsIgnoreCase(u.getRole())).count();
    }

    private String buildUserName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FMT);
    }
}
