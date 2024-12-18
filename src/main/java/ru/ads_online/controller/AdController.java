package ru.ads_online.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.Ads;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.ad.ExtendedAd;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.AdService;

import java.net.URI;

@CrossOrigin(value = "http://localhost:3000")
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
@Slf4j
public class AdController {
    private final AdService adService;

    @GetMapping()
    public ResponseEntity<Ads> getAllAds() {
        log.info("Received request to fetch all ads");

        Ads allAds = adService.getAllAds();

        log.info("Successfully fetched all {} ads", allAds.getCount());
        return ResponseEntity.ok(allAds);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ad> addAd(@AuthenticationPrincipal UserPrincipal userDetails,
                                    @RequestPart @Valid CreateOrUpdateAd properties,
                                    @RequestPart MultipartFile image) throws MimeTypeException {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to create ad with title={} from user={}", username, properties.getTitle());

        Ad createdAd = adService.addAd(userDetails, properties, image);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdAd.getId())
                .toUri();

        log.info("Successfully created ad with id={} ", createdAd.getId());
        return ResponseEntity.created(location).body(createdAd);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtendedAd> getAd(@Positive @PathVariable(name = "id") Integer id) {
        log.info("Received request to fetch ad with id={}", id);
        ExtendedAd adInfo = adService.getAd(id);
        log.info("Successfully fetched ad with id={}", id);
        return ResponseEntity.ok(adInfo);
    }

    @PreAuthorize("@authorizationService.hasPermissionForAd(#userDetails, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAd(@AuthenticationPrincipal UserPrincipal userDetails,
                                         @Positive @PathVariable(name = "id") int id) {

        String username = userDetails.getUser().getUsername();
        log.info("Received request to delete ad with id={} from user={}", username, id);

        adService.deleteAd(id);

        log.info("Successfully deleted ad with id={}", id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.hasPermissionForAd(#userDetails, #id)")
    @PatchMapping("/{id}")
    public ResponseEntity<Ad> updateAd(@AuthenticationPrincipal UserPrincipal userDetails,
                                       @PathVariable(name = "id") int id,
                                       @RequestBody @Valid CreateOrUpdateAd properties) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to update ad id={} from user={}", username, id);

        Ad updateAdInfo = adService.updateAd(id, properties);

        log.info("Successfully updated ad with id={}", id);
        return ResponseEntity.ok(updateAdInfo);
    }

    @GetMapping("/me")
    public ResponseEntity<Ads> getUserAds(@AuthenticationPrincipal UserPrincipal userDetails) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to get all ads of user={}", username);

        Ads currentUserAds = adService.getUserAds(userDetails);

        log.info("Successfully received {} ads of user={}", currentUserAds.getCount(), username);
        return ResponseEntity.ok(currentUserAds);
    }

    @PreAuthorize("@authorizationService.hasPermissionForAd(#userDetails, #id)")
    @PatchMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateAdImage(@AuthenticationPrincipal UserPrincipal userDetails,
                                                @PathVariable(name = "id") int id,
                                                @RequestParam MultipartFile image) {
        String username = userDetails.getUser().getUsername();
        log.info("Received request to update ad id={} with new image from user={}", id, username);

        String updateAdImageUrl = adService.updateAdImage(id, image);

        log.info("Successfully updated ad with id={} by image size={}", id, image.getSize());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .body(updateAdImageUrl);
    }
}