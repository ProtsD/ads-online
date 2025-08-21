package ru.ads_online.repository;

import org.springframework.data.repository.CrudRepository;
import ru.ads_online.pojo.entity.ImageEntity;

public interface ImageRepository extends CrudRepository<ImageEntity,Integer> {
}
