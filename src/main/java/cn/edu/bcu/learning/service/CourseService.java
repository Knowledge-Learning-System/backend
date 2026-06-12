package cn.edu.bcu.learning.service;

import cn.edu.bcu.learning.domain.entity.Course;
import cn.edu.bcu.learning.domain.entity.User;
import cn.edu.bcu.learning.domain.entity.UserCourse;
import cn.edu.bcu.learning.domain.vo.CourseVO;
import cn.edu.bcu.learning.domain.vo.MyCourseVO;
import cn.edu.bcu.learning.repository.mysql.CourseMapper;
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

    public List<CourseVO> listCourses() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
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
            vo.setProgress(userCourse.getProgress() == null ? 0 : userCourse.getProgress());
            result.add(vo);
        }
        return result;
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

    public Course getCourseById(Integer courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        return course;
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
