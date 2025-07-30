package ru.ads_online.service;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.pojo.dto.user.NewPassword;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.dto.user.User;
import ru.ads_online.security.UserPrincipal;

import java.io.IOException;

public interface UserService {
    void setPassword(UserPrincipal userDetails, NewPassword newPassword);

    User getData(UserPrincipal userDetails);

    UpdateUser updateData(UserPrincipal userDetails, UpdateUser updateUser);

    void updateImage(UserPrincipal userDetails, MultipartFile image) throws IOException;
}