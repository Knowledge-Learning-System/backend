package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.KnowledgePointDetailVO;
import cn.edu.bcu.learning.domain.vo.StudentOverviewVO;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.UserAnswerRecordMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生端学习统计真实链路聚合服务（功能3）。
 * 口径说明：
 * - 答题范围：当前课程 source 下全部知识点（与诊断接口一致，Course 按 source 检索图谱）；
 * - 正确率：user_answer_record.is_correct 简单正确率；
 * - 掌握度分布：knowledge_mastery.mastery_level 分档（≥80 已掌握 / 60-79 良好 / <60 待加强）；
 * - 课程进度：复用 CourseService.calculateCourseProgress（掌握度均值）；
 * - 学习时长：user_behavior action=study_time 的 duration_seconds 累计。
 */
@Service
@RequiredArgsConstructor
public class StudentStatisticsService {

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final CourseService courseService;
    private final UserBehaviorService userBehaviorService;

    public StudentOverviewVO getOverview(Integer userId, Integer courseId) {
        StudentOverviewVO vo = new StudentOverviewVO();
        vo.setTotalStudySeconds(userBehaviorService.getTotalStudySeconds(userId));
        vo.setCourseProgress(courseService.calculateCourseProgress(userId, courseId));

        Course course = courseService.getCourseById(courseId);
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(course.getSource());
        if (kpIds.isEmpty()) {
            vo.setTotalQuestions(0);
            vo.setAccuracy(0);
            vo.setKpCount(0);
            vo.setMasteryDistribution(buildEmptyDistribution());
            vo.setDailyTrend(buildEmptyTrend());
            vo.setKpStats(new ArrayList<>());
            return vo;
        }

        List<UserAnswerRecord> answers = userAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, userId)
                        .in(UserAnswerRecord::getKnowledgePointId, kpIds));

        int total = answers.size();
        int correct = (int) answers.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        vo.setTotalQuestions(total);
        vo.setAccuracy(total == 0 ? 0 : Math.round(correct * 100f / total));
        vo.setKpCount((int) answers.stream()
                .map(UserAnswerRecord::getKnowledgePointId).distinct().count());

        vo.setKpStats(buildKpStats(answers));
        vo.setDailyTrend(buildDailyTrend(answers));
        vo.setMasteryDistribution(buildMasteryDistribution(userId, courseId));
        return vo;
    }

    private List<StudentOverviewVO.KpStat> buildKpStats(List<UserAnswerRecord> answers) {
        Map<String, List<UserAnswerRecord>> byKp = answers.stream()
                .collect(Collectors.groupingBy(UserAnswerRecord::getKnowledgePointId));
        List<StudentOverviewVO.KpStat> result = new ArrayList<>();
        for (Map.Entry<String, List<UserAnswerRecord>> e : byKp.entrySet()) {
            String name = knowledgeGraphRepository.findDetailById(e.getKey())
                    .map(KnowledgePointDetailVO::getName).orElse(e.getKey());
            long kpTotal = e.getValue().size();
            long kpCorrect = e.getValue().stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
            result.add(new StudentOverviewVO.KpStat(
                    e.getKey(),
                    name,
                    (int) kpTotal,
                    (int) kpCorrect,
                    kpTotal == 0 ? 0 : Math.round(kpCorrect * 100f / kpTotal)));
        }
        return result;
    }

    private List<StudentOverviewVO.DailyStat> buildDailyTrend(List<UserAnswerRecord> answers) {
        LocalDate today = LocalDate.now();
        List<StudentOverviewVO.DailyStat> trend = new ArrayList<>();
        for (int d = 13; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            trend.add(new StudentOverviewVO.DailyStat(
                    day.getMonthValue() + "/" + day.getDayOfMonth(), 0, 0));
        }
        Map<LocalDate, int[]> dayMap = new HashMap<>();
        for (UserAnswerRecord a : answers) {
            LocalDate day = a.getCreateTime() == null ? today : a.getCreateTime().toLocalDate();
            int[] c = dayMap.computeIfAbsent(day, k -> new int[2]);
            c[0]++;
            if (Boolean.TRUE.equals(a.getIsCorrect())) {
                c[1]++;
            }
        }
        for (int i = 0; i < trend.size(); i++) {
            int[] c = dayMap.get(today.minusDays(13 - i));
            if (c != null) {
                trend.get(i).setTotal(c[0]);
                trend.get(i).setCorrect(c[1]);
            }
        }
        return trend;
    }

    private List<StudentOverviewVO.NameValue> buildMasteryDistribution(Integer userId, Integer courseId) {
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        int mastered = 0;
        int good = 0;
        int weak = 0;
        for (KnowledgeMastery km : masteries) {
            int level = km.getMasteryLevel() == null ? 0 : km.getMasteryLevel();
            if (level >= 80) {
                mastered++;
            } else if (level >= 60) {
                good++;
            } else {
                weak++;
            }
        }
        List<StudentOverviewVO.NameValue> result = new ArrayList<>();
        result.add(new StudentOverviewVO.NameValue("已掌握（≥80%）", mastered));
        result.add(new StudentOverviewVO.NameValue("良好（60%-79%）", good));
        result.add(new StudentOverviewVO.NameValue("待加强（<60%）", weak));
        return result;
    }

    private List<StudentOverviewVO.NameValue> buildEmptyDistribution() {
        List<StudentOverviewVO.NameValue> result = new ArrayList<>();
        result.add(new StudentOverviewVO.NameValue("已掌握（≥80%）", 0));
        result.add(new StudentOverviewVO.NameValue("良好（60%-79%）", 0));
        result.add(new StudentOverviewVO.NameValue("待加强（<60%）", 0));
        return result;
    }

    private List<StudentOverviewVO.DailyStat> buildEmptyTrend() {
        LocalDate today = LocalDate.now();
        List<StudentOverviewVO.DailyStat> trend = new ArrayList<>();
        for (int d = 13; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            trend.add(new StudentOverviewVO.DailyStat(
                    day.getMonthValue() + "/" + day.getDayOfMonth(), 0, 0));
        }
        return trend;
    }
}
