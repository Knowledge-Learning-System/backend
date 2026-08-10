package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.SaveProgressRequest;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.VideoProgress;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.VideoProgressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoProgressService {

    private final VideoProgressMapper videoProgressMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;

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

        // 视频观看触发 KnowledgeMastery（地板值 30）
        String kpId = request.getKnowledgePointId();
        if (kpId != null && !kpId.isEmpty()) {
            LambdaQueryWrapper<KnowledgeMastery> kmWrapper = new LambdaQueryWrapper<KnowledgeMastery>()
                    .eq(KnowledgeMastery::getUserId, userId)
                    .eq(KnowledgeMastery::getKnowledgePointId, kpId);
            if (request.getCourseId() != null) {
                kmWrapper.eq(KnowledgeMastery::getCourseId, request.getCourseId());
            }
            KnowledgeMastery km = knowledgeMasteryMapper.selectOne(kmWrapper);
            if (km == null) {
                km = new KnowledgeMastery();
                km.setUserId(userId);
                km.setKnowledgePointId(kpId);
                km.setCourseId(request.getCourseId());
                km.setMasteryLevel(30);
                km.setTotalAttempts(0);
                km.setCorrectAttempts(0);
                km.setLastAttemptTime(LocalDateTime.now());
                knowledgeMasteryMapper.insert(km);
            } else if (km.getMasteryLevel() != null && km.getMasteryLevel() < 30) {
                km.setMasteryLevel(30);
                km.setLastAttemptTime(LocalDateTime.now());
                knowledgeMasteryMapper.updateById(km);
            }
        }
    }
}
