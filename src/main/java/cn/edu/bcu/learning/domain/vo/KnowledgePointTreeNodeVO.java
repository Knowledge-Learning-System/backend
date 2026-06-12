package cn.edu.bcu.learning.domain.vo;

import cn.edu.bcu.learning.domain.entity.VideoResource;
import cn.edu.bcu.learning.domain.entity.CoursewareResource;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgePointTreeNodeVO {
    private String id;
    private String name;
    private String description;
    private int level;
    private List<KnowledgePointTreeNodeVO> children = new ArrayList<>();
    private List<VideoResource> videos = new ArrayList<>();
    private List<CoursewareResource> coursewares = new ArrayList<>();
    private List<AnswerDetailVO> questions = new ArrayList<>();
}
