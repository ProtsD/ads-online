package ru.ads_online.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ads_online.exception.NotFoundException;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;
import ru.ads_online.repository.CommentRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    private final AdRepository adRepository;
    private final CommentRepository commentRepository;

    public boolean hasPermissionForAd(UserPrincipal userDetails, int adId) {
        UserEntity currentUser = userDetails.getUser();
        AdEntity adEntity = adRepository.findById(adId)
                .orElseThrow(() -> {
                    String message = String.format("Ad with id=%d was not found", adId);
                    log.warn(message);
                    return new NotFoundException(message);
                });

        boolean isAuthor = currentUser.getId() == adEntity.getAuthor().getId();
        boolean isAdmin = currentUser.getRole().equals(Role.ADMIN);
        return isAuthor || isAdmin;
    }

    public boolean hasPermissionForComment(UserPrincipal userDetails, int adId, int commentId) {
        UserEntity currentUser = userDetails.getUser();
        CommentEntity commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    String message = String.format("Comment with id=%d was not found", commentId);
                    log.warn(message);
                    return new NotFoundException(message);
                });
        if (adId == commentEntity.getAdEntity().getId()) {
            boolean isAuthor = currentUser.getId() == commentEntity.getAuthor().getId();
            boolean isAdmin = currentUser.getRole().equals(Role.ADMIN);
            return isAuthor || isAdmin;
        } else {
            String message = String.format("Comment id=%d does not belong to ad id=%d", commentId, adId);
            log.warn(message);
            throw new NotFoundException(message);
        }
    }
}