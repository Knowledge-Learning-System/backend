package cn.edu.bcu.learning.repository.neo4j;

import cn.edu.bcu.learning.domain.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class KnowledgeGraphRepository {

    private final Neo4jClient neo4jClient;

    public KnowledgeGraphVO findGraphBySource(String source) {
        KnowledgeGraphVO graph = new KnowledgeGraphVO();

        Collection<Map<String, Object>> nodeRows = neo4jClient.query("""
                        MATCH (n:KnowledgePoint {source: $source})
                        RETURN n.id AS id, n.name AS name, n.source AS source,
                               coalesce(n.group, 0) AS `group`, coalesce(n.level, 0) AS level
                        """)
                .bind(source).to("source")
                .fetch()
                .all();

        for (Map<String, Object> row : nodeRows) {
            GraphNodeVO node = new GraphNodeVO();
            node.setId(String.valueOf(row.get("id")));
            node.setName(String.valueOf(row.get("name")));
            node.setCourseId(String.valueOf(row.get("source")));
            node.setGroup(((Number) row.get("group")).intValue());
            node.setLevel(((Number) row.get("level")).intValue());
            graph.getNodes().add(node);
        }

        // 包含 PARENT_KP 关系以支持树形展示
        Collection<Map<String, Object>> linkRows = neo4jClient.query("""
                        MATCH (a:KnowledgePoint {source: $source})-[r:PREREQUISITE|BELONGS_TO|PARENT_KP]->(b:KnowledgePoint {source: $source})
                        RETURN a.id AS source, b.id AS target, type(r) AS type
                        """)
                .bind(source).to("source")
                .fetch()
                .all();

        for (Map<String, Object> row : linkRows) {
            GraphLinkVO link = new GraphLinkVO();
            link.setSource(String.valueOf(row.get("source")));
            link.setTarget(String.valueOf(row.get("target")));
            link.setType(String.valueOf(row.get("type")));
            graph.getLinks().add(link);
        }

        return graph;
    }

    public Optional<KnowledgePointDetailVO> findDetailById(String id) {
        return neo4jClient.query("""
                        MATCH (n:KnowledgePoint {id: $id})
                        RETURN n.id AS id, n.name AS name, n.description AS description, n.source AS source
                        LIMIT 1
                        """)
                .bind(id).to("id")
                .fetch()
                .one()
                .map(row -> {
                    KnowledgePointDetailVO detail = new KnowledgePointDetailVO();
                    detail.setId(String.valueOf(row.get("id")));
                    detail.setName(String.valueOf(row.get("name")));
                    detail.setDescription(row.get("description") == null ? "" : String.valueOf(row.get("description")));
                    detail.setCourseId(String.valueOf(row.get("source")));
                    return detail;
                });
    }

    /**
     * 拓扑排序（Kahn 算法）获取课程学习路径。
     * 查询条件使用 source 字段匹配 Neo4j 中队友导入的数据。
     */
    public List<LearningPathItemVO> findLearningPathBySource(String source) {
        // 1. 查询所有节点
        Collection<Map<String, Object>> nodeRows = neo4jClient.query("""
                        MATCH (n:KnowledgePoint {source: $source})
                        RETURN n.id AS id, n.name AS name, n.description AS description,
                               n.source AS source, coalesce(n.group, 0) AS `group`
                        """)
                .bind(source).to("source")
                .fetch()
                .all();

        // 2. 查询所有 PREREQUISITE 关系
        Collection<Map<String, Object>> edgeRows = neo4jClient.query("""
                        MATCH (a:KnowledgePoint {source: $source})-[r:PREREQUISITE]->(b:KnowledgePoint {source: $source})
                        RETURN a.id AS source, b.id AS target
                        """)
                .bind(source).to("source")
                .fetch()
                .all();

        return topologicalSort(nodeRows, edgeRows);
    }

    /**
     * 查询指定知识点的所有直接前置知识点。
     */
    public List<KnowledgePointDetailVO> findPrerequisites(String kpId) {
        Collection<Map<String, Object>> rows = neo4jClient.query("""
                        MATCH (a:KnowledgePoint)-[:PREREQUISITE]->(b:KnowledgePoint {id: $id})
                        RETURN a.id AS id, a.name AS name, a.description AS description, a.source AS source
                        """)
                .bind(kpId).to("id")
                .fetch()
                .all();

        List<KnowledgePointDetailVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            KnowledgePointDetailVO detail = new KnowledgePointDetailVO();
            detail.setId(String.valueOf(row.get("id")));
            detail.setName(String.valueOf(row.get("name")));
            detail.setDescription(row.get("description") == null ? "" : String.valueOf(row.get("description")));
            detail.setCourseId(String.valueOf(row.get("source")));
            result.add(detail);
        }
        return result;
    }

    /**
     * 获取某 source 下所有知识点的 id 列表
     */
    public List<String> findAllKpIdsBySource(String source) {
        Collection<Map<String, Object>> rows = neo4jClient.query("""
                        MATCH (n:KnowledgePoint {source: $source})
                        RETURN n.id AS id
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ids.add(String.valueOf(row.get("id")));
        }
        return ids;
    }

    /**
     * 拓扑排序算法，提取为公共方法供学习路径和推荐使用。
     * 返回 LearningPathItemVO 列表，order 从 1 开始。
     */
    public List<LearningPathItemVO> topologicalSort(
            Collection<Map<String, Object>> nodeRows,
            Collection<Map<String, Object>> edgeRows) {

        Map<String, List<String>> adj = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();

        for (Map<String, Object> row : nodeRows) {
            String id = String.valueOf(row.get("id"));
            inDegree.putIfAbsent(id, 0);
            adj.putIfAbsent(id, new ArrayList<>());
            nodeMap.put(id, row);
        }

        for (Map<String, Object> row : edgeRows) {
            String src = String.valueOf(row.get("source"));
            String tgt = String.valueOf(row.get("target"));
            adj.computeIfAbsent(src, k -> new ArrayList<>()).add(tgt);
            inDegree.put(tgt, inDegree.getOrDefault(tgt, 0) + 1);
            inDegree.putIfAbsent(src, 0);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            topoOrder.add(cur);
            for (String nxt : adj.getOrDefault(cur, Collections.emptyList())) {
                inDegree.put(nxt, inDegree.get(nxt) - 1);
                if (inDegree.get(nxt) == 0) {
                    queue.offer(nxt);
                }
            }
        }

        for (String id : nodeMap.keySet()) {
            if (!topoOrder.contains(id)) {
                topoOrder.add(id);
            }
        }

        List<LearningPathItemVO> result = new ArrayList<>();
        for (int i = 0; i < topoOrder.size(); i++) {
            String id = topoOrder.get(i);
            Map<String, Object> row = nodeMap.get(id);

            LearningPathItemVO item = new LearningPathItemVO();
            item.setId(id);
            item.setName(String.valueOf(row.get("name")));
            item.setDescription(row.get("description") == null ? "" : String.valueOf(row.get("description")));
            item.setOrder(i + 1);
            item.setCourseId(String.valueOf(row.get("source")));
            item.setGroup(((Number) row.get("group")).intValue());

            List<String> prereqs = new ArrayList<>();
            Collection<Map<String, Object>> prereqRows = neo4jClient.query("""
                            MATCH (a:KnowledgePoint)-[:PREREQUISITE]->(b:KnowledgePoint {id: $id})
                            RETURN a.id AS id
                            """)
                    .bind(id).to("id")
                    .fetch()
                    .all();
            for (Map<String, Object> pr : prereqRows) {
                prereqs.add(String.valueOf(pr.get("id")));
            }
            item.setPrerequisites(prereqs);

            result.add(item);
        }

        return result;
    }

    /**
     * 查询某课程的所有 SubTopic（章/节）
     */
    public Collection<Map<String, Object>> findSubTopicsBySource(String source) {
        return neo4jClient.query("""
                        MATCH (ka:KnowledgeArea {source: $source})-[:HAS_SUBTOPIC]->(st:SubTopic)
                        RETURN st.id AS id, st.name AS name, st.description AS description,
                               coalesce(st.order, 0) AS `order`, st.level AS level,
                               st.childCount AS childCount, st.courseId AS courseId,
                               st.source AS source, st.parentId AS parentId
                        ORDER BY coalesce(st.order, 0)
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
    }

    /**
     * 查询某 SubTopic 下的所有 KnowledgePoint（PART_OF 关系）
     */
    public Collection<Map<String, Object>> findKnowledgePointsBySubTopic(String source, String subTopicId) {
        return neo4jClient.query("""
                        MATCH (st:SubTopic {id: $subTopicId})-[:PART_OF]->(kp:KnowledgePoint {source: $source})
                        RETURN kp.id AS id, kp.name AS name, kp.description AS description,
                               coalesce(kp.level, 0) AS level
                        """)
                .bind(source).to("source")
                .bind(subTopicId).to("subTopicId")
                .fetch()
                .all();
    }

    /**
     * 查询指定 KnowledgePoint 集合内的 PARENT_KP 父子关系
     */
    public Collection<Map<String, Object>> findParentKpRelations(String source, List<String> kpIds) {
        if (kpIds == null || kpIds.isEmpty()) {
            return Collections.emptyList();
        }
        return neo4jClient.query("""
                        MATCH (parent:KnowledgePoint {source: $source})-[r:PARENT_KP]->(child:KnowledgePoint {source: $source})
                        WHERE parent.id IN $kpIds AND child.id IN $kpIds
                        RETURN parent.id AS parentId, child.id AS childId
                        """)
                .bind(source).to("source")
                .bind(kpIds).to("kpIds")
                .fetch()
                .all();
    }

    /**
     * 递归查询某个 KnowledgePoint 的所有子孙节点 ID（含自身）
     * 通过 Neo4j 变长路径 PARENT_KP*0.. 实现递归
     */
    public List<String> findDescendantKpIds(String source, String kpId) {
        Collection<Map<String, Object>> rows = neo4jClient.query("""
                        MATCH (n:KnowledgePoint {id: $kpId, source: $source})
                        MATCH (n)-[:PARENT_KP*0..]->(descendant:KnowledgePoint {source: $source})
                        RETURN DISTINCT descendant.id AS id
                        """)
                .bind(source).to("source")
                .bind(kpId).to("kpId")
                .fetch()
                .all();
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ids.add(String.valueOf(row.get("id")));
        }
        return ids;
    }

    // ==================== 教师端图谱表格导入：写入与全量查询 ====================

    /**
     * 查询某课程（source）下的全量图谱：KnowledgePoint / SubTopic / KnowledgeArea 节点，
     * 以及 PART_OF / HAS_SUBTOPIC / BELONGS_TO / PARENT_KP / PREREQUISITE 全部关系。
     * group 字段约定：KnowledgeArea=0（根）、SubTopic=1、KnowledgePoint 取原 group（默认 2）。
     */
    public KnowledgeGraphVO getFullGraphBySource(String source) {
        KnowledgeGraphVO graph = new KnowledgeGraphVO();

        Collection<Map<String, Object>> kpRows = neo4jClient.query("""
                        MATCH (n:KnowledgePoint {source: $source})
                        RETURN n.id AS id, n.name AS name, n.source AS source,
                               coalesce(n.group, 2) AS `group`, coalesce(n.level, 0) AS level,
                               n.description AS description
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
        Collection<Map<String, Object>> stRows = neo4jClient.query("""
                        MATCH (n:SubTopic {source: $source})
                        RETURN n.id AS id, n.name AS name, n.source AS source,
                               coalesce(n.group, 1) AS `group`, coalesce(n.level, 0) AS level,
                               n.description AS description
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
        Collection<Map<String, Object>> kaRows = neo4jClient.query("""
                        MATCH (n:KnowledgeArea {source: $source})
                        RETURN n.id AS id, n.name AS name, n.source AS source,
                               coalesce(n.group, 0) AS `group`, coalesce(n.level, 0) AS level,
                               n.description AS description
                        """)
                .bind(source).to("source")
                .fetch()
                .all();

        for (Collection<Map<String, Object>> rows : Arrays.asList(kpRows, stRows, kaRows)) {
            for (Map<String, Object> row : rows) {
                GraphNodeVO node = new GraphNodeVO();
                node.setId(String.valueOf(row.get("id")));
                node.setName(String.valueOf(row.get("name")));
                node.setCourseId(String.valueOf(row.get("source")));
                node.setGroup(((Number) row.get("group")).intValue());
                node.setLevel(((Number) row.get("level")).intValue());
                node.setDescription(row.get("description") == null ? "" : String.valueOf(row.get("description")));
                graph.getNodes().add(node);
            }
        }

        Collection<Map<String, Object>> linkRows = neo4jClient.query("""
                        MATCH (a {source: $source})-[r:PART_OF|HAS_SUBTOPIC|BELONGS_TO|PARENT_KP|PREREQUISITE]->(b {source: $source})
                        RETURN a.id AS source, b.id AS target, type(r) AS type
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
        for (Map<String, Object> row : linkRows) {
            GraphLinkVO link = new GraphLinkVO();
            link.setSource(String.valueOf(row.get("source")));
            link.setTarget(String.valueOf(row.get("target")));
            link.setType(String.valueOf(row.get("type")));
            graph.getLinks().add(link);
        }
        return graph;
    }

    /**
     * 导出某课程（source）下全部节点行（含类型标签与父节点映射），供「下载当前课程图谱」Excel 使用。
     * 返回的 Map 键：id / type / name / description / level / group / order / source / parentId / parentType。
     */
    public List<Map<String, Object>> exportNodeRowsBySource(String source) {
        Collection<Map<String, Object>> nodeRows = neo4jClient.query("""
                        MATCH (n {source: $source})
                        RETURN n.id AS id, labels(n) AS labels, n.name AS name,
                               n.description AS description, coalesce(n.level, 0) AS level,
                               coalesce(n.group, 0) AS `group`, n.order AS `order`, n.source AS source
                        ORDER BY n.id
                        """)
                .bind(source).to("source")
                .fetch()
                .all();

        // 父节点映射：childId -> [parentId, parentType]（PART_OF / HAS_SUBTOPIC / PARENT_KP 取第一条）
        Collection<Map<String, Object>> parentRows = neo4jClient.query("""
                        MATCH (a {source: $source})-[r:PART_OF|HAS_SUBTOPIC|PARENT_KP]->(b {source: $source})
                        RETURN a.id AS parentId, labels(a) AS parentLabels, b.id AS childId
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
        Map<String, String[]> parentMap = new HashMap<>();
        for (Map<String, Object> row : parentRows) {
            parentMap.putIfAbsent(String.valueOf(row.get("childId")), new String[]{
                    String.valueOf(row.get("parentId")),
                    firstLabel(row.get("parentLabels"))});
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : nodeRows) {
            Map<String, Object> out = new HashMap<>();
            out.put("id", row.get("id"));
            out.put("type", firstLabel(row.get("labels")));
            out.put("name", row.get("name"));
            out.put("description", row.get("description") == null ? "" : row.get("description"));
            out.put("level", row.get("level"));
            out.put("group", row.get("group"));
            out.put("order", row.get("order"));
            out.put("source", row.get("source"));
            String[] parent = parentMap.get(String.valueOf(row.get("id")));
            out.put("parentId", parent == null ? "" : parent[0]);
            out.put("parentType", parent == null ? "" : parent[1]);
            result.add(out);
        }
        return result;
    }

    /**
     * 导出某课程（source）下全部关系行（含端点类型标签），供「下载当前课程图谱」Excel 使用。
     * 返回的 Map 键：sourceType / sourceId / targetType / targetId / type。
     */
    public List<Map<String, Object>> exportRelationRowsBySource(String source) {
        Collection<Map<String, Object>> rows = neo4jClient.query("""
                        MATCH (a {source: $source})-[r:PART_OF|HAS_SUBTOPIC|BELONGS_TO|PARENT_KP|PREREQUISITE]->(b {source: $source})
                        RETURN labels(a) AS sourceLabels, a.id AS sourceId,
                               labels(b) AS targetLabels, b.id AS targetId,
                               type(r) AS type
                        ORDER BY type(r), a.id, b.id
                        """)
                .bind(source).to("source")
                .fetch()
                .all();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new HashMap<>();
            out.put("sourceType", firstLabel(row.get("sourceLabels")));
            out.put("sourceId", row.get("sourceId"));
            out.put("targetType", firstLabel(row.get("targetLabels")));
            out.put("targetId", row.get("targetId"));
            out.put("type", row.get("type"));
            result.add(out);
        }
        return result;
    }

    private String firstLabel(Object labels) {
        if (labels instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        return "";
    }

    /**
     * MERGE 幂等写入节点（创建或修改）。
     *
     * @param type 白名单校验后的节点标签（KnowledgePoint / SubTopic / KnowledgeArea）
     */
    public void upsertNode(String source, String type, String id, String name, String description,
                           Integer level, Integer group, Integer order) {
        String cypher = """
                MERGE (n:`%s` {source: $source, id: $id})
                SET n.name = $name,
                    n.description = $description,
                    n.level = $level,
                    n.group = $group,
                    n.order = $order
                """.formatted(type);
        neo4jClient.query(cypher)
                .bind(source).to("source")
                .bind(id).to("id")
                .bind(name).to("name")
                .bind(description == null ? "" : description).to("description")
                .bind(level == null ? 0 : level).to("level")
                .bind(group == null ? 0 : group).to("group")
                .bind(order).to("order")
                .run();
    }

    /**
     * 删除节点（DETACH DELETE，级联删除其全部关系）。
     */
    public void deleteNode(String source, String type, String id) {
        String cypher = "MATCH (n:`" + type + "` {source: $source, id: $id}) DETACH DELETE n";
        neo4jClient.query(cypher)
                .bind(source).to("source")
                .bind(id).to("id")
                .run();
    }

    /**
     * MERGE 幂等写入关系（创建或修改）。
     */
    public void upsertRelation(String source, String sourceType, String sourceId,
                               String targetType, String targetId, String relType) {
        String cypher = """
                MATCH (a:`%s` {source: $source, id: $sourceId})
                MATCH (b:`%s` {source: $source, id: $targetId})
                MERGE (a)-[:`%s`]->(b)
                """.formatted(sourceType, targetType, relType);
        neo4jClient.query(cypher)
                .bind(source).to("source")
                .bind(sourceId).to("sourceId")
                .bind(targetId).to("targetId")
                .run();
    }

    /**
     * 删除指定两端与类型的关系。
     */
    public void deleteRelation(String source, String sourceType, String sourceId,
                               String targetType, String targetId, String relType) {
        String cypher = """
                MATCH (a:`%s` {source: $source, id: $sourceId})-[r:`%s`]->(b:`%s` {source: $source, id: $targetId})
                DELETE r
                """.formatted(sourceType, relType, targetType);
        neo4jClient.query(cypher)
                .bind(source).to("source")
                .bind(sourceId).to("sourceId")
                .bind(targetId).to("targetId")
                .run();
    }

    /**
     * 判断节点是否存在（用于节点表 parentId 自动建关系时的类型推断）。
     */
    public boolean existsNode(String source, String type, String id) {
        String cypher = "MATCH (n:`" + type + "` {source: $source, id: $id}) RETURN n.id AS id LIMIT 1";
        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(source).to("source")
                .bind(id).to("id")
                .fetch()
                .all();
        return !rows.isEmpty();
    }
}
