package ru.ads_online.service;

import ru.ads_online.pojo.entity.ImageEntity;

public interface ImageService {
    String IMAGE_URL_PREFIX = "/images/";


    ImageEntity getImage(int id);

    ImageEntity uploadImage(byte[] image);


    ImageEntity updateImage(int id, byte[] image);


    void deleteImage(int id);
}
