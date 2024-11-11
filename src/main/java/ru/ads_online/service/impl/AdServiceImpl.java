package ru.ads_online.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.exception.ImageDeletionException;
import ru.ads_online.exception.ImageUploadException;
import ru.ads_online.exception.NotFoundException;
import ru.ads_online.mapper.AdMapper;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.Ads;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.ad.ExtendedAd;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;
import ru.ads_online.security.DatabaseUserDetails;
import ru.ads_online.service.AdService;
import ru.ads_online.service.ImageService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdServiceImpl implements AdService {
    private final AdRepository adRepository;
    private final AdMapper adMapper;
    private final ImageService imageService;

    @Transactional(readOnly = true)
    @Override
    public Ads getAllAds() {
        List<AdEntity> adList = StreamSupport.stream(adRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        return adMapper.toAds(adList);
    }

    @Transactional
    @Override
    public Ad addAd(DatabaseUserDetails userDetails, CreateOrUpdateAd adBody, MultipartFile image) {
        UserEntity author = userDetails.getUser();

        if (image == null || image.isEmpty()) {
            String message = "No image provided for ad titled";
            log.warn(message);
            throw new ImageUploadException(message);
        }
        AdEntity currentAd = adMapper.toAdEntity(adBody).setAuthor(author);

        String imageURL = uploadImage(image);
        return adMapper.toAd(adRepository.save(currentAd.setImage(imageURL)));
    }

    @Transactional(readOnly = true)
    @Override
    public ExtendedAd getAd(int id) {
        AdEntity currentAd = adRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format("Ad with id=%d was not found", id);
                    log.warn(message);
                    return new NotFoundException(message);
                });
        return adMapper.toExtendedAd(currentAd);
    }

    @Transactional
    @Override
    public void deleteAd(int id) {
        AdEntity currentAd = adRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format("Ad with id=%d was not found", id);
                    log.warn(message);
                    return new NotFoundException(message);
                });

        int imageId = getImageIdFromUrl(currentAd.getImage());
        imageService.deleteImage(imageId);
        adRepository.delete(currentAd);
    }

    @Transactional
    @Override
    public Ad updateAd(int id, CreateOrUpdateAd properties) {
        AdEntity currentAd = adRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format("Ad with id=%d was not found", id);
                    log.warn(message);
                    return new NotFoundException(message);
                });

        currentAd.setTitle(properties.getTitle())
                .setPrice(properties.getPrice())
                .setDescription(properties.getDescription());
        return adMapper.toAd(adRepository.save(currentAd));
    }

    @Transactional(readOnly = true)
    @Override
    public Ads getUserAds(DatabaseUserDetails userDetails) {
        int currentUserId = userDetails.getUser().getId();
        List<AdEntity> adList = adRepository.findAllByAuthorId(currentUserId)
                .orElseThrow(() -> {
                    String message = "No ads found for the current user";
                    log.warn(message);
                    return new NotFoundException(message);
                });
        return adMapper.toAds(adList);
    }

    @Transactional
    @Override
    public String updateAdImage(int id, MultipartFile image) {
        AdEntity currentAd = adRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format("Ad with id=%d was not found", id);
                    log.warn(message);
                    return new NotFoundException(message);
                });
        String newImageURL = uploadImage(image);
        int oldImageID = getImageIdFromUrl(currentAd.getImage());
        imageService.deleteImage(oldImageID);
        currentAd.setImage(newImageURL);
        adRepository.save(currentAd);
        return currentAd.getImage();
    }

    private String uploadImage(MultipartFile image) {
        try {
            byte[] imageBytes = image.getBytes();
            ImageEntity imageEntity = imageService.uploadImage(imageBytes);
            if (imageEntity == null) {
                String message = "Image upload failed";
                log.warn(message);
                throw new ImageUploadException(message);
            }
            return getImageUrl(imageEntity);
        } catch (IOException e) {
            log.error("Error occurred while uploading image: {}", e.getMessage());
            throw new ImageUploadException("Image upload failed due to IO exception", e);
        }
    }

    private String getImageUrl(ImageEntity imageEntity) {
        return ImageService.IMAGE_URL_PREFIX + imageEntity.getId();
    }

    private int getImageIdFromUrl(String imageUrl) {
        try {
            return Integer.parseInt(imageUrl.replaceAll(ImageService.IMAGE_URL_PREFIX, ""));
        } catch (NumberFormatException e) {
            String message = String.format("Failed to parse image ID, error message: %s", e.getMessage());
            log.error(message);
            throw new ImageDeletionException(message);
        }
    }
}
