package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.ImportStudentResultVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentImportService {

    /** 从 Excel 批量导入学生 — POST /users/import */
    public ImportStudentResultVO importFromExcel(MultipartFile file, Long courseId) {
        // TODO: Apache POI 解析 .xlsx
        // TODO: 逐行读取学号→username、姓名→nickname
        // TODO: BCrypt 加密默认密码，插入 user 表
        // TODO: 批量插入 user_course 关联记录
        return null;
    }
}
