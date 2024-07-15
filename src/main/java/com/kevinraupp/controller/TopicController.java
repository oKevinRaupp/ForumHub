package com.kevinraupp.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import com.kevinraupp.domain.comment.Comment;
import com.kevinraupp.domain.comment.CommentRepository;
import com.kevinraupp.domain.comment.dto.CommentResponseDTO;
import com.kevinraupp.domain.comment.dto.CreateCommentDTO;
import com.kevinraupp.domain.course.CourseRepository;
import projeto.domain.topic.Status;
import com.kevinraupp.domain.topic.Topic;
import com.kevinraupp.domain.topic.TopicRepository;
import com.kevinraupp.domain.topic.dto.CreateTopicDTO;
import com.kevinraupp.domain.topic.dto.TopicResponseDTO;
import com.kevinraupp.domain.topic.dto.UpdateTopicDTO;
import com.kevinraupp.domain.user.UserRepository;
import com.kevinraupp.infra.exceptions.Forbidden;
import com.kevinraupp.infra.exceptions.NotFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/topic")
@SecurityRequirement(name = "bearer-key")
public class TopicController {

    private final CommentRepository commentRepository;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public TopicController(CommentRepository commentRepository, CourseRepository courseRepository, TopicRepository topicRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TopicResponseDTO> createTopic(@RequestBody @Valid CreateTopicDTO createDTO, Authentication authentication) {
        var userId = Long.parseLong(authentication.getCredentials().toString());
        var user = userRepository.findById(userId).orElseThrow(() -> new NotFound("User not found"));
        var course = courseRepository.findById(createDTO.course()).orElseThrow(() -> new NotFound("Course not found"));
        var topic = topicRepository.save(new Topic(createDTO, user, course));

        return ResponseEntity.ok().body(new TopicResponseDTO(topic));
    }

    @PostMapping("{topicId}/comments")
    @Transactional
    public ResponseEntity<CommentResponseDTO> createComment(@RequestBody @Valid CreateCommentDTO createDTO, @PathVariable Long topicId, Authentication authentication) {
        var userId = Long.parseLong(authentication.getCredentials().toString());
        var user = userRepository.getReferenceById(userId);
        var topic = topicRepository.findById(topicId).orElseThrow(() -> new NotFound("Topic not found"));

        if (topic.getStatus() != Status.SOLUCIONADO) {
            var comment = commentRepository.save(new Comment(createDTO.text(), topic, user));
            return ResponseEntity.ok(new CommentResponseDTO(comment));
        } else {
            throw new Forbidden("This topic is closed!");
        }
    }

    @GetMapping("/{topicId}")
    public ResponseEntity<TopicResponseDTO> getTopicById(@PathVariable Long topicId) {
        var topic = topicRepository.findById(topicId).orElseThrow(() -> new NotFound("Topic not found!"));
        return ResponseEntity.ok().body(new TopicResponseDTO(topic));
    }

    @GetMapping("/{topicId}/comments")
    public ResponseEntity<Page<CommentResponseDTO>> getTopicComments(@PageableDefault(size = 10, sort = {"createdAt"}) Pageable pagination, @PathVariable Long topicId) {
        var page = commentRepository.findAllByTopicId(pagination, topicId).map(CommentResponseDTO::new);
        return ResponseEntity.ok(page);
    }

    @GetMapping
    public ResponseEntity<Page<TopicResponseDTO>> getTopics(@RequestParam(required = false) String title, @PageableDefault(size = 10, sort = {"createdAt"}) Pageable pagination) {
        if (title != null && !title.isEmpty()) {
            var page = topicRepository.findAllByTitleContainingIgnoreCaseOrderByCreatedAtDesc(title, pagination).map(TopicResponseDTO::new);

            return ResponseEntity.ok(page);
        }
        var page = topicRepository.findAllByOrderByCreatedAtDesc(pagination).map(TopicResponseDTO::new);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<TopicResponseDTO>> getTopicsByCourseAndStatus(@RequestParam(required = false) String course, @RequestParam(required = false) String status, @PageableDefault(size = 10, sort = {"createdAt"}) Pageable pagination) {
        Status statusEnum = null;

        if (status != null) {
            try {
                statusEnum = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status value: " + status);
            }
        }

        if (course != null && !course.isEmpty() && statusEnum != null) {
            var page = topicRepository.findAllByCourseNameAndStatusIsOrderByCreatedAtDesc(course, statusEnum, pagination).map(TopicResponseDTO::new);
            return ResponseEntity.ok(page);
        } else if (course != null && !course.isEmpty()) {
            var page = topicRepository.findAllByCourseNameOrderByCreatedAtDesc(course, pagination).map(TopicResponseDTO::new);
            return ResponseEntity.ok(page);
        } else {
            var page = topicRepository.findAllByStatusOrderByCreatedAtDesc(statusEnum, pagination).map(TopicResponseDTO::new);
            return ResponseEntity.ok(page);
        }
    }

    @PutMapping("/{topicId}")
    @Transactional
    public ResponseEntity<TopicResponseDTO> updateTopic(@RequestBody @Valid UpdateTopicDTO updateDTO, @PathVariable Long topicId, Authentication authentication) {
        var topic = topicRepository.findById(topicId).orElseThrow(() -> new NotFound("Topic not found"));
        var userId = authentication.getCredentials().toString();
        if (!topic.getAuthor().getId().toString().equals(userId)) {
            throw new Forbidden("You do not have permission to perform this action");
        }
        topic.update(updateDTO);

        return ResponseEntity.ok().body(new TopicResponseDTO(topic));
    }

    @DeleteMapping("/{topicId}")
    @Transactional
    public ResponseEntity<Void> deleteTopic(@PathVariable Long topicId, Authentication authentication) {
        var userId = authentication.getCredentials().toString();
        var topic = topicRepository.findById(topicId).orElseThrow(() -> new NotFound("Topic not found"));
        if (!topic.getAuthor().getId().toString().equals(userId)) {
            throw new Forbidden("You do not have permission to perform this action");
        }
        topicRepository.deleteById(topicId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{topicId}/comments/{commentId}")
    @Transactional
    public ResponseEntity<CommentResponseDTO> markCommentAsSolution(@PathVariable Long topicId, @PathVariable Long commentId, Authentication authentication) {
        var userId = authentication.getCredentials().toString();
        var topic = topicRepository.findById(topicId).orElseThrow(() -> new NotFound("Topic not found"));
        var comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFound("Comment not found"));
        if (!topic.getAuthor().getId().toString().equals(userId) || !comment.getTopic().getId().equals(topic.getId())) {
            throw new Forbidden("You do not have permission to perform this action");
        }

        comment.markAsSolution();
        topic.solved();

        return ResponseEntity.ok(new CommentResponseDTO(comment));
    }
}
