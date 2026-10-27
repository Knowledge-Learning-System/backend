package cn.edu.bcu.learning.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 教师端图谱表格导入结果统计 VO
 */
@Data
public class GraphImportResultVO {

    /** 节点表总行数 */
    private int nodeTotal;

    /** 新增节点数（操作列=新增/创建） */
    private int createdNodes;

    /** 修改节点数（操作列=修改） */
    private int updatedNodes;

    /** 删除节点数（操作列=删除） */
    private int deletedNodes;

    /** 关系表总行数 */
    private int relationTotal;

    /** 新增/修改关系数（MERGE 幂等写入） */
    private int writtenRelations;

    /** 删除关系数 */
    private int deletedRelations;

    /** 由节点表 parentId 自动生成的关系数 */
    private int autoRelations;

    /** 失败行数 */
    private int failCount;

    /** 失败明细（行号 + 原因） */
    private List<String> failDetails = new ArrayList<>();
}
