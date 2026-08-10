package cn.edu.bcu.learning.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportStudentResultVO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private List<String> failDetails;
}
