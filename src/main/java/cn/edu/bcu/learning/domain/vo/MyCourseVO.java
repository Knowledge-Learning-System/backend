package cn.edu.bcu.learning.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MyCourseVO extends CourseVO {
    private Integer progress;
}
