package ru.ads_online.service;

import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.dto.comment.Comments;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.entity.UserEntity;

public interface CommentService {

    Comments getAllComments(int id);


    Comment createComment(UserEntity userId, int adId, CreateOrUpdateComment comment);

    void deleteComment(int adId, int commentId);

    Comment updateComment(int adId, int commentId, CreateOrUpdateComment createOrUpdateComment);
}
