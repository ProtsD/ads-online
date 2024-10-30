package ru.ads_online.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ads_online.exception.NotFoundException;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.service.ImageService;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;

    @Override
    public ImageEntity getImage(Integer id) {
        return imageRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Image with id=" + id + " doesn't found.")
                );
    }

    @Override
    public ImageEntity uploadImage(byte[] image) {
        ImageEntity newImage = new ImageEntity();
        newImage.setImage(image);

        newImage = imageRepository.save(newImage);

        return newImage;
    }

    @Override
    public ImageEntity updateImage(Integer id, byte[] image) {
        ImageEntity imageEntity = new ImageEntity();

        if (imageRepository.existsById(id)) {
            imageEntity.setId(id);
            imageEntity.setImage(image);
            imageEntity = imageRepository.save(imageEntity);
        } else {
            throw new NotFoundException("Image with id=" + id + " doesn't found.");
        }

        return imageEntity;
    }

    @Override
    public void deleteImage(Integer id) {
        if (imageRepository.existsById(id)) {
            imageRepository.deleteById(id);
        } else {
            throw new NotFoundException("");
        }
    }
}

