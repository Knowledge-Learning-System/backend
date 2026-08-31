package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.annotation.RequireRole;
import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.ResourceSearchResultVO;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.service.ResourceService;
import cn.edu.bcu.learning.utils.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final CourseMapper courseMapper;

    @Value("${resource.base-path}")
    private String basePath;

    public ResourceController(ResourceService resourceService, KnowledgeMasteryMapper knowledgeMasteryMapper, CourseMapper courseMapper) {
        this.resourceService = resourceService;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.courseMapper = courseMapper;
    }

    @GetMapping("/videos")
    public Result<List<VideoResource>> getVideos(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String knowledgePointId) {
        return Result.success(resourceService.getVideos(courseId, knowledgePointId));
    }

    @GetMapping("/search")
    public Result<ResourceSearchResultVO> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(resourceService.searchResources(keyword, type, page, pageSize));
    }

    @GetMapping("/courseware")
    public Result<List<CoursewareResource>> getCourseware(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String knowledgePointId) {
        return Result.success(resourceService.getCourseware(courseId, knowledgePointId));
    }

    @GetMapping("/videos/stream")
    public void streamVideo(@RequestParam String path, HttpServletResponse response) throws IOException {
        File file = resolveFile(path);
        if (!file.exists()) {
            response.sendError(404, "视频文件不存在");
            return;
        }
        response.setContentType("video/mp4");
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setContentLengthLong(file.length());

        try (OutputStream os = response.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(os);
        }
    }

    @GetMapping("/courseware/download")
    public void downloadCourseware(@RequestParam String path, 
                                   @RequestParam(defaultValue = "inline") String mode,
                                   HttpServletResponse response) throws IOException {
        File file = resolveFile(path);
        if (!file.exists()) {
            response.sendError(404, "课件文件不存在");
            return;
        }
        // PDF 在线预览，其他格式强制下载
        boolean inline = "inline".equals(mode) && file.getName().toLowerCase().endsWith(".pdf");
        response.setContentType(inline ? "application/pdf" : "application/octet-stream");
        String disposition = inline ? "inline" : "attachment";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                disposition + "; filename=\"" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8) + "\"");
        response.setContentLengthLong(file.length());

        try (OutputStream os = response.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(os);
        }
    }

    @PostMapping("/courseware/access")
    public Result<?> trackCoursewareAccess(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        String knowledgePointId = (String) body.get("knowledgePointId");
        Integer courseId = body.get("courseId") != null ? ((Number) body.get("courseId")).intValue() : null;

        if (knowledgePointId == null || knowledgePointId.isEmpty()) {
            return Result.success();
        }

        LambdaQueryWrapper<KnowledgeMastery> kmWrapper = new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getUserId, userId)
                .eq(KnowledgeMastery::getKnowledgePointId, knowledgePointId);
        if (courseId != null) {
            kmWrapper.eq(KnowledgeMastery::getCourseId, courseId);
        }
        KnowledgeMastery km = knowledgeMasteryMapper.selectOne(kmWrapper);
        if (km == null) {
            km = new KnowledgeMastery();
            km.setUserId(userId);
            km.setKnowledgePointId(knowledgePointId);
            km.setCourseId(courseId);
            km.setMasteryLevel(20);
            km.setTotalAttempts(0);
            km.setCorrectAttempts(0);
            km.setLastAttemptTime(LocalDateTime.now());
            knowledgeMasteryMapper.insert(km);
        } else if (km.getMasteryLevel() != null && km.getMasteryLevel() < 20) {
            km.setMasteryLevel(20);
            km.setLastAttemptTime(LocalDateTime.now());
            knowledgeMasteryMapper.updateById(km);
        }
        return Result.success();
    }

    /** 上传学习资料（教师端）— POST /resources/upload，type=video 上传视频，type=courseware 上传课件 */
    @RequireRole("teacher")
    @PostMapping("/upload")
    public Result<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String knowledgePointId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "courseware") String type,
            HttpServletRequest request) {
        // 归属校验：传入 courseId 时，仅该课程的负责教师可上传资源
        if (courseId != null) {
            Integer userId = (Integer) request.getAttribute("userId");
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                return Result.fail("课程不存在");
            }
            if (course.getTeacherId() != null && !course.getTeacherId().equals(userId)) {
                return Result.fail("您无权为该课程上传资源");
            }
        }
        if ("video".equals(type)) {
            return Result.success(resourceService.uploadVideo(courseId, knowledgePointId, title, file, basePath));
        }
        return Result.success(resourceService.uploadCourseware(courseId, knowledgePointId, title, file, basePath));
    }

    /** 存量课件补 RAG 索引 — POST /resources/reindex（运维接口） */
    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        return Result.success(resourceService.reindexAllCourseware(basePath));
    }

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.isAbsolute() && file.exists()) {
            return file;
        }
        return new File(basePath, path);
    }
}
