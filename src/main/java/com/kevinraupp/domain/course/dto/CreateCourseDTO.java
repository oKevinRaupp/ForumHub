package com.kevinraupp.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import com.kevinraupp.domain.course.Categories;

public record CreateCourseDTO(@NotBlank String name, Categories category) {

}
