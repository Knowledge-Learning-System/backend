package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSearchResultVO {

    private List<ResourceItem> list;

    private long total;

    private int page;

    private int pageSize;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceItem {

        private Integer id;

        /** video / courseware */
        private String type;

        private String title;

        private Integer courseId;

        private String knowledgePointId;
    }
}
