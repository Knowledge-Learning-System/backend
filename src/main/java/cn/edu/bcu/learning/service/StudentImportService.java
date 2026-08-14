package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.dto.ImportStudentResultVO;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.entity.UserCourse;
import cn.edu.bcu.learning.repository.mysql.UserCourseMapper;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final UserMapper userMapper;
    private final UserCourseMapper userCourseMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 学生默认登录密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    /**
     * 从 Excel 批量导入学生 — POST /users/import
     * Excel 约定：第一行为表头，从第二行开始为数据。
     * 第一列 = 学号(username)，第二列 = 姓名(nickname)。
     * 学号已存在的行自动跳过（幂等导入）。
     */
    @Transactional
    public ImportStudentResultVO importFromExcel(MultipartFile file, Long courseId) {
        ImportStudentResultVO result = new ImportStudentResultVO();
        List<String> failDetails = new ArrayList<>();
        int total = 0;
        int success = 0;
        int fail = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // i = 1 跳过表头
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String username = getCellString(row.getCell(0));
                if (username == null || username.isBlank()) {
                    continue;
                }
                String nickname = getCellString(row.getCell(1));
                total++;
                try {
                    importOneStudent(username.trim(), nickname, courseId);
                    success++;
                } catch (Exception e) {
                    fail++;
                    failDetails.add("第" + (i + 1) + "行（" + username.trim() + "）：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 Excel 失败：" + e.getMessage(), e);
        }

        result.setTotalCount(total);
        result.setSuccessCount(success);
        result.setFailCount(fail);
        result.setFailDetails(failDetails);
        return result;
    }

    /** 导入单个学生：BCrypt 加密默认密码插入 user，并建立选课关联。学号已存在则跳过。 */
    private void importOneStudent(String username, String nickname, Long courseId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count != null && count > 0) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole("student");
        user.setNickname(nickname);
        userMapper.insert(user);

        if (courseId != null) {
            UserCourse userCourse = new UserCourse();
            userCourse.setUserId(user.getId());
            userCourse.setCourseId(courseId.intValue());
            userCourse.setProgress(0);
            userCourseMapper.insert(userCourse);
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double value = cell.getNumericCellValue();
                // 学号常为纯数字，避免科学计数法
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    return String.valueOf((long) value);
                }
                return String.valueOf(value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
}
