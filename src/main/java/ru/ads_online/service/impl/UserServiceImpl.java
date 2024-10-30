package ru.ads_online.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.exception.ForbiddenException;
import ru.ads_online.mapper.UserMapper;
import ru.ads_online.pojo.dto.user.NewPassword;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.dto.user.User;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.UserRepository;
import ru.ads_online.security.DatabaseUserDetails;
import ru.ads_online.service.ImageService;
import ru.ads_online.service.UserService;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;

    @Override
    public void setPassword(Authentication authentication, NewPassword newPassword) {
        UserEntity currentUser = ((DatabaseUserDetails) authentication.getPrincipal()).getUser();
        if (passwordEncoder.matches(newPassword.getCurrentPassword(), currentUser.getPassword())) {
            currentUser.setPassword(passwordEncoder.encode(newPassword.getNewPassword()));
            userRepository.save(currentUser);
        } else {
            throw new ForbiddenException("Wrong password");
        }
    }

    @Override
    public User getData(Authentication authentication) {
        UserEntity currentUser = ((DatabaseUserDetails) authentication.getPrincipal()).getUser();
        return userMapper.toUser(currentUser);
    }

    @Override
    public UpdateUser updateData(Authentication authentication, UpdateUser updateUser) {
        UserEntity currentUser = ((DatabaseUserDetails) authentication.getPrincipal())
                .getUser()
                .setFirstName(updateUser.getFirstName())
                .setLastName(updateUser.getLastName())
                .setPhone(updateUser.getPhone());
        userRepository.save(currentUser);
        return updateUser;
    }

    @Override
    public void updateImage(Authentication authentication, MultipartFile image) {
        UserEntity currentUser = ((DatabaseUserDetails) authentication.getPrincipal()).getUser();
        ImageEntity imageEntity;

        try {
            byte[] imageBytes = image.getBytes();

            if (currentUser.getImage() == null) {
                imageEntity = imageService.uploadImage(imageBytes);
            } else {
                Integer imageId = Integer.valueOf(currentUser.getImage().replaceAll(ImageService.IMAGE_URL_PREFIX, ""));
                imageEntity = imageService.updateImage(imageId, imageBytes);
            }

            String imageURL = ImageService.IMAGE_URL_PREFIX + imageEntity.getId();
            currentUser.setImage(imageURL);
            userRepository.save(currentUser);

        } catch (IOException | NumberFormatException e) {
            log.error("Failed to update image for user {}: {}", currentUser.getId(), e.getMessage());
        }
    }
}