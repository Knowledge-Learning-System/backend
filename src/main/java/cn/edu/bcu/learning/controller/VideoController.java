package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.vo.VideoDetailVO;
import cn.edu.bcu.learning.repository.mysql.VideoResourceMapper;
import cn.edu.bcu.learning.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoResourceMapper videoResourceMapper;

    @GetMapping("/{id}")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable Integer id) {
        VideoResource video = videoResourceMapper.selectById(id);
        if (video == null) {
            return Result.failed();
        }

        VideoDetailVO vo = new VideoDetailVO();
        vo.setId(video.getId());
        vo.setCourseId(video.getCourseId());
        vo.setKnowledgePointId(video.getKnowledgePointId());
        vo.setTitle(video.getTitle());
        vo.setFilePath(video.getFilePath());
        vo.setDuration(video.getDuration());

        if (video.getFilePath() != null) {
            vo.setStreamUrl("/api/resources/videos/stream?path="
                    + URLEncoder.encode(video.getFilePath(), StandardCharsets.UTF_8));
        }

        if (video.getCreateTime() != null) {
            vo.setCreateTime(video.getCreateTime().toString());
        }

        return Result.success(vo);
    }
}
