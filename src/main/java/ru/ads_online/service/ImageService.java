package ru.ads_online.service;

import ru.ads_online.pojo.entity.ImageEntity;

public interface ImageService {
    String IMAGE_URL_PREFIX = "/images/";

    /**
     * Returns the image entity by its ID.
     *
     * @param id the ID of the requested image
     * @return the image entity
     */
    ImageEntity getImage(int id);

    /**
     * Saves a new image.
     *
     * @param image the image bytes
     * @return the saved image entity
     */
    ImageEntity uploadImage(byte[] image);

    /**
     * Updates the existing image with the specified ID.
     *
     * @param id    the ID of the image to update
     * @param image the new image bytes
     * @return the updated image entity
     */
    ImageEntity updateImage(int id, byte[] image);

    /**
     * Deletes the image with the specified ID.
     *
     * @param id the ID of the image to delete
     */
    void deleteImage(int id);
}
