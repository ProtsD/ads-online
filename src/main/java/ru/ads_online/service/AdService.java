package ru.ads_online.service;

import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.Ads;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.ad.ExtendedAd;
import ru.ads_online.security.UserPrincipal;

public interface AdService {
    Ads getAllAds();

    Ad addAd(UserPrincipal userDetails, CreateOrUpdateAd properties, MultipartFile image);

    ExtendedAd getAd(int id);

    void deleteAd(int id);

    Ad updateAd(int id, CreateOrUpdateAd properties);

    Ads getUserAds(UserPrincipal userDetails);

    String updateAdImage(int id, MultipartFile image);
}