package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.*;
import cn.edu.bcu.learning.repository.neo4j.KnowledgeGraphRepository;
import cn.edu.bcu.learning.util.GraphImportParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    // ==================== 教师端图谱表格导入 ====================

    /**
     * 解析教师上传的节点/关系表格并写入 Neo4j。
     * 支持创建 / 修改 / 删除节点与关系；节点表 parentId 列可自动生成层级关系。
     */
    public GraphImportResultVO importGraph(String source, MultipartFile[] files) {
        GraphImportParser parser = new GraphImportParser();
        GraphImportResultVO result = new GraphImportResultVO();
        int execFail = 0;

        try {
            GraphImportParser.ParseResult parsed = parser.parse(files);

            // 1. 节点：创建 / 修改 / 删除
            for (GraphImportParser.NodeRow row : parsed.nodes) {
                String type = GraphImportParser.normalizeNodeType(row.type);
                if (type == null) {
                    execFail++;
                    result.getFailDetails().add("节点 [" + row.id + "] 类型不合法：" + row.type);
                    continue;
                }
                String nodeSource = (row.source != null && !row.source.isEmpty()) ? row.source : source;
                try {
                    if (GraphImportParser.OP_DELETE.equals(row.operation)) {
                        knowledgeGraphRepository.deleteNode(nodeSource, type, row.id);
                        result.setDeletedNodes(result.getDeletedNodes() + 1);
                    } else {
                        knowledgeGraphRepository.upsertNode(nodeSource, type, row.id, row.name,
                                row.description, row.level, row.group, row.order);
                        if (GraphImportParser.OP_UPDATE.equals(row.operation)) {
                            result.setUpdatedNodes(result.getUpdatedNodes() + 1);
                        } else {
                            result.setCreatedNodes(result.getCreatedNodes() + 1);
                        }
                    }
                    result.setNodeTotal(result.getNodeTotal() + 1);
                } catch (Exception e) {
                    execFail++;
                    result.getFailDetails().add("节点 [" + row.id + "] 写入失败：" + e.getMessage());
                }
            }

            // 2. 节点表 parentId → 自动生成层级关系
            for (GraphImportParser.NodeRow row : parsed.nodes) {
                if (row.parentId == null || row.parentId.isEmpty()) {
                    continue;
                }
                if (GraphImportParser.OP_DELETE.equals(row.operation)) {
                    continue;
                }
                String type = GraphImportParser.normalizeNodeType(row.type);
                if (type == null) {
                    continue;
                }
                String nodeSource = (row.source != null && !row.source.isEmpty()) ? row.source : source;
                try {
                    autoBuildParentRelation(nodeSource, type, row);
                    result.setAutoRelations(result.getAutoRelations() + 1);
                } catch (Exception e) {
                    execFail++;
                    result.getFailDetails().add("节点 [" + row.id + "] 父节点关系生成失败：" + e.getMessage());
                }
            }

            // 3. 关系：创建 / 修改 / 删除
            for (GraphImportParser.RelationRow row : parsed.relations) {
                String relType = GraphImportParser.normalizeRelationType(row.type);
                if (relType == null) {
                    execFail++;
                    result.getFailDetails().add("关系 [" + row.sourceId + " → " + row.targetId + "] 类型不合法：" + row.type);
                    continue;
                }
                String srcType = GraphImportParser.normalizeNodeType(row.sourceType);
                String tgtType = GraphImportParser.normalizeNodeType(row.targetType);
                if (srcType == null || tgtType == null) {
                    String[] endpoints = GraphImportParser.defaultEndpointsFor(relType);
                    if (endpoints == null) {
                        execFail++;
                        result.getFailDetails().add("关系 [" + row.sourceId + " → " + row.targetId + "] 无法推断端点类型，请在表格中填写源类型/目标类型");
                        continue;
                    }
                    srcType = srcType != null ? srcType : endpoints[0];
                    tgtType = tgtType != null ? tgtType : endpoints[1];
                }
                try {
                    if (GraphImportParser.OP_DELETE.equals(row.operation)) {
                        knowledgeGraphRepository.deleteRelation(source, srcType, row.sourceId,
                                tgtType, row.targetId, relType);
                        result.setDeletedRelations(result.getDeletedRelations() + 1);
                    } else {
                        knowledgeGraphRepository.upsertRelation(source, srcType, row.sourceId,
                                tgtType, row.targetId, relType);
                        result.setWrittenRelations(result.getWrittenRelations() + 1);
                    }
                    result.setRelationTotal(result.getRelationTotal() + 1);
                } catch (Exception e) {
                    execFail++;
                    result.getFailDetails().add("关系 [" + row.sourceId + " → " + row.targetId + "] 写入失败：" + e.getMessage());
                }
            }

            result.setFailCount(parsed.errors.size() + execFail);
        } catch (Exception e) {
            result.setFailCount(result.getFailCount() + 1);
            result.getFailDetails().add("文件解析失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 返回某课程（source）下的全量图谱数据（供教师端可视化）。
     */
    public KnowledgeGraphVO getFullGraph(String source) {
        return knowledgeGraphRepository.getFullGraphBySource(source);
    }

    /**
     * 导出某课程（source）下全部节点行（含类型标签与父节点映射），供「下载当前课程图谱」Excel 使用。
     */
    public List<Map<String, Object>> exportNodeRows(String source) {
        return knowledgeGraphRepository.exportNodeRowsBySource(source);
    }

    /**
     * 导出某课程（source）下全部关系行（含端点类型标签），供「下载当前课程图谱」Excel 使用。
     */
    public List<Map<String, Object>> exportRelationRows(String source) {
        return knowledgeGraphRepository.exportRelationRowsBySource(source);
    }

    /**
     * 根据节点表 parentId 自动创建层级关系：
     *  - SubTopic.parentId → KnowledgeArea，生成 HAS_SUBTOPIC (KnowledgeArea)->(SubTopic)
     *  - KnowledgePoint.parentId → 优先 SubTopic（PART_OF），其次 KnowledgePoint（PARENT_KP）
     *  - 表内填写了父节点类型时，按指定类型与 child 类型推导关系
     */
    private void autoBuildParentRelation(String source, String childType,
                                         GraphImportParser.NodeRow row) {
        String parentType = GraphImportParser.normalizeNodeType(row.parentType);
        if (parentType != null) {
            ensureNode(source, parentType, row.parentId);
            String relType = relationTypeFor(parentType, childType);
            if (relType != null) {
                knowledgeGraphRepository.upsertRelation(source, parentType, row.parentId, childType, row.id, relType);
            }
            return;
        }
        if ("SubTopic".equals(childType)) {
            ensureNode(source, "KnowledgeArea", row.parentId);
            knowledgeGraphRepository.upsertRelation(source, "KnowledgeArea", row.parentId,
                    "SubTopic", row.id, "HAS_SUBTOPIC");
        } else if ("KnowledgePoint".equals(childType)) {
            if (knowledgeGraphRepository.existsNode(source, "SubTopic", row.parentId)) {
                knowledgeGraphRepository.upsertRelation(source, "SubTopic", row.parentId,
                        "KnowledgePoint", row.id, "PART_OF");
            } else if (knowledgeGraphRepository.existsNode(source, "KnowledgePoint", row.parentId)) {
                knowledgeGraphRepository.upsertRelation(source, "KnowledgePoint", row.parentId,
                        "KnowledgePoint", row.id, "PARENT_KP");
            } else {
                // 父节点均不存在：自动创建 SubTopic 父节点并建立 PART_OF，保证知识点的归属
                ensureNode(source, "SubTopic", row.parentId);
                knowledgeGraphRepository.upsertRelation(source, "SubTopic", row.parentId,
                        "KnowledgePoint", row.id, "PART_OF");
            }
        }
        // KnowledgeArea 一般无父节点，忽略
    }

    private void ensureNode(String source, String type, String id) {
        if (!knowledgeGraphRepository.existsNode(source, type, id)) {
            knowledgeGraphRepository.upsertNode(source, type, id, id, "", null, 0, null);
        }
    }

    private String relationTypeFor(String parentType, String childType) {
        if ("SubTopic".equals(parentType) && "KnowledgePoint".equals(childType)) {
            return "PART_OF";
        }
        if ("KnowledgeArea".equals(parentType) && "SubTopic".equals(childType)) {
            return "HAS_SUBTOPIC";
        }
        if ("KnowledgePoint".equals(parentType) && "KnowledgePoint".equals(childType)) {
            return "PARENT_KP";
        }
        return null;
    }
}
