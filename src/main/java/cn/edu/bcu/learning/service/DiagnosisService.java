package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.UserAnswerRecord;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.UserAnswerRecordMapper;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final CourseService courseService;

    /**
     * 根据答题记录计算所有知识点的掌握度，写入 knowledge_mastery 表。
     * 公式：mastery = correctCount / totalAttempts * 100
     */
    @Async
    public void calculateMastery(Integer userId, Integer courseId, String source) {
        try {
            List<String> allKpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
            if (allKpIds.isEmpty()) return;

            List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(
                    new LambdaQueryWrapper<UserAnswerRecord>()
                            .eq(UserAnswerRecord::getUserId, userId)
                            .in(UserAnswerRecord::getKnowledgePointId, allKpIds));

            // 按知识点分组统计
            Map<String, Integer> totalMap = new HashMap<>();
            Map<String, Integer> correctMap = new HashMap<>();
            for (UserAnswerRecord r : records) {
                String kpId = r.getKnowledgePointId();
                totalMap.merge(kpId, 1, Integer::sum);
                if (Boolean.TRUE.equals(r.getIsCorrect())) {
                    correctMap.merge(kpId, 1, Integer::sum);
                }
            }

            for (String kpId : allKpIds) {
                int total = totalMap.getOrDefault(kpId, 0);
                int correct = correctMap.getOrDefault(kpId, 0);
                int mastery = total == 0 ? 0 : (int) Math.round((double) correct / total * 100);

                // upsert
                KnowledgeMastery km = knowledgeMasteryMapper.selectOne(
                        new LambdaQueryWrapper<KnowledgeMastery>()
                                .eq(KnowledgeMastery::getUserId, userId)
                                .eq(KnowledgeMastery::getKnowledgePointId, kpId));
                if (km == null) {
                    km = new KnowledgeMastery();
                    km.setUserId(userId);
                    km.setKnowledgePointId(kpId);
                    km.setCourseId(courseId);
                }
                km.setMasteryLevel(mastery);
                km.setTotalAttempts(total);
                km.setCorrectAttempts(correct);
                km.setLastAttemptTime(LocalDateTime.now());
                knowledgeMasteryMapper.insertOrUpdate(km);
            }
        } catch (Exception e) {
            log.error("异步计算掌握度失败 userId={} courseId={}", userId, courseId, e);
        }
    }

    /**
     * 雷达图数据：所有知识点的掌握度列表
     */
    public List<RadarItemVO> getRadar(Integer userId, String source) {
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (kpIds.isEmpty()) return Collections.emptyList();

        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .in(KnowledgeMastery::getKnowledgePointId, kpIds));

        Map<String, Integer> masteryMap = masteries.stream()
                .collect(Collectors.toMap(KnowledgeMastery::getKnowledgePointId, KnowledgeMastery::getMasteryLevel));

        List<RadarItemVO> result = new ArrayList<>();
        for (String kpId : kpIds) {
            String name = knowledgeGraphRepository.findDetailById(kpId)
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(kpId);
            result.add(new RadarItemVO(kpId, name, masteryMap.getOrDefault(kpId, 0)));
        }
        return result;
    }

    /**
     * 薄弱点查询：掌握度 < 60 或答题错误率 > 40%
     */
    public List<WeakPointVO> getWeakPoints(Integer userId, String source) {
        List<String> kpIds = knowledgeGraphRepository.findAllKpIdsBySource(source);
        if (kpIds.isEmpty()) return Collections.emptyList();

        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .in(KnowledgeMastery::getKnowledgePointId, kpIds));

        List<WeakPointVO> result = new ArrayList<>();
        for (KnowledgeMastery km : masteries) {
            if (km.getMasteryLevel() == null || km.getMasteryLevel() >= 60) continue;

            String name = knowledgeGraphRepository.findDetailById(km.getKnowledgePointId())
                    .map(KnowledgePointDetailVO::getName)
                    .orElse(km.getKnowledgePointId());
            int errorCount = km.getTotalAttempts() - km.getCorrectAttempts();
            result.add(new WeakPointVO(km.getKnowledgePointId(), name, km.getMasteryLevel(),
                    km.getTotalAttempts(), errorCount));
        }
        return result;
    }
}
