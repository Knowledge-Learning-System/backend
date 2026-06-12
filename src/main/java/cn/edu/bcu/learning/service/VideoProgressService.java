package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.SaveProgressRequest;
import cn.edu.bcu.learning.domain.entity.VideoProgress;
import cn.edu.bcu.learning.repository.mysql.VideoProgressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoProgressService {

    private final VideoProgressMapper videoProgressMapper;

    public VideoProgress getProgress(Integer userId, Integer videoId) {
        LambdaQueryWrapper<VideoProgress> wrapper = new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId)
                .eq(VideoProgress::getVideoId, videoId);
        return videoProgressMapper.selectOne(wrapper);
    }

    public void saveProgress(Integer userId, SaveProgressRequest request) {
        LambdaQueryWrapper<VideoProgress> wrapper = new LambdaQueryWrapper<VideoProgress>()
                .eq(VideoProgress::getUserId, userId)
                .eq(VideoProgress::getVideoId, request.getVideoId());
        VideoProgress existing = videoProgressMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setPosition(request.getPosition());
            if (request.getProgress() != null) {
                existing.setProgress(request.getProgress());
            }
            videoProgressMapper.updateById(existing);
        } else {
            VideoProgress progress = new VideoProgress();
            progress.setUserId(userId);
            progress.setVideoId(request.getVideoId());
            progress.setPosition(request.getPosition());
            progress.setProgress(request.getProgress() != null ? request.getProgress() : 0.0);
            videoProgressMapper.insert(progress);
        }
    }
}
