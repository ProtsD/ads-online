package ru.ads_online.service;

import ru.ads_online.pojo.entity.ImageEntity;

public interface ImageService {
    String IMAGE_URL_PREFIX = "/images/";


    ImageEntity getImage(Integer id);

    ImageEntity uploadImage(byte[] image);


    ImageEntity updateImage(Integer id, byte[] image);


    void deleteImage(Integer id);
}
