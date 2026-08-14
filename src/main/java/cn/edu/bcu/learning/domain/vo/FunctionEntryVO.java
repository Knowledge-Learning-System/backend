package cn.edu.bcu.learning.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统功能入口（功能导航）
 * AI 判断用户询问某功能时，回复附带该入口，前端渲染为可点击跳转卡片。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunctionEntryVO {

    /** 功能名 */
    private String name;

    /** 前端路由 */
    private String path;

    /** 功能说明 */
    private String description;
}
