package cn.edu.bcu.learning.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资料内容切块（RAG 向量索引）
 * 上传资料解析正文后按段切块，embedding 存为 JSON 浮点数组文本。
 */
@Data
@TableName("resource_chunk")
public class ResourceChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联课程 ID */
    private Integer courseId;

    /** 关联课件资源 ID（courseware_resource.id） */
    private Integer resourceId;

    /** 资源标题（冗余，便于召回时展示来源） */
    private String resourceTitle;

    /** 关联知识点 ID */
    private String knowledgePointId;

    /** 切块序号 */
    private Integer chunkIndex;

    /** 切块正文 */
    private String content;

    /** 向量（JSON 浮点数组文本） */
    private String embedding;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
