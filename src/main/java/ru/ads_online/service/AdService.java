package ru.ads_online.service;

import org.apache.tika.mime.MimeTypeException;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.Ads;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.ad.ExtendedAd;
import ru.ads_online.security.UserPrincipal;

public interface AdService {

    /**
     * Returns a list of all available ads and their total count.
     *
     * @return all available ads
     */
    Ads getAllAds();

    /**
     * Creates and saves a new ad.
     *
     * @param userDetails    the current user's authentication
     * @param properties     the ad details
     * @param image          the image file to be associated with the ad
     * @return the created ad
     */
    Ad addAd(UserPrincipal userDetails, CreateOrUpdateAd properties, MultipartFile image) throws MimeTypeException;

    /**
     * Returns detailed information about the ad with the specified ID.
     *
     * @param id             ID of the ad
     * @return extended ad details
     * @throws ru.ads_online.exception.NotFoundException if the ad is not found
     */
    ExtendedAd getAd(int id);

    /**
     * Deletes the ad with the specified ID.
     *
     * @param id             ID of the ad to delete
     * @throws ru.ads_online.exception.ForbiddenException if the user does not have access
     * @throws ru.ads_online.exception.NotFoundException  if the ad is not found
     */
    void deleteAd(int id);

    /**
     * Updates the ad with the specified ID.
     *
     * @param id             ID of the ad to update
     * @param properties     updated ad details
     * @return the updated ad
     * @throws ru.ads_online.exception.ForbiddenException if the user does not have access
     * @throws ru.ads_online.exception.NotFoundException  if the ad is not found
     */
    Ad updateAd(int id, CreateOrUpdateAd properties);

    /**
     * Returns a list and count of ads posted by the currently authenticated user.
     *
     * @param userDetails the current user's authentication
     * @return the user's ads
     */
    Ads getUserAds(UserPrincipal userDetails);

    /**
     * Updates the image of the ad with the specified ID.
     *
     * @param id             ID of the ad whose image to update
     * @param image          the new image file
     * @return URL of the updated image
     * @throws ru.ads_online.exception.ForbiddenException if the user does not have access
     * @throws ru.ads_online.exception.NotFoundException  if the ad is not found
     */
    String updateAdImage(int id, MultipartFile image);
}