package ru.ads_online.repository;

import org.springframework.data.repository.CrudRepository;
import ru.ads_online.pojo.entity.AdEntity;

import java.util.List;
import java.util.Optional;

public interface AdRepository extends CrudRepository<AdEntity, Integer> {
    Optional<List<AdEntity>> findAllByAuthorId(Integer id);
}
