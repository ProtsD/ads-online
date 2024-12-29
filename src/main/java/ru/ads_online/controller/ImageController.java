package ru.ads_online.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.service.ImageService;

@CrossOrigin(value = "http://localhost:3000")
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {
    private final ImageService imageService;
    private static final Tika tika = new Tika();

    @Operation(summary = "Get image", tags = {"Images"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = {
                    @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "byte")),
                    @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "byte")),
                    @Content(mediaType = MediaType.IMAGE_GIF_VALUE, schema = @Schema(type = "string", format = "byte"))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content())}
    )
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@Positive @PathVariable(name = "id") int id) {
        log.info("Received request to fetch image with id={}", id);

        ImageEntity imageEntity = imageService.getImage(id);
        String mimeType = determineContentType(imageEntity);

        log.info("Successfully fetched image with id={}", id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(imageEntity.getImage());
    }

    private String determineContentType(ImageEntity imageEntity) {
        byte[] imageData = imageEntity.getImage();
        return tika.detect(imageData);
    }
}