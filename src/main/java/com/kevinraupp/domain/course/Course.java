package com.kevinraupp.domain.course;

import com.kevinraupp.domain.course.dto.CreateCourseDTO;
import com.kevinraupp.domain.course.dto.UpdateCourseDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "courses")
@Entity(name = "Course")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private Categories category;

    public Course(Course course) {
        this.id = course.getId();
        this.name = course.getName();
        this.category = course.getCategory();
    }

    public Course(CreateCourseDTO createDTO) {
        this.name = createDTO.name();
        this.category = createDTO.category();
    }

    public void update(UpdateCourseDTO updateDTO) {
        if (updateDTO.name() != null) {
            this.name = updateDTO.name();
        }
        if (updateDTO.category() != null) {
            this.category = updateDTO.category();
        }
    }
}
