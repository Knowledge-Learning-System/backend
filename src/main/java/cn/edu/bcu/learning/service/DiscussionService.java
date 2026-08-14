package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.CreateDiscussionRequest;
import cn.edu.bcu.learning.domain.dto.CreateReplyRequest;
import cn.edu.bcu.learning.domain.entity.Discussion;
import cn.edu.bcu.learning.domain.entity.DiscussionReply;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.vo.DiscussionReplyVO;
import cn.edu.bcu.learning.domain.vo.DiscussionVO;
import cn.edu.bcu.learning.repository.mysql.DiscussionMapper;
import cn.edu.bcu.learning.repository.mysql.DiscussionReplyMapper;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import cn.edu.bcu.learning.utils.SensitiveWordFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionMapper discussionMapper;
    private final DiscussionReplyMapper replyMapper;
    private final UserMapper userMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    public DiscussionVO createDiscussion(Integer userId, CreateDiscussionRequest request) {
        Discussion d = new Discussion();
        d.setCourseId(request.getCourseId());
        d.setVideoId(request.getVideoId());
        d.setKnowledgePointId(request.getKnowledgePointId());
        d.setUserId(userId);
        d.setTitle(sensitiveWordFilter.filter(request.getTitle()));
        d.setContent(sensitiveWordFilter.filter(request.getContent()));
        d.setReplyCount(0);
        discussionMapper.insert(d);
        return toVO(d, null);
    }

    public IPage<DiscussionVO> listDiscussions(Integer courseId, Integer videoId,
                                                Integer page, Integer size) {
        LambdaQueryWrapper<Discussion> wrapper = new LambdaQueryWrapper<Discussion>()
                .eq(courseId != null, Discussion::getCourseId, courseId)
                .eq(videoId != null, Discussion::getVideoId, videoId)
                .orderByDesc(Discussion::getCreateTime);
        Page<Discussion> p = new Page<>(page != null ? page : 1, size != null ? size : 20);
        IPage<Discussion> result = discussionMapper.selectPage(p, wrapper);

        return result.convert(d -> toVO(d, null));
    }

    public DiscussionVO getDiscussion(Integer id) {
        Discussion d = discussionMapper.selectById(id);
        if (d == null) return null;

        LambdaQueryWrapper<DiscussionReply> rw = new LambdaQueryWrapper<DiscussionReply>()
                .eq(DiscussionReply::getDiscussionId, id)
                .orderByAsc(DiscussionReply::getCreateTime);
        List<DiscussionReply> replies = replyMapper.selectList(rw);

        return toVO(d, replies);
    }

    public boolean deleteDiscussion(Integer userId, Integer id) {
        Discussion d = discussionMapper.selectById(id);
        if (d == null) return false;
        if (!d.getUserId().equals(userId) && !isTeacher(userId)) return false;

        LambdaQueryWrapper<DiscussionReply> rw = new LambdaQueryWrapper<DiscussionReply>()
                .eq(DiscussionReply::getDiscussionId, id);
        replyMapper.delete(rw);
        discussionMapper.deleteById(id);
        return true;
    }

    public DiscussionReplyVO addReply(Integer userId, Integer discussionId,
                                       CreateReplyRequest request) {
        DiscussionReply r = new DiscussionReply();
        r.setDiscussionId(discussionId);
        r.setUserId(userId);
        r.setReplyToId(request.getReplyToId());
        r.setContent(sensitiveWordFilter.filter(request.getContent()));
        replyMapper.insert(r);

        Long count = replyMapper.selectCount(
                new LambdaQueryWrapper<DiscussionReply>()
                        .eq(DiscussionReply::getDiscussionId, discussionId));
        Discussion d = discussionMapper.selectById(discussionId);
        if (d != null) {
            d.setReplyCount(count.intValue());
            discussionMapper.updateById(d);
        }

        return toReplyVO(r);
    }

    public boolean deleteReply(Integer userId, Integer replyId) {
        DiscussionReply r = replyMapper.selectById(replyId);
        if (r == null) return false;
        if (!r.getUserId().equals(userId) && !isTeacher(userId)) return false;
        replyMapper.deleteById(replyId);

        Long count = replyMapper.selectCount(
                new LambdaQueryWrapper<DiscussionReply>()
                        .eq(DiscussionReply::getDiscussionId, r.getDiscussionId()));
        Discussion d = discussionMapper.selectById(r.getDiscussionId());
        if (d != null) {
            d.setReplyCount(count.intValue());
            discussionMapper.updateById(d);
        }
        return true;
    }

    private DiscussionVO toVO(Discussion d, List<DiscussionReply> replies) {
        DiscussionVO vo = new DiscussionVO();
        vo.setId(d.getId());
        vo.setCourseId(d.getCourseId());
        vo.setVideoId(d.getVideoId());
        vo.setKnowledgePointId(d.getKnowledgePointId());
        vo.setUserId(d.getUserId());
        vo.setTitle(d.getTitle());
        vo.setContent(d.getContent());
        vo.setReplyCount(d.getReplyCount());
        vo.setCreateTime(d.getCreateTime());

        User u = userMapper.selectById(d.getUserId());
        vo.setUsername(u != null ? u.getUsername() : "未知用户");
        vo.setNickname(u != null ? u.getNickname() : null);
        vo.setRole(u != null ? u.getRole() : null);

        if (replies != null) {
            vo.setReplies(replies.stream().map(this::toReplyVO).collect(Collectors.toList()));
        } else {
            vo.setReplies(new ArrayList<>());
        }
        return vo;
    }

    private DiscussionReplyVO toReplyVO(DiscussionReply r) {
        DiscussionReplyVO vo = new DiscussionReplyVO();
        vo.setId(r.getId());
        vo.setDiscussionId(r.getDiscussionId());
        vo.setUserId(r.getUserId());
        vo.setReplyToId(r.getReplyToId());
        vo.setContent(r.getContent());
        vo.setCreateTime(r.getCreateTime());

        User u = userMapper.selectById(r.getUserId());
        vo.setUsername(u != null ? u.getUsername() : "未知用户");
        vo.setNickname(u != null ? u.getNickname() : null);
        vo.setRole(u != null ? u.getRole() : null);

        if (r.getReplyToId() != null) {
            User rt = userMapper.selectById(r.getReplyToId());
            vo.setReplyToUsername(rt != null ? rt.getUsername() : "未知用户");
        }
        return vo;
    }

    private boolean isTeacher(Integer userId) {
        User u = userMapper.selectById(userId);
        return u != null && "teacher".equals(u.getRole());
    }
}
