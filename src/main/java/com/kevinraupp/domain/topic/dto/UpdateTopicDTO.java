package com.kevinraupp.domain.topic.dto;

import projeto.domain.topic.Status;

public record UpdateTopicDTO(
        String title,
        String text,
        Status status
) {
}
