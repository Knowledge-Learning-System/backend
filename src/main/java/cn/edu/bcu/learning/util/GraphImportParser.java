package cn.edu.bcu.learning.util;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 教师端图谱表格导入解析器。
 *
 * 支持两种来源：
 *  1. Excel（.xlsx / .xls）：可包含多个 sheet，按 sheet 名称或表头自动识别"节点表"与"关系表"；
 *  2. CSV（.csv）：每个文件是一张表，按表头自动识别；支持一个文件（仅节点）或两个文件（节点 + 关系）。
 *
 * 节点表列（中文/英文均支持）：
 *  操作(operation/op) | 类型(type/nodeType) | ID(id) | 名称(name) | 描述(description/desc)
 *  | 层级(level) | 分组(group) | 顺序(order) | 来源(source) | 父节点ID(parentId) | 父节点类型(parentType)
 *
 * 关系表列：
 *  操作(operation/op) | 源类型(sourceType) | 源ID(source) | 目标类型(targetType) | 目标ID(target) | 关系类型(type/relationType)
 */
public class GraphImportParser {

    /** 允许写入的节点标签（白名单，防止 Cypher 注入） */
    public static final List<String> ALLOWED_NODE_TYPES =
            Arrays.asList("KnowledgePoint", "SubTopic", "KnowledgeArea");

    /** 允许写入的关系类型（白名单，防止 Cypher 注入） */
    public static final List<String> ALLOWED_RELATION_TYPES =
            Arrays.asList("PART_OF", "BELONGS_TO", "HAS_SUBTOPIC", "PARENT_KP", "PREREQUISITE");

    /** 操作类型 */
    public static final String OP_CREATE = "create";
    public static final String OP_UPDATE = "update";
    public static final String OP_DELETE = "delete";

    public static class NodeRow {
        public String operation;
        public String type;
        public String id;
        public String name;
        public String description = "";
        public Integer level;
        public Integer group;
        public Integer order;
        public String source;
        public String parentId;
        public String parentType;
    }

    public static class RelationRow {
        public String operation;
        public String sourceType;
        public String sourceId;
        public String targetType;
        public String targetId;
        public String type;
    }

    public static class ParseResult {
        public List<NodeRow> nodes = new ArrayList<>();
        public List<RelationRow> relations = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    private static final DataFormatter FORMATTER = new DataFormatter();

    // ── 节点表列名别名 ──
    private static final String[] COL_OP = {"操作", "operation", "op", "动作"};
    private static final String[] COL_NODE_TYPE = {"类型", "节点类型", "nodetype", "type", "标签"};
    private static final String[] COL_ID = {"id", "节点id", "nodeid", "编号", "节点编号"};
    private static final String[] COL_NAME = {"名称", "name", "节点名称", "nodename"};
    private static final String[] COL_DESC = {"描述", "description", "desc", "节点描述"};
    private static final String[] COL_LEVEL = {"层级", "level", "深度"};
    private static final String[] COL_GROUP = {"分组", "group", "组"};
    private static final String[] COL_ORDER = {"顺序", "order", "排序", "序号"};
    private static final String[] COL_SOURCE = {"来源", "source", "课程", "课程id"};
    private static final String[] COL_PARENT_ID = {"父节点id", "parentid", "父节点", "上级id"};
    private static final String[] COL_PARENT_TYPE = {"父节点类型", "parenttype", "上级类型"};

    // ── 关系表列名别名 ──
    private static final String[] COL_REL_OP = {"操作", "operation", "op", "动作"};
    private static final String[] COL_SRC_TYPE = {"源类型", "sourcetype", "起点类型"};
    private static final String[] COL_SRC_ID = {"源id", "sourceid", "源节点", "source", "起点id", "起点"};
    private static final String[] COL_TGT_TYPE = {"目标类型", "targettype", "终点类型"};
    private static final String[] COL_TGT_ID = {"目标id", "targetid", "目标节点", "target", "终点id", "终点"};
    private static final String[] COL_REL_TYPE = {"关系类型", "type", "relationtype", "关系"};

    /**
     * 解析一批上传文件（Excel/CSV），合并为节点行 + 关系行。
     */
    public ParseResult parse(MultipartFile[] files) throws IOException {
        ParseResult result = new ParseResult();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                parseExcel(file, result);
            } else if (name.endsWith(".csv")) {
                parseCsv(file, result);
            } else {
                result.errors.add("[" + file.getOriginalFilename() + "] 不支持的文件格式，仅支持 .xlsx / .xls / .csv");
            }
        }
        return result;
    }

    // ── Excel 解析 ──
    private void parseExcel(MultipartFile file, ParseResult result) throws IOException {
        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName();
                boolean isRelationSheet = isRelationSheetName(sheetName);
                List<String[]> rows = readSheet(sheet);
                if (rows.isEmpty()) {
                    continue;
                }
                if (isRelationSheet) {
                    parseRelationRows(rows, result, "[" + file.getOriginalFilename() + " / " + sheetName + "]");
                } else {
                    parseNodeRows(rows, result, "[" + file.getOriginalFilename() + " / " + sheetName + "]");
                }
            }
        }
    }

    private boolean isRelationSheetName(String sheetName) {
        String s = sheetName.toLowerCase(Locale.ROOT);
        return s.contains("关系") || s.contains("edge") || s.contains("rel") || s.contains("边");
    }

    private List<String[]> readSheet(Sheet sheet) {
        List<String[]> rows = new ArrayList<>();
        for (Row row : sheet) {
            if (row == null) {
                continue;
            }
            int last = row.getLastCellNum();
            if (last <= 0) {
                continue;
            }
            String[] cells = new String[last];
            boolean allEmpty = true;
            for (int c = 0; c < last; c++) {
                cells[c] = FORMATTER.formatCellValue(row.getCell(c)).trim();
                if (!cells[c].isEmpty()) {
                    allEmpty = false;
                }
            }
            if (!allEmpty) {
                rows.add(cells);
            }
        }
        return rows;
    }

    // ── CSV 解析 ──
    private void parseCsv(MultipartFile file, ParseResult result) throws IOException {
        byte[] bytes = file.getBytes();
        List<String[]> rows = readCsv(bytes, StandardCharsets.UTF_8);
        // 若 UTF-8 解析出的表头无法识别为节点/关系表，尝试 GBK 重读
        if (!isRecognizableHeader(rows)) {
            rows = readCsv(bytes, Charset.forName("GBK"));
        }
        if (rows.isEmpty()) {
            return;
        }
        if (isRelationHeader(rows.get(0))) {
            parseRelationRows(rows, result, "[" + file.getOriginalFilename() + "]");
        } else {
            parseNodeRows(rows, result, "[" + file.getOriginalFilename() + "]");
        }
    }

    private boolean isRecognizableHeader(List<String[]> rows) {
        if (rows.isEmpty() || rows.get(0).length == 0) {
            return false;
        }
        String[] header = rows.get(0);
        return isRelationHeader(header) || isNodeHeader(header);
    }

    private List<String[]> readCsv(byte[] bytes, Charset charset) {
        String text = new String(bytes, charset);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<String[]> rows = new ArrayList<>();
        String[] lines = text.split("\r\n|\r|\n");
        for (String line : lines) {
            List<String> cells = splitCsvLine(line);
            boolean allEmpty = true;
            for (String c : cells) {
                if (!c.trim().isEmpty()) {
                    allEmpty = false;
                    break;
                }
            }
            if (!allEmpty) {
                rows.add(cells.toArray(new String[0]));
            }
        }
        return rows;
    }

    /** 手写 CSV 行解析，支持双引号包裹与转义 */
    private List<String> splitCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    cells.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
            }
        }
        cells.add(sb.toString().trim());
        return cells;
    }

    // ── 表头识别 ──
    private boolean isNodeHeader(String[] header) {
        return findColumn(header, COL_ID) >= 0 && findColumn(header, COL_NAME) >= 0;
    }

    private boolean isRelationHeader(String[] header) {
        return findColumn(header, COL_SRC_ID) >= 0
                && findColumn(header, COL_TGT_ID) >= 0
                && findColumn(header, COL_REL_TYPE) >= 0;
    }

    private int findColumn(String[] header, String[] aliases) {
        for (int i = 0; i < header.length; i++) {
            String h = normalize(header[i]);
            if (h.isEmpty()) {
                continue;
            }
            for (String alias : aliases) {
                if (h.equals(alias) || h.contains(alias)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\uFEFF", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    // ── 节点行解析 ──
    private void parseNodeRows(List<String[]> rows, ParseResult result, String scope) {
        if (rows.isEmpty()) {
            return;
        }
        String[] header = rows.get(0);
        int colOp = findColumn(header, COL_OP);
        int colType = findColumn(header, COL_NODE_TYPE);
        int colId = findColumn(header, COL_ID);
        int colName = findColumn(header, COL_NAME);
        int colDesc = findColumn(header, COL_DESC);
        int colLevel = findColumn(header, COL_LEVEL);
        int colGroup = findColumn(header, COL_GROUP);
        int colOrder = findColumn(header, COL_ORDER);
        int colSource = findColumn(header, COL_SOURCE);
        int colParentId = findColumn(header, COL_PARENT_ID);
        int colParentType = findColumn(header, COL_PARENT_TYPE);

        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            if (row.length == 0 || allEmpty(row)) {
                continue;
            }
            NodeRow nr = new NodeRow();
            nr.operation = normOp(get(row, colOp));
            nr.type = get(row, colType);
            nr.id = get(row, colId);
            nr.name = get(row, colName);
            nr.description = get(row, colDesc);
            nr.level = parseInteger(get(row, colLevel));
            nr.group = parseInteger(get(row, colGroup));
            nr.order = parseInteger(get(row, colOrder));
            nr.source = get(row, colSource);
            nr.parentId = get(row, colParentId);
            nr.parentType = get(row, colParentType);

            if (nr.id == null || nr.id.isEmpty()) {
                result.errors.add(scope + " 节点表第 " + (r + 1) + " 行缺少 ID，已跳过");
                continue;
            }
            if (nr.name == null || nr.name.isEmpty()) {
                result.errors.add(scope + " 节点表第 " + (r + 1) + " 行缺少名称，已跳过");
                continue;
            }
            if (nr.type == null || nr.type.isEmpty()) {
                result.errors.add(scope + " 节点表第 " + (r + 1) + " 行缺少类型，已跳过");
                continue;
            }
            result.nodes.add(nr);
        }
    }

    // ── 关系行解析 ──
    private void parseRelationRows(List<String[]> rows, ParseResult result, String scope) {
        if (rows.isEmpty()) {
            return;
        }
        String[] header = rows.get(0);
        int colOp = findColumn(header, COL_REL_OP);
        int colSrcType = findColumn(header, COL_SRC_TYPE);
        int colSrcId = findColumn(header, COL_SRC_ID);
        int colTgtType = findColumn(header, COL_TGT_TYPE);
        int colTgtId = findColumn(header, COL_TGT_ID);
        int colType = findColumn(header, COL_REL_TYPE);

        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            if (row.length == 0 || allEmpty(row)) {
                continue;
            }
            RelationRow rr = new RelationRow();
            rr.operation = normOp(get(row, colOp));
            rr.sourceType = get(row, colSrcType);
            rr.sourceId = get(row, colSrcId);
            rr.targetType = get(row, colTgtType);
            rr.targetId = get(row, colTgtId);
            rr.type = get(row, colType);

            if (rr.sourceId == null || rr.sourceId.isEmpty()
                    || rr.targetId == null || rr.targetId.isEmpty()
                    || rr.type == null || rr.type.isEmpty()) {
                result.errors.add(scope + " 关系表第 " + (r + 1) + " 行缺少源ID/目标ID/关系类型，已跳过");
                continue;
            }
            result.relations.add(rr);
        }
    }

    // ── 工具方法 ──
    private String get(String[] row, int col) {
        if (col < 0 || col >= row.length) {
            return null;
        }
        return row[col];
    }

    private boolean allEmpty(String[] row) {
        for (String s : row) {
            if (s != null && !s.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normOp(String op) {
        if (op == null || op.trim().isEmpty()) {
            return OP_CREATE;
        }
        String s = op.trim().toLowerCase(Locale.ROOT);
        if (s.contains("删") || s.equals("delete") || s.equals("del") || s.equals("remove")) {
            return OP_DELETE;
        }
        if (s.contains("改") || s.contains("修") || s.contains("更新") || s.equals("update")) {
            return OP_UPDATE;
        }
        return OP_CREATE;
    }

    private Integer parseInteger(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 将节点类型规整为标准标签名 */
    public static String normalizeNodeType(String type) {
        if (type == null) {
            return null;
        }
        String s = type.trim();
        for (String allowed : ALLOWED_NODE_TYPES) {
            if (allowed.equalsIgnoreCase(s)) {
                return allowed;
            }
        }
        // 兼容"知识点 / 章节 / 知识领域"中文描述
        if (s.contains("知识") && s.contains("点")) {
            return "KnowledgePoint";
        }
        if (s.contains("章") || s.contains("节") || s.contains("主题")) {
            return "SubTopic";
        }
        if (s.contains("领域") || s.contains("区域")) {
            return "KnowledgeArea";
        }
        return null;
    }

    /** 将关系类型规整为标准大写类型 */
    public static String normalizeRelationType(String type) {
        if (type == null) {
            return null;
        }
        String s = type.trim().toUpperCase(Locale.ROOT);
        for (String allowed : ALLOWED_RELATION_TYPES) {
            if (allowed.equals(s)) {
                return allowed;
            }
        }
        if (s.contains("PART") || s.contains("属于") || s.contains("章节")) {
            return "PART_OF";
        }
        if (s.contains("HAS_SUBTOPIC") || s.contains("子主题")) {
            return "HAS_SUBTOPIC";
        }
        if (s.contains("BELONGS")) {
            return "BELONGS_TO";
        }
        if (s.contains("PARENT") || s.contains("父子") || s.contains("子节点")) {
            return "PARENT_KP";
        }
        if (s.contains("PREREQ") || s.contains("前置")) {
            return "PREREQUISITE";
        }
        return null;
    }

    /**
     * 关系类型默认端点标签推断表：
     * 关系表未填写源/目标类型时使用。
     */
    public static String[] defaultEndpointsFor(String relType) {
        switch (relType) {
            case "PART_OF":
                return new String[]{"SubTopic", "KnowledgePoint"};
            case "HAS_SUBTOPIC":
                return new String[]{"KnowledgeArea", "SubTopic"};
            case "BELONGS_TO":
                return new String[]{"KnowledgePoint", "SubTopic"};
            case "PARENT_KP":
            case "PREREQUISITE":
                return new String[]{"KnowledgePoint", "KnowledgePoint"};
            default:
                return null;
        }
    }
}
