package ru.ads_online.service;

import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.dto.comment.Comments;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.entity.UserEntity;

public interface CommentService {

    /**
     * Returns all comments associated with the specified ad.
     *
     * @param id             ID of the ad
     * @return a list of comments for the ad
     */
    Comments getAllComments(int id);

    /**
     * Creates a new comment for the specified ad.
     *
     * @param userId the current user
     * @param adId             ID of the ad
     * @param comment        the comment content
     * @return the created comment
     */
    Comment createComment(UserEntity userId, int adId, CreateOrUpdateComment comment);

    /**
     * Deletes the specified comment from the ad.
     *
     * @param adId           ID of the ad
     * @param commentId      ID of the comment to delete
     * @throws ru.ads_online.exception.ForbiddenException if the user does not have permission to delete the comment
     * @throws ru.ads_online.exception.NotFoundException  if the comment is not found
     */
    void deleteComment(int adId, int commentId);

    /**
     * Updates the specified comment.
     *
     * @param adId                  ID of the ad
     * @param commentId             ID of the comment to update
     * @param createOrUpdateComment updated comment content
     * @return the updated comment
     * @throws ru.ads_online.exception.ForbiddenException if the user does not have permission to update the comment
     * @throws ru.ads_online.exception.NotFoundException  if the comment is not found
     */
    Comment updateComment(int adId, int commentId, CreateOrUpdateComment createOrUpdateComment);
}
