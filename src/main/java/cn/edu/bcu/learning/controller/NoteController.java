package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.AddNoteRequest;
import cn.edu.bcu.learning.domain.dto.EditNoteRequest;
import cn.edu.bcu.learning.domain.vo.NoteVO;
import cn.edu.bcu.learning.service.VideoNoteService;
import cn.edu.bcu.learning.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final VideoNoteService videoNoteService;

    @PostMapping
    public Result<NoteVO> addNote(
            @RequestBody AddNoteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        NoteVO note = videoNoteService.addNote(userId, request);
        return Result.success(note);
    }

    @GetMapping
    public Result<List<NoteVO>> listNotes(
            @RequestParam(required = false) Integer videoId,
            @RequestParam(required = false) String knowledgePointId,
            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        List<NoteVO> notes = videoNoteService.listNotes(userId, videoId, knowledgePointId);
        return Result.success(notes);
    }

    @PutMapping("/{id}")
    public Result<NoteVO> editNote(
            @PathVariable Integer id,
            @RequestBody EditNoteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        NoteVO note = videoNoteService.editNote(userId, id, request);
        if (note == null) {
            return Result.failed();
        }
        return Result.success(note);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteNote(
            @PathVariable Integer id,
            HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        boolean deleted = videoNoteService.deleteNote(userId, id);
        if (!deleted) {
            return Result.failed();
        }
        return Result.success();
    }
}
