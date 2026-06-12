package cn.edu.bcu.learning.config;

import cn.edu.bcu.learning.debug.DebugLog;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CourseMapper courseMapper;

    @Override
    public void run(String... args) {
        // #region agent log
        DebugLog.log("D", "DataInitializer.java:run", "data init start", "{}");
        // #endregion
        try {
            initCourses();
            // #region agent log
            DebugLog.log("D", "DataInitializer.java:initCourses", "mysql course init ok", "{}");
            // #endregion
        } catch (Exception e) {
            log.error("MySQL 初始化失败，请检查 SSH 隧道(3306)和数据库密码：{}", e.getMessage());
            // #region agent log
            DebugLog.log(
                    "A,B,C,D",
                    "DataInitializer.java:initCourses",
                    "mysql course init failed",
                    "{\"errorType\":\"" + e.getClass().getSimpleName() + "\",\"errorMsg\":\""
                            + (e.getMessage() == null ? "" : e.getMessage().replace("\"", "'")) + "\"}");
            // #endregion
        }
        try {
            initKnowledgeGraphs();
            // #region agent log
            DebugLog.log("F", "DataInitializer.java:initKnowledgeGraphs", "neo4j graph init ok", "{}");
            // #endregion
        } catch (Exception e) {
            log.error("Neo4j 图谱初始化失败：{}", e.getMessage());
            // #region agent log
            DebugLog.log(
                    "F",
                    "DataInitializer.java:initKnowledgeGraphs",
                    "neo4j graph init failed",
                    "{\"errorType\":\"" + e.getClass().getSimpleName() + "\",\"errorMsg\":\""
                            + (e.getMessage() == null ? "" : e.getMessage().replace("\"", "'")) + "\"}");
            // #endregion
        }
    }

    private void initCourses() {
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            return;
        }

        String[][] courses = {
                {"大数据管理与应用",           "大数据管理、存储与处理核心技术",                 "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=400&h=240&fit=crop", "mooc-kg-22718"},
                {"数据科学导论",               "数据科学基础概念、方法论与工具链",               "https://images.unsplash.com/photo-1526374965288-7f61d4dc18c5?w=400&h=240&fit=crop", "mooc-kg-40881"},
                {"人工智能基础",               "人工智能核心算法与经典模型",                     "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=400&h=240&fit=crop", "mooc-kg-42244"},
                {"智能系统设计与开发",         "智能系统的架构设计、开发流程与实践",             "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=240&fit=crop", "mooc-kg-44325"},
                {"云计算与微服务架构",         "云原生技术栈、微服务架构设计与容器化部署",       "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&h=240&fit=crop",    "mooc-kg-45459"},
                {"网络与系统安全",             "网络安全原理、攻防技术与系统安全实践",           "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=400&h=240&fit=crop",       "mooc-kg-48075"},
                {"移动应用开发",               "Android/iOS 平台应用开发核心技术",              "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=400&h=240&fit=crop",    "mooc-kg-52459"}
        };

        for (String[] courseData : courses) {
            Course course = new Course();
            course.setName(courseData[0]);
            course.setDescription(courseData[1]);
            course.setCover(courseData[2]);
            course.setSource(courseData[3]);
            course.setStatus(1);
            courseMapper.insert(course);
        }

        log.info("7 门慕课课程数据初始化完成");
    }

    private void initKnowledgeGraphs() {
        // 队友已通过数据导入脚本将七门慕课图谱写入 Neo4j，此处不再种子
        log.info("Neo4j 知识图谱已由外部导入，跳过种子初始化");
    }
}
