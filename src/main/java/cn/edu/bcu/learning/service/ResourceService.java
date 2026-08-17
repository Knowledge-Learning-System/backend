package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.ResourceSearchResultVO;
import cn.edu.bcu.learning.repository.mysql.CoursewareResourceMapper;
import cn.edu.bcu.learning.repository.mysql.ResourceChunkMapper;
import cn.edu.bcu.learning.repository.mysql.VideoResourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final VideoResourceMapper videoResourceMapper;
    private final CoursewareResourceMapper coursewareResourceMapper;
    private final ResourceChunkMapper resourceChunkMapper;
    private final RagService ragService;

    public List<VideoResource> getVideos(Integer courseId, String knowledgePointId) {
        LambdaQueryWrapper<VideoResource> wrapper = new LambdaQueryWrapper<VideoResource>()
                .eq(courseId != null, VideoResource::getCourseId, courseId)
                .eq(knowledgePointId != null && !knowledgePointId.isEmpty(),
                        VideoResource::getKnowledgePointId, knowledgePointId);
        return videoResourceMapper.selectList(wrapper);
    }

    public List<CoursewareResource> getCourseware(Integer courseId, String knowledgePointId) {
        LambdaQueryWrapper<CoursewareResource> wrapper = new LambdaQueryWrapper<CoursewareResource>()
                .eq(courseId != null, CoursewareResource::getCourseId, courseId)
                .eq(knowledgePointId != null && !knowledgePointId.isEmpty(),
                        CoursewareResource::getKnowledgePointId, knowledgePointId);
        return coursewareResourceMapper.selectList(wrapper);
    }

    public List<VideoResource> getVideosByKpIds(Integer courseId, List<String> kpIds) {
        if (CollectionUtils.isEmpty(kpIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<VideoResource> wrapper = new LambdaQueryWrapper<VideoResource>()
                .eq(courseId != null, VideoResource::getCourseId, courseId)
                .in(VideoResource::getKnowledgePointId, kpIds);
        return videoResourceMapper.selectList(wrapper);
    }

    public List<CoursewareResource> getCoursewareByKpIds(Integer courseId, List<String> kpIds) {
        if (CollectionUtils.isEmpty(kpIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<CoursewareResource> wrapper = new LambdaQueryWrapper<CoursewareResource>()
                .eq(courseId != null, CoursewareResource::getCourseId, courseId)
                .in(CoursewareResource::getKnowledgePointId, kpIds);
        return coursewareResourceMapper.selectList(wrapper);
    }

    /**
     * 全文搜索资源（使用 SQL LIKE）
     */
    public ResourceSearchResultVO searchResources(String keyword, String type, int page, int pageSize) {
        List<ResourceSearchResultVO.ResourceItem> allItems = new ArrayList<>();

        boolean includeVideo = "all".equals(type) || "video".equals(type);
        boolean includeCourseware = "all".equals(type) || "courseware".equals(type);

        if (includeVideo && StringUtils.hasText(keyword)) {
            LambdaQueryWrapper<VideoResource> videoWrapper = new LambdaQueryWrapper<VideoResource>()
                    .like(VideoResource::getTitle, keyword)
                    .orderByDesc(VideoResource::getId);
            List<VideoResource> videos = videoResourceMapper.selectList(videoWrapper);
            for (VideoResource v : videos) {
                ResourceSearchResultVO.ResourceItem item = new ResourceSearchResultVO.ResourceItem();
                item.setId(v.getId());
                item.setType("video");
                item.setTitle(v.getTitle());
                item.setCourseId(v.getCourseId());
                item.setKnowledgePointId(v.getKnowledgePointId());
                allItems.add(item);
            }
        }

        if (includeCourseware && StringUtils.hasText(keyword)) {
            LambdaQueryWrapper<CoursewareResource> cwWrapper = new LambdaQueryWrapper<CoursewareResource>()
                    .like(CoursewareResource::getTitle, keyword)
                    .orderByDesc(CoursewareResource::getId);
            List<CoursewareResource> coursewares = coursewareResourceMapper.selectList(cwWrapper);
            for (CoursewareResource cw : coursewares) {
                ResourceSearchResultVO.ResourceItem item = new ResourceSearchResultVO.ResourceItem();
                item.setId(cw.getId());
                item.setType("courseware");
                item.setTitle(cw.getTitle());
                item.setCourseId(cw.getCourseId());
                item.setKnowledgePointId(cw.getKnowledgePointId());
                allItems.add(item);
            }
        }

        // 手动分页
        long total = allItems.size();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= total) {
            return new ResourceSearchResultVO(Collections.emptyList(), total, page, pageSize);
        }
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<ResourceSearchResultVO.ResourceItem> pageList = allItems.subList(fromIndex, toIndex);

        return new ResourceSearchResultVO(pageList, total, page, pageSize);
    }

    /** 上传学习资料（教师端）— POST /resources/upload */
    public CoursewareResource uploadCourseware(Integer courseId, String knowledgePointId,
                                               String title, MultipartFile file, String basePath) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }

        String dir = basePath + File.separator + "courseware";
        File directory = new File(dir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("创建存储目录失败：" + dir);
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        File target = new File(directory, storedName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败：" + e.getMessage(), e);
        }

        CoursewareResource resource = new CoursewareResource();
        resource.setCourseId(courseId);
        resource.setKnowledgePointId(knowledgePointId);
        resource.setTitle(title != null && !title.isBlank() ? title : originalName);
        resource.setFilePath("courseware/" + storedName);
        resource.setFileType(ext);
        coursewareResourceMapper.insert(resource);
        // 解析正文并向量化写入 resource_chunk，供 AI 检索（失败不影响上传）
        ragService.indexResource(resource, target);
        return resource;
    }

    /** 上传视频（教师端）— POST /resources/upload type=video */
    public VideoResource uploadVideo(Integer courseId, String knowledgePointId,
                                     String title, MultipartFile file, String basePath) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }

        String dir = basePath + File.separator + "videos";
        File directory = new File(dir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("创建存储目录失败：" + dir);
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        File target = new File(directory, storedName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败：" + e.getMessage(), e);
        }

        VideoResource resource = new VideoResource();
        resource.setCourseId(courseId);
        resource.setKnowledgePointId(knowledgePointId);
        resource.setTitle(title != null && !title.isBlank() ? title : originalName);
        resource.setFilePath("videos/" + storedName);
        videoResourceMapper.insert(resource);
        return resource;
    }

    /** 存量课件补 RAG 索引：清空 resource_chunk 后全量重建。 */
    public Map<String, Object> reindexAllCourseware(String basePath) {
        resourceChunkMapper.delete(new LambdaQueryWrapper<>());
        List<CoursewareResource> all = coursewareResourceMapper.selectList(null);
        int total = all.size();
        int success = 0;
        int missing = 0;
        for (CoursewareResource r : all) {
            File f = new File(basePath, r.getFilePath());
            if (f.exists()) {
                ragService.indexResource(r, f);
                success++;
            } else {
                missing++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fileMissing", missing);
        result.put("chunks", resourceChunkMapper.selectCount(null));
        return result;
    }
}
