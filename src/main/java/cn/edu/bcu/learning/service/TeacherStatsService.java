package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.*;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.mysql.*;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端教学洞察服务：选课学生、班级学情、学生学习数据、预警识别、学生诊断报告。
 * 复用 CourseService.calculateCourseProgress（KnowledgeMastery 均值）与
 * DiagnosisService.getRadar / getWeakPoints（DINA 认知诊断）。
 */
@Service
@RequiredArgsConstructor
public class TeacherStatsService {

    private final CourseService courseService;
    private final UserCourseMapper userCourseMapper;
    private final UserMapper userMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final DiagnosisService diagnosisService;

    /**
     * 校验课程归属：仅允许负责该课程的教师访问。
     */
    private Course checkOwnership(Integer teacherId, Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        if (!course.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("无权访问该课程");
        }
        return course;
    }

    /**
     * 功能1：查看选课学生（含动态学习进度与加入时间）。
     */
    public List<TeacherCourseStudentVO> listCourseStudents(Integer teacherId, Integer courseId) {
        checkOwnership(teacherId, courseId);
        List<UserCourse> userCourses = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getCourseId, courseId));
        if (userCourses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> studentIds = userCourses.stream().map(UserCourse::getUserId).toList();
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .filter(user -> "student".equals(user.getRole()))
                .collect(Collectors.toMap(User::getId, user -> user));

        List<TeacherCourseStudentVO> result = new ArrayList<>();
        for (UserCourse userCourse : userCourses) {
            User user = userMap.get(userCourse.getUserId());
            if (user == null) {
                continue;
            }
            TeacherCourseStudentVO vo = new TeacherCourseStudentVO();
            vo.setStudentId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname() != null && !user.getNickname().isBlank()
                    ? user.getNickname() : user.getUsername());
            vo.setProgress(courseService.calculateCourseProgress(user.getId(), courseId));
            vo.setSelectedAt(userCourse.getSelectedAt());
            result.add(vo);
        }
        result.sort(Comparator.comparing(TeacherCourseStudentVO::getSelectedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * 功能2：班级学情统计（整体进度 + 掌握度分布）。
     */
    public ClassOverviewVO getClassOverview(Integer teacherId, Integer courseId) {
        Course course = checkOwnership(teacherId, courseId);
        List<TeacherCourseStudentVO> students = listCourseStudents(teacherId, courseId);

        ClassOverviewVO vo = new ClassOverviewVO();
        vo.setCourseId(courseId);
        vo.setCourseName(course.getName());
        vo.setStudentCount(students.size());

        int excellent = 0, good = 0, weak = 0, sum = 0;
        for (TeacherCourseStudentVO student : students) {
            int progress = student.getProgress();
            sum += progress;
            if (progress >= 80) {
                excellent++;
            } else if (progress >= 60) {
                good++;
            } else {
                weak++;
            }
        }
        vo.setExcellentCount(excellent);
        vo.setGoodCount(good);
        vo.setWeakCount(weak);
        vo.setAvgProgress(students.isEmpty() ? 0.0
                : Math.round(sum * 10.0 / students.size()) / 10.0);
        vo.setStudents(students);
        return vo;
    }

    /**
     * 功能3：学生学习数据（按时间聚合答题/行为/掌握度趋势）。
     * 说明：系统未采集"学习时长"字段，以 user_behavior 行为次数近似学习活跃度；
     * 掌握度趋势按答题记录逐日累计正确率计算（与 DiagnosisService.calculateMastery 同公式）。
     */
    public StudentLearningDataVO getStudentLearningData(Integer teacherId, Integer courseId, Integer studentId) {
        Course course = checkOwnership(teacherId, courseId);
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())) {
            throw new RuntimeException("学生不存在");
        }
        String source = course.getSource();
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);

        List<UserAnswerRecord> records = kpIds.isEmpty() ? Collections.emptyList()
                : userAnswerRecordMapper.selectList(new LambdaQueryWrapper<UserAnswerRecord>()
                        .eq(UserAnswerRecord::getUserId, studentId)
                        .in(UserAnswerRecord::getKnowledgePointId, kpIds));
        List<UserBehavior> behaviors = userBehaviorMapper.selectList(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, studentId));

        // 按日答题统计
        Map<LocalDate, int[]> dailyAnswer = new TreeMap<>(); // date -> [total, correct]
        for (UserAnswerRecord record : records) {
            if (record.getCreateTime() == null) {
                continue;
            }
            LocalDate date = record.getCreateTime().toLocalDate();
            int[] acc = dailyAnswer.computeIfAbsent(date, d -> new int[2]);
            acc[0]++;
            if (Boolean.TRUE.equals(record.getIsCorrect())) {
                acc[1]++;
            }
        }

        // 按日行为统计
        Map<LocalDate, Integer> dailyBehavior = new TreeMap<>();
        for (UserBehavior behavior : behaviors) {
            if (behavior.getCreateTime() == null) {
                continue;
            }
            LocalDate date = behavior.getCreateTime().toLocalDate();
            dailyBehavior.merge(date, 1, Integer::sum);
        }

        // 掌握度趋势：按答题日期累计正确率（与 calculateMastery 同公式）
        List<StudentLearningDataVO.MasteryTrendItem> masteryTrend = new ArrayList<>();
        records.sort(Comparator.comparing(r -> r.getCreateTime() == null ? LocalDate.MIN : r.getCreateTime().toLocalDate(),
                Comparator.nullsLast(Comparator.naturalOrder())));
        int runningTotal = 0, runningCorrect = 0;
        for (UserAnswerRecord record : records) {
            if (record.getCreateTime() == null) {
                continue;
            }
            LocalDate date = record.getCreateTime().toLocalDate();
            runningTotal++;
            if (Boolean.TRUE.equals(record.getIsCorrect())) {
                runningCorrect++;
            }
            int mastery = (int) Math.round(runningCorrect * 100.0 / runningTotal);
            if (!masteryTrend.isEmpty() && masteryTrend.get(masteryTrend.size() - 1).getDate().equals(date.toString())) {
                masteryTrend.get(masteryTrend.size() - 1).setAvgMastery((double) mastery);
            } else {
                StudentLearningDataVO.MasteryTrendItem item = new StudentLearningDataVO.MasteryTrendItem();
                item.setDate(date.toString());
                item.setAvgMastery((double) mastery);
                masteryTrend.add(item);
            }
        }

        // 每日趋势合并
        List<StudentLearningDataVO.DailyTrendItem> dailyTrend = new ArrayList<>();
        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(dailyAnswer.keySet());
        allDates.addAll(dailyBehavior.keySet());
        for (LocalDate date : allDates) {
            StudentLearningDataVO.DailyTrendItem item = new StudentLearningDataVO.DailyTrendItem();
            item.setDate(date.toString());
            int[] acc = dailyAnswer.getOrDefault(date, new int[2]);
            item.setAnswerCount(acc[0]);
            item.setAccuracy(acc[0] == 0 ? 0.0 : Math.round(acc[1] * 1000.0 / acc[0]) / 10.0);
            item.setBehaviorCount(dailyBehavior.getOrDefault(date, 0));
            dailyTrend.add(item);
        }

        // 掌握度汇总（以 knowledge_mastery 为准）
        List<KnowledgeMastery> masteryList = knowledgeMasteryMapper.selectList(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getUserId, studentId)
                .eq(KnowledgeMastery::getCourseId, courseId));
        int masteredCount = 0, masterySum = 0;
        for (KnowledgeMastery km : masteryList) {
            if (km.getMasteryLevel() != null) {
                masterySum += km.getMasteryLevel();
                if (km.getMasteryLevel() >= 60) {
                    masteredCount++;
                }
            }
        }

        StudentLearningDataVO vo = new StudentLearningDataVO();
        vo.setCourseId(courseId);
        vo.setStudentId(studentId);
        vo.setStudentName(student.getNickname() != null && !student.getNickname().isBlank()
                ? student.getNickname() : student.getUsername());
        vo.setTotalAnswers(records.size());
        vo.setCorrectAnswers(runningCorrect);
        vo.setAccuracy(records.isEmpty() ? 0.0
                : Math.round(runningCorrect * 1000.0 / records.size()) / 10.0);
        vo.setBehaviorCount(behaviors.size());
        vo.setMasteredKpCount(masteredCount);
        vo.setTotalKpCount(kpIds.size());
        vo.setAvgMastery(masteryList.isEmpty() ? 0.0
                : Math.round(masterySum * 10.0 / masteryList.size()) / 10.0);
        vo.setDailyTrend(dailyTrend);
        vo.setMasteryTrend(masteryTrend);
        return vo;
    }

    /**
     * 功能4：预警学生识别。
     * 规则：进度 < 30 判定为滞后；薄弱知识点（掌握度<60）>= 3 判定为掌握度显著偏低。
     */
    public List<WarningStudentVO> getWarningStudents(Integer teacherId, Integer courseId) {
        Course course = checkOwnership(teacherId, courseId);
        List<TeacherCourseStudentVO> students = listCourseStudents(teacherId, courseId);
        if (students.isEmpty()) {
            return Collections.emptyList();
        }
        String source = course.getSource();

        List<WarningStudentVO> result = new ArrayList<>();
        for (TeacherCourseStudentVO student : students) {
            List<WeakPointVO> weakPoints = diagnosisService.getWeakPoints(student.getStudentId(), source);
            List<String> reasons = new ArrayList<>();
            if (student.getProgress() < 30) {
                reasons.add("课程进度滞后（当前进度 " + student.getProgress() + "%）");
            }
            if (weakPoints.size() >= 3) {
                reasons.add("薄弱知识点过多（" + weakPoints.size() + " 个知识点掌握度低于 60）");
            }
            if (!reasons.isEmpty()) {
                WarningStudentVO vo = new WarningStudentVO();
                vo.setStudentId(student.getStudentId());
                vo.setUsername(student.getUsername());
                vo.setNickname(student.getNickname());
                vo.setProgress(student.getProgress());
                vo.setWeakPointCount(weakPoints.size());
                vo.setReasons(reasons);
                result.add(vo);
            }
        }
        result.sort(Comparator.comparingInt(WarningStudentVO::getProgress));
        return result;
    }

    /**
     * 功能5：学生诊断报告（雷达图 + 薄弱点 + 学习建议）。
     */
    public StudentDiagnosisVO getStudentDiagnosis(Integer teacherId, Integer courseId, Integer studentId) {
        Course course = checkOwnership(teacherId, courseId);
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())) {
            throw new RuntimeException("学生不存在");
        }
        String source = course.getSource();

        List<RadarItemVO> radar = diagnosisService.getRadar(studentId, source);
        List<WeakPointVO> weakPoints = diagnosisService.getWeakPoints(studentId, source);

        List<String> suggestions = new ArrayList<>();
        if (weakPoints.isEmpty()) {
            suggestions.add("整体掌握情况良好，建议保持当前学习节奏，并定期回顾薄弱知识点。");
        } else {
            String top = weakPoints.stream().limit(3)
                    .map(WeakPointVO::getName)
                    .collect(Collectors.joining("、"));
            suggestions.add("优先复习薄弱知识点：" + top + "。建议回看对应章节视频并完成针对性练习。");
            for (WeakPointVO weak : weakPoints.stream().limit(3).toList()) {
                suggestions.add("针对知识点「" + weak.getName() + "」：当前掌握度 " + weak.getMastery()
                        + "%，共答题 " + weak.getTotalAttempts() + " 次，错误 " + weak.getErrorCount()
                        + " 次，建议重新学习后再次测评。");
            }
        }

        double overall = radar.isEmpty() ? 0.0
                : radar.stream().mapToInt(RadarItemVO::getMastery).average().orElse(0.0);

        StudentDiagnosisVO vo = new StudentDiagnosisVO();
        vo.setCourseId(courseId);
        vo.setStudentId(studentId);
        vo.setStudentName(student.getNickname() != null && !student.getNickname().isBlank()
                ? student.getNickname() : student.getUsername());
        vo.setOverallScore(Math.round(overall * 10.0) / 10.0);
        vo.setRadar(radar);
        vo.setWeakPoints(weakPoints);
        vo.setSuggestions(suggestions);
        return vo;
    }
}
