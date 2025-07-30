package ru.ads_online.service;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.pojo.dto.user.NewPassword;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.dto.user.User;
import ru.ads_online.security.UserPrincipal;

import java.io.IOException;

public interface UserService {

    /**
     * Sets a new password for the authenticated user.
     *
     * @param userDetails the current user
     * @param newPassword    the new password data
     * @throws ru.ads_online.exception.ForbiddenException if the old password is incorrect
     */
    void setPassword(UserPrincipal userDetails, NewPassword newPassword);

    /**
     * Returns data of the authenticated user.
     *
     * @param userDetails the current authenticated user
     * @return the user data
     */
    User getData(UserPrincipal userDetails);

    /**
     * Updates data of the authenticated user.
     *
     * @param userDetails the current authenticated user
     * @param updateUser     the updated user data
     * @return the updated user data
     */
    UpdateUser updateData(UserPrincipal userDetails, UpdateUser updateUser);

    /**
     * Updates the avatar image of the authenticated user.
     *
     * @param userDetails the current authenticated user
     * @param image          the new avatar image file
     */
    void updateImage(UserPrincipal userDetails, MultipartFile image) throws IOException;
}