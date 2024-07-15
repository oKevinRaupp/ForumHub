package com.kevinraupp.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import com.kevinraupp.domain.course.Course;
import com.kevinraupp.domain.course.CourseRepository;
import com.kevinraupp.domain.course.dto.CreateCourseDTO;
import com.kevinraupp.domain.course.dto.UpdateCourseDTO;
import com.kevinraupp.infra.exceptions.Conflict;
import com.kevinraupp.infra.exceptions.NotFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("course")
@SecurityRequirement(name = "bearer-key")
public class CourseController {

    private final CourseRepository repository;

    public CourseController(CourseRepository courseRepository) {
        this.repository = courseRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Course> create(@RequestBody @Valid CreateCourseDTO createDTO) {
        try {
            var course = repository.save(new Course(createDTO));
            return ResponseEntity.ok(course);
        } catch (DataIntegrityViolationException exception) {
            throw new Conflict("A course with that name already exists");
        }
    }

    @GetMapping
    public ResponseEntity<Page<Course>> getCourses(@PageableDefault(size = 10, sort = {"id"}) Pageable pagination) {
        var page = repository.findAll(pagination).map(Course::new);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        var course = repository.findById(id).orElseThrow(() -> new NotFound("Course not found"));

        return ResponseEntity.ok(course);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody @Valid UpdateCourseDTO courseData) {
        var courseEntity = repository.findById(id).orElseThrow(() -> new NotFound("Course not found"));
        courseEntity.update(courseData);

        return ResponseEntity.ok(courseEntity);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}
