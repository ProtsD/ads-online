package ru.ads_online.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ads_online.exception.NotFoundException;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;

@Component
@RequiredArgsConstructor
public class AuthorizationService {
    private final AdRepository adRepository;

    public boolean hasPermissionForAd(DatabaseUserDetails userDetails, Integer adId) {
        UserEntity currentUser = userDetails.getUser();
        AdEntity currentAd = adRepository.findById(adId).orElseThrow(() -> new NotFoundException("Ad with id=" + adId + " not found."));

        boolean isAuthor = currentUser.getId() == currentAd.getAuthor().getId();
        boolean isAdmin = currentUser.getRole().equals(Role.ADMIN);
        return isAuthor || isAdmin;
    }
}