package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final ResourceService resourceService;
    private final QuestionService questionService;

    public KnowledgeGraphVO getGraphBySource(String source) {
        return knowledgeGraphRepository.findGraphBySource(source);
    }

    public KnowledgePointDetailVO getKnowledgePointDetail(String id) {
        return knowledgeGraphRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("知识点不存在"));
    }

    public List<LearningPathItemVO> getLearningPathBySource(String source) {
        return knowledgeGraphRepository.findLearningPathBySource(source);
    }

    public List<KnowledgePointDetailVO> getPrerequisites(String kpId) {
        return knowledgeGraphRepository.findPrerequisites(kpId);
    }

    /**
     * 获取某课程的完整章-知识点树形结构，包含视频/课件/测试题资源
     */
    public List<SubTopicVO> getChapterStructure(Integer courseId, String source) {
        // 1. 查询所有 SubTopic
        Collection<Map<String, Object>> subTopicRows = knowledgeGraphRepository.findSubTopicsBySource(source);

        // 2. 收集所有 SubTopic 下的所有 KP ID，并构建 SubTopic → KP 映射
        List<SubTopicVO> subTopics = new ArrayList<>();
        List<String> allKpIds = new ArrayList<>();
        // 暂存 subTopic 的 KP 行数据，供后续构建树使用
        Map<String, Collection<Map<String, Object>>> stKpRowsMap = new LinkedHashMap<>();

        for (Map<String, Object> stRow : subTopicRows) {
            SubTopicVO st = new SubTopicVO();
            String stId = String.valueOf(stRow.get("id"));
            st.setId(stId);
            st.setName(String.valueOf(stRow.get("name")));
            st.setDescription(stRow.get("description") == null ? "" : String.valueOf(stRow.get("description")));
            st.setOrder(((Number) stRow.get("order")).intValue());
            subTopics.add(st);

            Collection<Map<String, Object>> kpRows =
                    knowledgeGraphRepository.findKnowledgePointsBySubTopic(source, stId);
            stKpRowsMap.put(stId, kpRows);

            for (Map<String, Object> kpRow : kpRows) {
                allKpIds.add(String.valueOf(kpRow.get("id")));
            }
        }

        // 3. 批量按 KP ID（IN 查询）拉取所有资源
        List<VideoResource> allVideos = resourceService.getVideosByKpIds(courseId, allKpIds);
        List<CoursewareResource> allCourseware = resourceService.getCoursewareByKpIds(courseId, allKpIds);
        List<AnswerDetailVO> allQuestions = questionService.getQuestionsByKpIds(courseId, allKpIds);

        // 按知识点分组资源
        Map<String, List<VideoResource>> videoMap = allVideos.stream()
                .filter(v -> v.getKnowledgePointId() != null)
                .collect(Collectors.groupingBy(VideoResource::getKnowledgePointId));
        Map<String, List<CoursewareResource>> coursewareMap = allCourseware.stream()
                .filter(c -> c.getKnowledgePointId() != null)
                .collect(Collectors.groupingBy(CoursewareResource::getKnowledgePointId));
        Map<String, List<AnswerDetailVO>> questionMap = allQuestions.stream()
                .filter(q -> q.getKnowledgePointId() != null)
                .collect(Collectors.groupingBy(AnswerDetailVO::getKnowledgePointId));

        // 4. 为每个 SubTopic 构建知识点树并挂资源
        for (SubTopicVO st : subTopics) {
            Collection<Map<String, Object>> kpRows = stKpRowsMap.get(st.getId());
            if (kpRows == null || kpRows.isEmpty()) {
                continue;
            }

            List<String> kpIds = kpRows.stream()
                    .map(r -> String.valueOf(r.get("id")))
                    .collect(Collectors.toList());

            Map<String, KnowledgePointTreeNodeVO> nodeMap = new HashMap<>();
            for (Map<String, Object> kpRow : kpRows) {
                KnowledgePointTreeNodeVO node = new KnowledgePointTreeNodeVO();
                node.setId(String.valueOf(kpRow.get("id")));
                node.setName(String.valueOf(kpRow.get("name")));
                node.setDescription(kpRow.get("description") == null ? "" : String.valueOf(kpRow.get("description")));
                node.setLevel(((Number) kpRow.get("level")).intValue());
                // 挂资源
                node.setVideos(videoMap.getOrDefault(node.getId(), Collections.emptyList()));
                node.setCoursewares(coursewareMap.getOrDefault(node.getId(), Collections.emptyList()));
                node.setQuestions(questionMap.getOrDefault(node.getId(), Collections.emptyList()));
                nodeMap.put(node.getId(), node);
            }

            // 查询 PARENT_KP 关系
            Collection<Map<String, Object>> parentKpRows =
                    knowledgeGraphRepository.findParentKpRelations(source, kpIds);

            Map<String, List<String>> childParentMap = new HashMap<>();
            for (Map<String, Object> row : parentKpRows) {
                String parentId = String.valueOf(row.get("parentId"));
                String childId = String.valueOf(row.get("childId"));
                childParentMap.computeIfAbsent(childId, k -> new ArrayList<>()).add(parentId);
            }

            // 构建树：无 parent 的为根节点
            Set<String> childIds = childParentMap.keySet();
            for (Map.Entry<String, KnowledgePointTreeNodeVO> entry : nodeMap.entrySet()) {
                String kpId = entry.getKey();
                if (!childIds.contains(kpId)) {
                    st.getKnowledgePoints().add(entry.getValue());
                }
            }

            // 将子节点挂到父节点下
            for (Map.Entry<String, List<String>> e : childParentMap.entrySet()) {
                String childId = e.getKey();
                KnowledgePointTreeNodeVO childNode = nodeMap.get(childId);
                if (childNode == null) continue;

                for (String parentId : e.getValue()) {
                    KnowledgePointTreeNodeVO parentNode = nodeMap.get(parentId);
                    if (parentNode != null) {
                        parentNode.getChildren().add(childNode);
                    }
                }
            }
        }

        return subTopics;
    }
}
