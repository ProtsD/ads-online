package ru.ads_online.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.dto.comment.Comments;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.CommentService;

import java.net.URI;

@CrossOrigin(value = "http://localhost:3000")
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{id}/comments")
    public ResponseEntity<Comments> getAllCommentsForAd(@PathVariable(name = "id") @Positive int adId) {
        log.info("Received request to fetch all comments for Ad ID: {}", adId);

        Comments comments = commentService.getAllComments(adId);

        log.info("Successfully fetched {} comments for Ad ID: {}", comments.getCount(), adId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> createComment(@AuthenticationPrincipal UserPrincipal userDetails,
                                                 @PathVariable(name = "id") @Positive int adId,
                                                 @RequestBody @Valid CreateOrUpdateComment properties) {
        UserEntity user = userDetails.getUser();
        log.info("Received request to create comment for Ad ID={} from user={}", adId, user.getUsername());

        Comment comment = commentService.createComment(user, adId, properties);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(comment.getId())
                .toUri();

        log.info("Successfully created comment with id={} for Ad ID={}", comment.getId(), adId);
        return ResponseEntity.created(location).body(comment);
    }

    @PreAuthorize("@authorizationService.hasPermissionForComment(#userDetails, #adId, #commentId)")
    @DeleteMapping("/{adId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal UserPrincipal userDetails,
                                           @PathVariable(name = "adId") @Positive int adId,
                                           @PathVariable(name = "commentId") @Positive int commentId) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to delete comment with id={} for ad id={} from user={}", commentId, adId, username);

        commentService.deleteComment(adId, commentId);

        log.info("Successfully deleted comment with id={} for ad id={}", commentId, adId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.hasPermissionForComment(#userDetails, #adId, #commentId)")
    @PatchMapping("/{adId}/comments/{commentId}")
    public ResponseEntity<Comment> updateComment(@AuthenticationPrincipal UserPrincipal userDetails,
                                                 @PathVariable @Positive int adId,
                                                 @PathVariable @Positive int commentId,
                                                 @RequestBody @Valid CreateOrUpdateComment createOrUpdateComment) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to update comment with id={} for ad id={} from user={}", commentId, adId, username);

        Comment updatedComment  = commentService.updateComment(adId, commentId, createOrUpdateComment);

        log.info("Successfully updated comment with id={} for ad id={}", commentId, adId);
        return ResponseEntity.ok(updatedComment );
    }
}
