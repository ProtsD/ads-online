package ru.ads_online.mapper;

import org.springframework.stereotype.Component;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.Ads;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.ad.ExtendedAd;
import ru.ads_online.pojo.entity.AdEntity;

import java.util.List;

@Component
public class AdMapper {

    public AdEntity toAdEntity(CreateOrUpdateAd createOrUpdateAd) {
        if (createOrUpdateAd == null) {
            return null;
        }

        return new AdEntity()
                .setPrice(createOrUpdateAd.getPrice())
                .setTitle(createOrUpdateAd.getTitle())
                .setDescription(createOrUpdateAd.getDescription());
    }

    public Ad toAd(AdEntity adEntity) {
        if (adEntity == null) {
            return null;
        }

        return new Ad()
                .setAuthor(adEntity.getAuthor().getId())
                .setImage(adEntity.getImage())
                .setPk(adEntity.getId())
                .setPrice(adEntity.getPrice())
                .setTitle(adEntity.getTitle());
    }

    public ExtendedAd toExtendedAd(AdEntity adEntity) {
        if (adEntity == null) {
            return null;
        }

        return new ExtendedAd()
                .setPk(adEntity.getId())
                .setAuthorFirstName(adEntity.getAuthor().getFirstName())
                .setAuthorLastName(adEntity.getAuthor().getLastName())
                .setDescription(adEntity.getDescription())
                .setEmail(adEntity.getAuthor().getUsername())
                .setImage(adEntity.getImage())
                .setPhone(adEntity.getAuthor().getPhone())
                .setPrice(adEntity.getPrice())
                .setTitle(adEntity.getTitle());
    }

    public Ads toAds(List<Ad> adList) {
        if (adList == null) {
            return null;
        }

        return new Ads()
                .setCount(adList.size())
                .setResults(adList);
    }
}
