package ru.ads_online.mapper;

import org.springframework.stereotype.Component;
import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.dto.comment.Comments;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.UserEntity;

import java.util.Date;
import java.util.List;

@Component
public class CommentMapper {
    public CommentEntity toCommentEntity(CreateOrUpdateComment createOrUpdateComment, UserEntity userEntity, AdEntity adEntity) {
        Date date = new Date();
        if (userEntity == null || adEntity == null) {
            return null;
        } else {
            return new CommentEntity()
                    .setAuthor(userEntity)
                    .setAdEntity(adEntity)
                    .setCreatedAt(date.getTime())
                    .setText(createOrUpdateComment.getText());
        }
    }

    public Comment toComment(CommentEntity commentEntity) {
        return new Comment()
                .setAuthor(commentEntity.getAuthor().getId())
                .setText(commentEntity.getText())
                .setPk(commentEntity.getId())
                .setCreatedAt(commentEntity.getCreatedAt())
                .setAuthorImage(commentEntity.getAuthor().getImage())
                .setAuthorFirstName(commentEntity.getAuthor().getFirstName());
    }

    public Comments toComments(List<Comment> commentList) {
        return new Comments()
                .setCount(commentList.size())
                .setResults(commentList);
    }
}