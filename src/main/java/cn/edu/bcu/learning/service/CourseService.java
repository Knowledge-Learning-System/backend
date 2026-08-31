package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.KnowledgeMastery;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.entity.UserCourse;
import cn.edu.bcu.learning.domain.vo.CourseVO;
import cn.edu.bcu.learning.domain.vo.MyCourseVO;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
import cn.edu.bcu.learning.repository.mysql.KnowledgeMasteryMapper;
import cn.edu.bcu.learning.repository.mysql.UserCourseMapper;
import cn.edu.bcu.learning.repository.mysql.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserMapper userMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;

    public List<CourseVO> listCourses() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, 1))
                .stream()
                .map(this::toCourseVO)
                .collect(Collectors.toList());
    }

    /** 教师"我的课程"：按 course.teacher_id 返回该教师负责的课程列表 */
    public List<CourseVO> listTeachingCourses(Integer teacherId) {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getTeacherId, teacherId)
                        .eq(Course::getStatus, 1))
                .stream()
                .map(this::toCourseVO)
                .collect(Collectors.toList());
    }

    public List<MyCourseVO> listMyCourses(Integer userId) {
        List<UserCourse> userCourses = userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId));

        if (userCourses.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> courseIds = userCourses.stream().map(UserCourse::getCourseId).toList();
        Map<Integer, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, course -> course));

        List<MyCourseVO> result = new ArrayList<>();
        for (UserCourse userCourse : userCourses) {
            Course course = courseMap.get(userCourse.getCourseId());
            if (course == null) {
                continue;
            }
            MyCourseVO vo = new MyCourseVO();
            BeanUtils.copyProperties(toCourseVO(course), vo);
            vo.setProgress(calculateCourseProgress(userId, userCourse.getCourseId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 动态计算用户在某课程的进度（0-100）。
     * 基于该用户在该课程所有知识点的 KnowledgeMastery.masteryLevel 平均值。
     */
    public int calculateCourseProgress(Integer userId, Integer courseId) {
        List<KnowledgeMastery> masteryList = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getUserId, userId)
                        .eq(KnowledgeMastery::getCourseId, courseId));
        if (masteryList.isEmpty()) {
            return 0;
        }
        int sum = masteryList.stream().mapToInt(KnowledgeMastery::getMasteryLevel).sum();
        return sum / masteryList.size();
    }

    public void enroll(Integer userId, Integer courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        Long count = userCourseMapper.selectCount(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .eq(UserCourse::getCourseId, courseId));
        if (count != null && count > 0) {
            return;
        }

        UserCourse userCourse = new UserCourse();
        userCourse.setUserId(userId);
        userCourse.setCourseId(courseId);
        userCourse.setProgress(0);
        userCourseMapper.insert(userCourse);
    }

    public void switchCurrentCourse(Integer userId, Integer courseId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setCurrentCourseId(courseId);
        userMapper.updateById(user);
    }

    public void unenroll(Integer userId, Integer courseId) {
        int deleted = userCourseMapper.delete(new LambdaQueryWrapper<UserCourse>()
                .eq(UserCourse::getUserId, userId)
                .eq(UserCourse::getCourseId, courseId));
        if (deleted == 0) {
            throw new RuntimeException("未加入该课程");
        }
    }

    public Course getCourseById(Integer courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        return course;
    }

    /**
     * 添加课程，courseCode 自动递增（cs1001, cs1002...）
     */
    public Course addCourse(String name, String description, String cover, String source, Integer teacherId) {
        // 查询当前最大的 courseCode
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .isNotNull(Course::getCourseCode)
                .orderByDesc(Course::getCourseCode)
                .last("LIMIT 1");
        List<Course> list = courseMapper.selectList(wrapper);

        String newCode;
        if (list == null || list.isEmpty()) {
            newCode = "cs1001";
        } else {
            String maxCode = list.get(0).getCourseCode();
            // 提取数字部分
            String numPart = maxCode.replaceAll("[^0-9]", "");
            int nextNum = Integer.parseInt(numPart) + 1;
            // 提取前缀
            String prefix = maxCode.replaceAll("[0-9]", "");
            newCode = prefix + nextNum;
        }

        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        course.setCover(cover);
        course.setSource(source);
        course.setTeacherId(teacherId);
        course.setCourseCode(newCode);
        course.setStatus(1);
        courseMapper.insert(course);
        return course;
    }

    /** 编辑课程（教师端）— PUT /courses/{courseId} */
    public Course updateCourse(Integer courseId, Map<String, String> body) {
        Course course = getCourseById(courseId);
        if (body.containsKey("name")) {
            course.setName(body.get("name"));
        }
        if (body.containsKey("description")) {
            course.setDescription(body.get("description"));
        }
        if (body.containsKey("cover")) {
            course.setCover(body.get("cover"));
        }
        if (body.containsKey("source")) {
            course.setSource(body.get("source"));
        }
        courseMapper.updateById(course);
        return course;
    }

    /** 删除课程（教师端，软删除 status=0）— DELETE /courses/{courseId} */
    public void deleteCourse(Integer courseId) {
        Course course = getCourseById(courseId);
        course.setStatus(0);
        courseMapper.updateById(course);
    }

    private CourseVO toCourseVO(Course course) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setDescription(course.getDescription());
        vo.setCover(course.getCover());
        vo.setStatus(course.getStatus());
        return vo;
    }
}
