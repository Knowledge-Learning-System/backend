package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.SaveProgressRequest;
import cn.edu.bcu.learning.domain.entity.VideoProgress;
import cn.edu.bcu.learning.service.VideoProgressService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoProgressController {

    private final VideoProgressService videoProgressService;

    @GetMapping("/progress")
    public Result<VideoProgress> getProgress(
            @RequestParam Integer videoId,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        VideoProgress progress = videoProgressService.getProgress(userId, videoId);
        return Result.success(progress);
    }

    @PostMapping("/progress")
    public Result<?> saveProgress(
            @RequestBody SaveProgressRequest saveRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        videoProgressService.saveProgress(userId, saveRequest);
        return Result.success();
    }
}
