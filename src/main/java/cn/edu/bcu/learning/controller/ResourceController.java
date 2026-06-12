package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.ResourceSearchResultVO;
import cn.edu.bcu.learning.service.ResourceService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    @Value("${resource.base-path}")
    private String basePath;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
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

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.isAbsolute() && file.exists()) {
            return file;
        }
        return new File(basePath, path);
    }
}
