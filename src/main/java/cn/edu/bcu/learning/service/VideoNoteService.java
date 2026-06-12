package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.AddNoteRequest;
import cn.edu.bcu.learning.domain.dto.EditNoteRequest;
import cn.edu.bcu.learning.domain.entity.VideoNote;
import cn.edu.bcu.learning.domain.vo.NoteVO;
import cn.edu.bcu.learning.repository.mysql.VideoNoteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoNoteService {

    private final VideoNoteMapper videoNoteMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NoteVO addNote(Integer userId, AddNoteRequest request) {
        VideoNote note = new VideoNote();
        note.setUserId(userId);
        note.setVideoId(request.getVideoId());
        note.setKnowledgePointId(request.getKnowledgePointId());
        note.setTimestamp(request.getTimestamp());
        note.setContent(request.getContent());
        videoNoteMapper.insert(note);
        return toVO(note);
    }

    public List<NoteVO> listNotes(Integer userId, Integer videoId, String knowledgePointId) {
        LambdaQueryWrapper<VideoNote> wrapper = new LambdaQueryWrapper<VideoNote>()
                .eq(VideoNote::getUserId, userId)
                .eq(videoId != null, VideoNote::getVideoId, videoId)
                .eq(knowledgePointId != null && !knowledgePointId.isEmpty(),
                        VideoNote::getKnowledgePointId, knowledgePointId)
                .orderByAsc(VideoNote::getTimestamp);
        return videoNoteMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public NoteVO editNote(Integer userId, Integer noteId, EditNoteRequest request) {
        VideoNote note = getOwnedNote(userId, noteId);
        if (note == null) return null;
        note.setContent(request.getContent());
        videoNoteMapper.updateById(note);
        return toVO(note);
    }

    public boolean deleteNote(Integer userId, Integer noteId) {
        VideoNote note = getOwnedNote(userId, noteId);
        if (note == null) return false;
        videoNoteMapper.deleteById(noteId);
        return true;
    }

    private VideoNote getOwnedNote(Integer userId, Integer noteId) {
        LambdaQueryWrapper<VideoNote> wrapper = new LambdaQueryWrapper<VideoNote>()
                .eq(VideoNote::getId, noteId)
                .eq(VideoNote::getUserId, userId);
        return videoNoteMapper.selectOne(wrapper);
    }

    private NoteVO toVO(VideoNote note) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setVideoId(note.getVideoId());
        vo.setKnowledgePointId(note.getKnowledgePointId());
        vo.setTimestamp(note.getTimestamp());
        vo.setContent(note.getContent());
        if (note.getCreateTime() != null) {
            vo.setCreateTime(note.getCreateTime().format(FORMATTER));
        }
        if (note.getUpdateTime() != null) {
            vo.setUpdateTime(note.getUpdateTime().format(FORMATTER));
        }
        return vo;
    }
}
