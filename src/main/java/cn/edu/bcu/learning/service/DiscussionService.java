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

    public DiscussionVO createDiscussion(Integer userId, CreateDiscussionRequest request) {
        Discussion d = new Discussion();
        d.setCourseId(request.getCourseId());
        d.setVideoId(request.getVideoId());
        d.setKnowledgePointId(request.getKnowledgePointId());
        d.setUserId(userId);
        d.setTitle(request.getTitle());
        d.setContent(request.getContent());
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
        if (d == null || !d.getUserId().equals(userId)) return false;

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
        r.setContent(request.getContent());
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
        if (r == null || !r.getUserId().equals(userId)) return false;
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

        if (replies != null) {
            Map<Integer, String> userMap = replies.stream()
                    .map(r -> r.getUserId())
                    .distinct()
                    .collect(Collectors.toMap(uid -> uid, uid -> {
                        User u2 = userMapper.selectById(uid);
                        return u2 != null ? u2.getUsername() : "未知用户";
                    }));
            vo.setReplies(replies.stream().map(r -> {
                DiscussionReplyVO rv = toReplyVO(r);
                rv.setUsername(userMap.getOrDefault(r.getUserId(), "未知用户"));
                if (r.getReplyToId() != null) {
                    rv.setReplyToUsername(userMap.getOrDefault(r.getReplyToId(), "未知用户"));
                }
                return rv;
            }).collect(Collectors.toList()));
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
        return vo;
    }
}
