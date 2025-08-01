package ru.ads_online.repository;

import org.springframework.data.repository.CrudRepository;
import ru.ads_online.pojo.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);
}

