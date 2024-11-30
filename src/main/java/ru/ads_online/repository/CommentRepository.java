package ru.ads_online.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import ru.ads_online.pojo.entity.CommentEntity;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Integer> {
    Optional<List<CommentEntity>> findAllByAdEntityId(int id);
}