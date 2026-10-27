package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.vo.GraphImportResultVO;
import cn.edu.bcu.learning.domain.vo.KnowledgeGraphVO;
import cn.edu.bcu.learning.service.CourseService;
import cn.edu.bcu.learning.service.KnowledgeGraphService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final CourseService courseService;

    @GetMapping("/{courseId}")
    public Result<KnowledgeGraphVO> getGraph(@PathVariable Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        return Result.success(knowledgeGraphService.getGraphBySource(course.getSource()));
    }

    /**
     * 教师端：上传节点/关系表格（Excel/CSV），解析后写入 Neo4j。
     */
    @PostMapping("/import")
    public Result<GraphImportResultVO> importGraph(
            @RequestParam("courseId") Integer courseId,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.fail("请上传至少一个表格文件");
        }
        Course course = courseService.getCourseById(courseId);
        String source = (course.getSource() == null || course.getSource().isEmpty())
                ? "course_" + courseId : course.getSource();
        return Result.success(knowledgeGraphService.importGraph(source, files));
    }

    /**
     * 教师端：返回某课程全量图谱数据（三种节点 + 全部关系）。
     */
    @GetMapping("/full/{courseId}")
    public Result<KnowledgeGraphVO> getFullGraph(@PathVariable Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        return Result.success(knowledgeGraphService.getFullGraph(resolveSource(course)));
    }

    /**
     * 教师端：下载当前课程图谱数据文件（节点表 + 关系表两个 Sheet，表头与导入模板一致，内容为该课程 source 下全部真实数据）。
     */
    @GetMapping("/export/{courseId}")
    public void downloadCourseGraph(@PathVariable Integer courseId, HttpServletResponse response) throws Exception {
        Course course = courseService.getCourseById(courseId);
        String source = resolveSource(course);
        List<Map<String, Object>> nodes = knowledgeGraphService.exportNodeRows(source);
        List<Map<String, Object>> relations = knowledgeGraphService.exportRelationRows(source);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Sheet1 节点表
            Sheet nodeSheet = workbook.createSheet("节点表");
            String[] nodeHeaders = {"操作", "类型", "ID", "名称", "描述", "层级", "分组", "顺序", "来源", "父节点ID", "父节点类型"};
            Row headerRow = nodeSheet.createRow(0);
            for (int i = 0; i < nodeHeaders.length; i++) {
                headerRow.createCell(i).setCellValue(nodeHeaders[i]);
            }
            int r = 1;
            for (Map<String, Object> m : nodes) {
                Row row = nodeSheet.createRow(r++);
                Object[] values = {
                        "新增",
                        m.get("type"),
                        m.get("id"),
                        m.get("name"),
                        m.get("description") == null ? "" : m.get("description"),
                        m.get("level"),
                        m.get("group"),
                        m.get("order") == null ? "" : m.get("order"),
                        m.get("source"),
                        m.get("parentId"),
                        m.get("parentType"),
                };
                for (int c = 0; c < values.length; c++) {
                    Object v = values[c];
                    if (v == null) {
                        continue;
                    }
                    row.createCell(c).setCellValue(String.valueOf(v));
                }
            }
            int[] nodeWidths = {10, 16, 12, 30, 30, 8, 8, 8, 12, 14, 14};
            for (int i = 0; i < nodeWidths.length; i++) {
                nodeSheet.setColumnWidth(i, nodeWidths[i] * 256);
            }

            // Sheet2 关系表
            Sheet relSheet = workbook.createSheet("关系表");
            String[] relHeaders = {"操作", "源类型", "源ID", "目标类型", "目标ID", "关系类型"};
            Row relHeaderRow = relSheet.createRow(0);
            for (int i = 0; i < relHeaders.length; i++) {
                relHeaderRow.createCell(i).setCellValue(relHeaders[i]);
            }
            int r2 = 1;
            for (Map<String, Object> m : relations) {
                Row row = relSheet.createRow(r2++);
                Object[] values = {"新增", m.get("sourceType"), m.get("sourceId"), m.get("targetType"), m.get("targetId"), m.get("type")};
                for (int c = 0; c < values.length; c++) {
                    Object v = values[c];
                    if (v == null) {
                        continue;
                    }
                    row.createCell(c).setCellValue(String.valueOf(v));
                }
            }
            int[] relWidths = {10, 16, 12, 16, 12, 14};
            for (int i = 0; i < relWidths.length; i++) {
                relSheet.setColumnWidth(i, relWidths[i] * 256);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = (course.getName() == null || course.getName().isEmpty())
                    ? "知识图谱数据" : course.getName() + "图谱数据";
            String encoded = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    /**
     * 课程 source 兜底：与导入链路一致（source 为空时使用 course_{courseId}）。
     */
    private String resolveSource(Course course) {
        return (course.getSource() == null || course.getSource().isEmpty())
                ? "course_" + course.getId() : course.getSource();
    }

    /**
     * 教师端：下载图谱导入表格模板（节点表 + 关系表两个 Sheet）。
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Sheet1 节点表
            Sheet nodeSheet = workbook.createSheet("节点表");
            String[] nodeHeaders = {"操作", "类型", "ID", "名称", "描述", "层级", "分组", "顺序", "来源", "父节点ID", "父节点类型"};
            Row headerRow = nodeSheet.createRow(0);
            for (int i = 0; i < nodeHeaders.length; i++) {
                headerRow.createCell(i).setCellValue(nodeHeaders[i]);
            }
            Object[][] nodeSamples = {
                    {"新增", "KnowledgeArea", "KA001", "数据库原理", "", 0, 0, null, null, null, null},
                    {"新增", "SubTopic", "ST001", "第一章 绪论", "", 1, 1, 1, null, "KA001", "KnowledgeArea"},
                    {"新增", "KnowledgePoint", "KP001", "数据库系统概论", "描述信息", 0, 2, null, null, "ST001", "SubTopic"},
                    {"修改", "KnowledgePoint", "KP001", "数据库系统概论", "修改后的描述", 0, 2, null, null, null, null},
                    {"删除", "KnowledgePoint", "KP002", null, null, null, null, null, null, null, null},
            };
            for (int r = 0; r < nodeSamples.length; r++) {
                Row row = nodeSheet.createRow(r + 1);
                Object[] sample = nodeSamples[r];
                for (int c = 0; c < sample.length; c++) {
                    Object v = sample[c];
                    if (v == null) {
                        continue;
                    }
                    row.createCell(c).setCellValue(String.valueOf(v));
                }
            }
            int[] nodeWidths = {10, 16, 12, 30, 30, 8, 8, 8, 12, 14, 14};
            for (int i = 0; i < nodeWidths.length; i++) {
                nodeSheet.setColumnWidth(i, nodeWidths[i] * 256);
            }

            // Sheet2 关系表
            Sheet relSheet = workbook.createSheet("关系表");
            String[] relHeaders = {"操作", "源类型", "源ID", "目标类型", "目标ID", "关系类型"};
            Row relHeaderRow = relSheet.createRow(0);
            for (int i = 0; i < relHeaders.length; i++) {
                relHeaderRow.createCell(i).setCellValue(relHeaders[i]);
            }
            Object[][] relSamples = {
                    {"新增", "KnowledgeArea", "KA001", "SubTopic", "ST001", "HAS_SUBTOPIC"},
                    {"新增", "SubTopic", "ST001", "KnowledgePoint", "KP001", "PART_OF"},
                    {"新增", "KnowledgePoint", "KP001", "KnowledgePoint", "KP002", "PREREQUISITE"},
                    {"删除", "SubTopic", "ST001", "KnowledgePoint", "KP001", "PART_OF"},
            };
            for (int r = 0; r < relSamples.length; r++) {
                Row row = relSheet.createRow(r + 1);
                Object[] sample = relSamples[r];
                for (int c = 0; c < sample.length; c++) {
                    row.createCell(c).setCellValue(String.valueOf(sample[c]));
                }
            }
            int[] relWidths = {10, 16, 12, 16, 12, 14};
            for (int i = 0; i < relWidths.length; i++) {
                relSheet.setColumnWidth(i, relWidths[i] * 256);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encoded = URLEncoder.encode("知识图谱导入模板.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
