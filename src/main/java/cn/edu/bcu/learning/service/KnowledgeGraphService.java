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

    // ==================== 图谱 → 章节 / 知识点 自动生成 ====================

    /**
     * 图谱上传完成后，依据图谱内容自动生成课程章节及其下属知识点归属。
     * 规则（幂等，不覆盖教师已维护的层级）：
     * 1. 该 source 下无 KnowledgeArea 时自动创建 1 个作为章节根；
     * 2. 已有 SubTopic 但未挂到 KnowledgeArea 的，自动补 HAS_SUBTOPIC 使其在章节列表中可见；
     * 3. 尚未归属任何章节的知识点，按「分组(group) → 父知识点(PARENT_KP)根 → 全部」的顺序归章，
     *    生成 SubTopic 并建立 PART_OF 关系，新章节顺序接在已有章节之后。
     *
     * @return int[0] = 新增章节数，int[1] = 归属到章节的知识点数
     */
    public int[] generateChaptersFromGraph(String source, String courseName) {
        if (source == null || source.isBlank()) {
            return new int[]{0, 0};
        }
        Collection<Map<String, Object>> kpRows = knowledgeGraphRepository.findAllKpBasicBySource(source);
        Collection<Map<String, Object>> stRows = knowledgeGraphRepository.findSubTopicsBySourceOnly(source);
        if (kpRows.isEmpty() && stRows.isEmpty()) {
            return new int[]{0, 0};
        }

        // 1. 章节根 KnowledgeArea
        String areaId = resolveChapterArea(source, courseName);

        // 2. 已有 SubTopic：补齐挂载与顺序（只处理 source 属于本课程的 SubTopic）
        //    若本次图谱已带来真实章节（非系统自动占位章），先清掉历史占位章，避免章节重复
        boolean hasRealChapter = false;
        for (Map<String, Object> stRow : stRows) {
            if (stRow.get("id") != null && !String.valueOf(stRow.get("id")).startsWith("ST_AUTO_")) {
                hasRealChapter = true;
                break;
            }
        }
        if (hasRealChapter) {
            knowledgeGraphRepository.deleteAutoSubTopics(source);
            stRows = knowledgeGraphRepository.findSubTopicsBySourceOnly(source);
        }

        int maxOrder = 0;
        for (Map<String, Object> stRow : stRows) {
            if (stRow.get("id") == null) {
                continue;
            }
            String stId = String.valueOf(stRow.get("id"));
            int stOrder = stRow.get("order") == null ? 0 : ((Number) stRow.get("order")).intValue();
            if (stRow.get("areaId") == null) {
                knowledgeGraphRepository.upsertRelation(source, "KnowledgeArea", areaId,
                        "SubTopic", stId, "HAS_SUBTOPIC");
            }
            if (stOrder <= 0) {
                stOrder = maxOrder + 1;
                knowledgeGraphRepository.upsertNode(source, "SubTopic", stId,
                        stRow.get("name") == null ? stId : String.valueOf(stRow.get("name")),
                        "", 1, 0, stOrder);
            }
            maxOrder = Math.max(maxOrder, stOrder);
        }

        // 3. 尚未归属任何章节的知识点
        Set<String> assignedKpIds = new HashSet<>(knowledgeGraphRepository.findKpIdsWithSubTopic(source));
        List<Map<String, Object>> unassigned = new ArrayList<>();
        List<String> allKpIds = new ArrayList<>();
        for (Map<String, Object> kpRow : kpRows) {
            if (kpRow.get("id") == null) {
                continue;
            }
            String kpId = String.valueOf(kpRow.get("id"));
            allKpIds.add(kpId);
            if (!assignedKpIds.contains(kpId)) {
                unassigned.add(kpRow);
            }
        }
        if (unassigned.isEmpty()) {
            return new int[]{0, 0};
        }

        // 4. 分组
        Map<String, List<Map<String, Object>>> groups =
                groupUnassignedKnowledgePoints(source, kpRows, unassigned, allKpIds);

        // 5. 生成章节节点 + 章节根关系 + 知识点归属关系
        int chapterCount = 0;
        int attachedCount = 0;
        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            int seq = maxOrder + chapterCount + 1;
            String subTopicId = "ST_AUTO_" + seq;
            knowledgeGraphRepository.upsertNode(source, "SubTopic", subTopicId, entry.getKey(), "", 1, 0, seq);
            knowledgeGraphRepository.upsertRelation(source, "KnowledgeArea", areaId,
                    "SubTopic", subTopicId, "HAS_SUBTOPIC");
            for (Map<String, Object> kp : entry.getValue()) {
                knowledgeGraphRepository.upsertRelation(source, "SubTopic", subTopicId,
                        "KnowledgePoint", String.valueOf(kp.get("id")), "PART_OF");
                attachedCount++;
            }
            chapterCount++;
        }
        return new int[]{chapterCount, attachedCount};
    }

    /** 章节根 KnowledgeArea：已存在则复用第一个，否则新建 */
    private String resolveChapterArea(String source, String courseName) {
        Collection<Map<String, Object>> areas = knowledgeGraphRepository.findAreasBySource(source);
        for (Map<String, Object> area : areas) {
            if (area.get("id") != null && !String.valueOf(area.get("id")).isEmpty()) {
                return String.valueOf(area.get("id"));
            }
        }
        String areaName = (courseName == null || courseName.isBlank()) ? "课程知识体系" : courseName;
        String areaId = "KA_" + source;
        knowledgeGraphRepository.upsertNode(source, "KnowledgeArea", areaId, areaName, "", 0, 0, 0);
        return areaId;
    }

    /**
     * 未归属知识点分组：优先按「分组」列，其次按父知识点根节点，最后归入单章。
     */
    private Map<String, List<Map<String, Object>>> groupUnassignedKnowledgePoints(
            String source, Collection<Map<String, Object>> kpRows,
            List<Map<String, Object>> unassigned, List<String> allKpIds) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        int chapterIndex = 1;

        // 4.1 按分组列（group > 0）分组
        boolean hasGroup = false;
        for (Map<String, Object> row : unassigned) {
            if (row.get("group") != null && ((Number) row.get("group")).intValue() > 0) {
                hasGroup = true;
                break;
            }
        }
        if (hasGroup) {
            TreeMap<Integer, List<Map<String, Object>>> byGroup = new TreeMap<>();
            for (Map<String, Object> row : unassigned) {
                int g = row.get("group") == null ? 0 : ((Number) row.get("group")).intValue();
                byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(row);
            }
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : byGroup.entrySet()) {
                List<Map<String, Object>> list = entry.getValue();
                String firstName = list.isEmpty() ? "" : String.valueOf(list.get(0).get("name"));
                String chapterName = "第" + chapterIndex + "章 " + firstName;
                groups.put(chapterName, list);
                chapterIndex++;
            }
            return groups;
        }

        // 4.2 按父知识点根节点分组
        Map<String, String> parentOf = new HashMap<>();
        for (Map<String, Object> row : knowledgeGraphRepository.findParentKpRelations(source, allKpIds)) {
            if (row.get("childId") != null && row.get("parentId") != null) {
                parentOf.putIfAbsent(String.valueOf(row.get("childId")), String.valueOf(row.get("parentId")));
            }
        }
        if (!parentOf.isEmpty()) {
            Map<String, String> kpNameById = new HashMap<>();
            for (Map<String, Object> row : kpRows) {
                if (row.get("id") != null) {
                    kpNameById.put(String.valueOf(row.get("id")), String.valueOf(row.get("name")));
                }
            }
            Map<String, List<Map<String, Object>>> byRoot = new LinkedHashMap<>();
            List<Map<String, Object>> orphans = new ArrayList<>();
            for (Map<String, Object> row : unassigned) {
                String kpId = String.valueOf(row.get("id"));
                if (!parentOf.containsKey(kpId)) {
                    orphans.add(row);
                    continue;
                }
                String root = kpId;
                int guard = 0;
                while (parentOf.containsKey(root) && guard++ < 200) {
                    root = parentOf.get(root);
                }
                byRoot.computeIfAbsent(root, k -> new ArrayList<>()).add(row);
            }
            for (Map.Entry<String, List<Map<String, Object>>> entry : byRoot.entrySet()) {
                String rootName = kpNameById.getOrDefault(entry.getKey(), entry.getKey());
                groups.put("第" + chapterIndex + "章 " + rootName, entry.getValue());
                chapterIndex++;
            }
            if (!orphans.isEmpty()) {
                groups.put("第" + chapterIndex + "章 未分组知识点", orphans);
            }
            return groups;
        }

        // 4.3 兜底：全部归入单章
        groups.put("第1章 核心知识点", new ArrayList<>(unassigned));
        return groups;
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
