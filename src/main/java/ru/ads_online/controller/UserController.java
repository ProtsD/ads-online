package ru.ads_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.ads_online.pojo.dto.user.NewPassword;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.dto.user.User;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.UserService;

import java.io.IOException;

@CrossOrigin(value = "http://localhost:3000")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @Operation(summary = "Password update", tags = {"Users"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content())})
    @PatchMapping("/set_password")
    public ResponseEntity<?> setPassword(@AuthenticationPrincipal UserPrincipal userDetails,
                                         @RequestBody @Valid NewPassword newPassword) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to update password from user {}", username);

        userService.setPassword(userDetails, newPassword);

        log.info("Password successfully updated for user {}", username);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get information about the authorized user", tags = {"Users"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())})
    @GetMapping("/me")
    public ResponseEntity<User> getData(@AuthenticationPrincipal UserPrincipal userDetails) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to get user details from user {}", username);

        User user = userService.getData(userDetails);

        log.info("Successfully fetched user details for {}", username);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Update information about the authorized user", tags = {"Users"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UpdateUser.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())})
    @PatchMapping("/me")
    public ResponseEntity<UpdateUser> updateData(@AuthenticationPrincipal UserPrincipal userDetails,
                                                 @RequestBody @Valid UpdateUser updateUser) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to update user details from user {}", username);

        UpdateUser updateUserReturn = userService.updateData(userDetails, updateUser);

        log.info("Information successfully updated for user {}", username);
        return ResponseEntity.ok(updateUserReturn);
    }

    @Operation(summary = "Update the avatar of the authorized user", tags = {"Users"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())})
    @PatchMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateImage(@AuthenticationPrincipal UserPrincipal userDetails,
                                         @RequestParam MultipartFile image) throws IOException {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to update avatar from user {}", username);

        userService.updateImage(userDetails, image);

        log.info("Avatar successfully updated for user {}", username);
        return ResponseEntity.ok().build();
    }
}