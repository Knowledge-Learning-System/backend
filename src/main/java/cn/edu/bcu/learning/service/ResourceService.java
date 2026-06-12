package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.ResourceSearchResultVO;
import cn.edu.bcu.learning.repository.mysql.CoursewareResourceMapper;
import cn.edu.bcu.learning.repository.mysql.VideoResourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final VideoResourceMapper videoResourceMapper;
    private final CoursewareResourceMapper coursewareResourceMapper;

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
}
