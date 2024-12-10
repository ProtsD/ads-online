package ru.ads_online.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ads_online.exception.NotFoundException;
import ru.ads_online.mapper.CommentMapper;
import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.dto.comment.Comments;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;
import ru.ads_online.repository.CommentRepository;
import ru.ads_online.service.CommentService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final AdRepository adRepository;

    @Transactional(readOnly = true)
    @Override
    public Comments getAllComments(int adId) {
        if (!adRepository.existsById(adId)) {
            String message = String.format("Ad with id=%d was not found", adId);
            log.warn(message);
            throw new NotFoundException(message);
        }
        List<Comment> comments = commentRepository.findAllByAdId(adId);
        return commentMapper.toComments(comments);
    }

    @Override
    public Comment createComment(UserEntity user, int adId, CreateOrUpdateComment createOrUpdateComment) {
        CommentEntity result = commentMapper.toCommentEntity(
                createOrUpdateComment,
                user,
                adRepository.findById(adId)
                        .orElseThrow(() -> {
                            String message = String.format("Ad with id=%d was not found", adId);
                            log.warn(message);
                            return new NotFoundException(message);
                        })
        );
        CommentEntity createdComment = commentRepository.save(result);
        return commentMapper.toComment(commentRepository.save(createdComment));
    }

    @Override
    public void deleteComment(int adId, int commentId) {
        checkInputParameters(adId, commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public Comment updateComment(int adId, int commentId, CreateOrUpdateComment createOrUpdateComment) {
        CommentEntity commentEntity = checkInputParameters(adId, commentId);
        commentEntity.setText(createOrUpdateComment.getText());
        CommentEntity updatedCommentEntity = commentRepository.save(commentEntity);
        return commentMapper.toComment(updatedCommentEntity);
    }

    private CommentEntity checkInputParameters(int adId, int commentId) {
        CommentEntity commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    String message = String.format("Comment with id=%d was not found", commentId);
                    log.warn(message);
                    return new NotFoundException(message);
                });
        if (adId != commentEntity.getAdEntity().getId()) {
            String message = String.format("Comment id=%d does not belong to ad id=%d", commentId, adId);
            log.warn(message);
            throw new NotFoundException(message);
        }
        return commentEntity;
    }
}