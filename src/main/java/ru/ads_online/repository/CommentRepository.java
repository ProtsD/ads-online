package ru.ads_online.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.entity.CommentEntity;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Integer> {
    @Query("SELECT new ru.ads_online.pojo.dto.comment.Comment(u.id, u.image, u.firstName, c.createdAt, c.id, c.text)" +
            "FROM CommentEntity c JOIN UserEntity u on c.author = u WHERE c.adEntity.id = :id")
    List<Comment> findAllByAdId(@Param("id") int adId);
}