package cn.edu.bcu.learning.controller;

import cn.edu.bcu.learning.domain.dto.CreateDiscussionRequest;
import cn.edu.bcu.learning.domain.dto.CreateReplyRequest;
import cn.edu.bcu.learning.domain.vo.DiscussionReplyVO;
import cn.edu.bcu.learning.domain.vo.DiscussionVO;
import cn.edu.bcu.learning.service.DiscussionService;
import cn.edu.bcu.learning.utils.Result;
import cn.edu.bcu.learning.utils.ThreadLocalUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @PostMapping
    public Result<DiscussionVO> create(@RequestBody CreateDiscussionRequest request) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(discussionService.createDiscussion(userId, request));
    }

    @GetMapping
    public Result<IPage<DiscussionVO>> list(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer videoId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(discussionService.listDiscussions(courseId, videoId, page, size));
    }

    @GetMapping("/{id}")
    public Result<DiscussionVO> detail(@PathVariable Integer id) {
        DiscussionVO vo = discussionService.getDiscussion(id);
        return vo != null ? Result.success(vo) : Result.fail("讨论不存在");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        Integer userId = ThreadLocalUtil.getUserId();
        boolean ok = discussionService.deleteDiscussion(userId, id);
        return ok ? Result.success() : Result.fail("无权删除或讨论不存在");
    }

    @PostMapping("/{id}/reply")
    public Result<DiscussionReplyVO> reply(@PathVariable Integer id,
                                            @RequestBody CreateReplyRequest request) {
        Integer userId = ThreadLocalUtil.getUserId();
        return Result.success(discussionService.addReply(userId, id, request));
    }

    @DeleteMapping("/reply/{replyId}")
    public Result<?> deleteReply(@PathVariable Integer replyId) {
        Integer userId = ThreadLocalUtil.getUserId();
        boolean ok = discussionService.deleteReply(userId, replyId);
        return ok ? Result.success() : Result.fail("无权删除或回复不存在");
    }
}
