package ru.ads_online.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ads_online.exception.ImageUploadException;
import ru.ads_online.exception.NotFoundException;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.service.ImageService;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;

    @Value("${image.upload.max-size}")
    private int maxImageSize;

    @Transactional(readOnly = true)
    @Override
    public ImageEntity getImage(int id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format("Image with id=%d was not found", id);
                    log.warn(message);
                    return new NotFoundException(message);
                });
    }

    @Override
    public ImageEntity uploadImage(byte[] image) {

        if (image == null || image.length == 0) {
            String message = "No image provided or empty image data";
            log.warn(message);
            throw new ImageUploadException(message);
        }

        if (image.length > maxImageSize) {
            String message = String.format("Image size exceeds the allowed limit: %d bytes", maxImageSize);
            log.warn(message);
            throw new ImageUploadException(message);
        }

        ImageEntity newImage = new ImageEntity();
        newImage.setImage(image);
        return imageRepository.save(newImage);
    }

    @Override
    public ImageEntity updateImage(int id, byte[] image) {
        ImageEntity imageEntity = imageRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format("Image with id=%d was not found", id);
                    log.warn(message);
                    return new NotFoundException(message);
                });

        if (Arrays.equals(imageEntity.getImage(), image)) {
            log.info("Image is unchanged, skipping save.");
            return imageEntity;
        }

        imageEntity.setImage(image);
        return imageRepository.save(imageEntity);
    }

    @Override
    public void deleteImage(int id) {

        imageRepository.findById(id)
                .ifPresentOrElse(
                        imageRepository::delete,
                        () -> {
                            String message = String.format("Image with id=%d was not found", id);
                            log.warn(message);
                            throw new NotFoundException(message);
                        }
                );
    }
}

