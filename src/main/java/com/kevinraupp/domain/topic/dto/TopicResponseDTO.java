package com.kevinraupp.domain.topic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kevinraupp.domain.topic.Topic;
import projeto.domain.topic.Status;

import java.time.LocalDateTime;

public record TopicResponseDTO(
        Long id,
        String title,
        String text,
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        Status status,
        String author,
        String course
) {

    public TopicResponseDTO(Topic topic) {
        this(topic.getId(), topic.getTitle(), topic.getText(), topic.getCreatedAt(), topic.getStatus(), topic.getAuthor().getName(), topic.getCourse().getName());
    }
}
