package com.kevinraupp.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentDTO(@NotBlank String text) {
}
