package ru.ads_online.controller.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import net.minidev.json.JSONObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.ImageService;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestUtils {
    public static final int usernameMinSize = 4, usernameMaxSize = 32;
    public static final int passwordMinSize = 8, passwordMaxSize = 16;
    public static final int firstNameMinSize = 2, firstNameMaxSize = 16;
    public static final int lastNameMinSize = 2, lastNameMaxSize = 16;
    public static final int titleMinSize = 4, titleMaxSize = 32;
    public static final int descriptionMinSize = 8, descriptionMaxSize = 64;
    public static final int commentMinSize = 8, commentMaxSize = 64;

    static final int maxPrice = 10000000;
    static final int maxImageSize = 10485760;
    static final Faker fakerEn = new Faker(Locale.forLanguageTag("en-US"));
    static final Faker fakerRu = new Faker(Locale.forLanguageTag("ru-RU"));
    static final Random random = new Random();

    public static List<UserEntity> createUniqueUsers(int numberOfUsers, PasswordEncoder passwordEncoder) {
        List<String> emails = getEmails(numberOfUsers);
        List<String> passwords = getDistinctPasswords(numberOfUsers);
        List<String> firstNames = getFirstNames(numberOfUsers);
        List<String> lastNames = getLastNames(numberOfUsers);
        List<String> uniquePhones = getPhones(numberOfUsers);

        return IntStream.range(0, numberOfUsers)
                .mapToObj(i -> new UserEntity()
                        .setUsername(emails.get(i))
                        .setPassword(passwordEncoder.encode(passwords.get(i)))
                        .setFirstName(firstNames.get(i))
                        .setLastName(lastNames.get(i))
                        .setPhone(uniquePhones.get(i))
                        .setRole(Role.USER)
                        .setImage(null))
                .toList();
    }

    public static UserEntity createAdmin(PasswordEncoder passwordEncoder) {
        return createUniqueUsers(1, passwordEncoder).getFirst().setRole(Role.ADMIN);
    }

    public static UserEntity getRandomUserFrom(List<UserEntity> users) {
        return users.get(random.nextInt(users.size()));
    }

    public static List<String> getEmails(int quantity) {
        return Stream.generate(() -> fakerEn.internet().emailAddress())
                .distinct()
                .filter(email -> email.length() >= usernameMinSize)
                .map(email -> email.substring(Math.max(email.length(), usernameMaxSize) - usernameMaxSize))
                .limit(quantity)
                .toList();
    }

    public static List<String> getDistinctPasswords(int quantity) {
        return Stream.generate(() -> fakerEn.internet().password(passwordMinSize, passwordMaxSize))
                .distinct()
                .limit(quantity)
                .toList();
    }

    public static String getPassword() {
        return fakerEn.internet().password(passwordMinSize, passwordMaxSize);
    }

    public static List<String> getFirstNames(int quantity) {
        return Stream.generate(() -> fakerEn.name().firstName())
                .filter(firstName -> firstName.length() >= firstNameMinSize)
                .map(firstName -> firstName.substring(0, Math.min(firstName.length(), firstNameMaxSize)))
                .limit(quantity)
                .toList();
    }

    public static List<String> getLastNames(int Quantity) {
        return Stream.generate(() -> fakerEn.name().lastName())
                .filter(lastName -> lastName.length() >= lastNameMinSize)
                .map(lastName -> lastName.substring(0, Math.min(lastName.length(), lastNameMaxSize)))
                .limit(Quantity)
                .toList();
    }

    public static List<String> getPhones(int quantity) {
        return Stream.generate(() -> fakerRu.phoneNumber().phoneNumber())
                .distinct()
                .limit(quantity)
                .toList();
    }

    public static List<AdEntity> createAds(int numberOfAds, List<UserEntity> users, ImageService imageService) {
        List<String> titles = getTitle(numberOfAds);
        List<String> description = getDescriptions(10, numberOfAds);
        List<String> image = getImage(numberOfAds, imageService);

        return IntStream.range(0, numberOfAds)
                .mapToObj(i -> new AdEntity()
                        .setPrice(random.nextInt(maxPrice))
                        .setTitle(titles.get(i))
                        .setDescription(description.get(i))
                        .setImage(image.get(i))
                        .setAuthor(users.get(random.nextInt(users.size() - 1))))
                .toList();
    }

    public static CreateOrUpdateAd getUpdateForAd() {
        List<String> titles = getTitle(1);
        List<String> description = getDescriptions(10, 1);

        return new CreateOrUpdateAd()
                .setTitle(titles.getFirst())
                .setPrice(random.nextInt(maxPrice))
                .setDescription(description.getFirst());
    }

    public static List<String> getTitle(int quantity) {
        return Stream.generate(() -> fakerEn.commerce().productName())
                .filter(title -> title.length() >= titleMinSize && title.length() <= titleMaxSize)
                .distinct()
                .limit(quantity)
                .toList();
    }

    public static List<String> getDescriptions(int maxWords, int quantity) {
        return Stream.generate(() -> fakerEn.lorem().sentence(maxWords))
                .filter(desc -> desc.length() >= descriptionMinSize)
                .map(s -> s.substring(0, Math.min(s.length(), descriptionMaxSize)))
                .distinct()
                .limit(quantity)
                .toList();
    }

    public static List<String> getImage(int quantity, ImageService imageService) {
        return IntStream.range(0, quantity)
                .mapToObj(i -> {
                    byte[] imageBytes = generateRandomImageBytes();
                    ImageEntity imageEntity = imageService.uploadImage(imageBytes);
                    return ImageService.IMAGE_URL_PREFIX + imageEntity.getId();
                })
                .toList();
    }

    public static byte[] generateRandomImageBytes() {
        byte[] randomBytes = new byte[Math.max(1, random.nextInt(maxImageSize))];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    public static AdEntity getRandomAdFrom(List<AdEntity> ads) {
        return ads.get(random.nextInt(ads.size()));
    }

    public static int getRandomNonExistentId(JpaRepository<?, Integer> repository) {
        int nonExistentId;
        do {
            nonExistentId = random.nextInt(Integer.MAX_VALUE);
        } while (repository.existsById(nonExistentId));
        return nonExistentId;
    }

    public static List<CommentEntity> createComments(int numberOfComments, List<UserEntity> users, List<AdEntity> ads) {
        List<String> text = getCommentsText(10, numberOfComments);
        Date commentCreationDate = new Date();
        return IntStream.range(0, numberOfComments)
                .mapToObj(i -> new CommentEntity()
                        .setCreatedAt(commentCreationDate.getTime())
                        .setText(text.get(i))
                        .setAdEntity(ads.get(random.nextInt(ads.size() - 1)))
                        .setAuthor(users.get(random.nextInt(users.size() - 1))))
                .toList();
    }

    public static List<String> getCommentsText(int maxWords, int commentCount) {
        return Stream.generate(() -> fakerEn.lorem().sentence(maxWords))
                .filter(s -> s.length() >= commentMinSize)
                .map(s -> s.substring(0, Math.min(s.length(), commentMaxSize)))
                .distinct()
                .limit(commentCount)
                .toList();
    }

    public static String getRandomCommentDtoJson(ObjectMapper objectMapper) throws JsonProcessingException {
        String commentText = TestUtils.getCommentsText(10, 1).get(0);
        CreateOrUpdateComment commentRequest = new CreateOrUpdateComment().setText(commentText);
        return objectMapper.writeValueAsString(commentRequest);
    }

    public static CommentEntity getRandomCommentFrom(List<CommentEntity> comments) {
        return comments.get(new Random().nextInt(comments.size()));
    }

    public static Authentication getAuthenticationFor(UserEntity userEntity) {
        Authentication authentication = createAuthenticationTokenForUser(userEntity);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    public static Authentication getRandomUserAuthentication(List<UserEntity> predefinedUsers) {
        UserEntity user = TestUtils.getRandomUserFrom(predefinedUsers);
        Authentication authentication = createAuthenticationTokenForUser(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    public static Authentication createAuthenticationTokenForUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for authentication");
        }
        UserPrincipal userPrincipal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()
        );
    }

    public static <T> T findDistinctElement(List<T> list, T object) {
        if (list == null || object == null) {
            throw new IllegalArgumentException("List and object must not be null.");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List must not be empty.");
        }
        return list.stream()
                .filter(o -> !o.equals(object))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No distinct element found in the list."));
    }

    public static JSONObject createNewPasswordDto(String currentPassword, String newPassword) {
        JSONObject newPasswordDto = new JSONObject();
        newPasswordDto.put("currentPassword", currentPassword);
        newPasswordDto.put("newPassword", newPassword);
        return newPasswordDto;
    }

    public static JSONObject createNewUpdateUserDto(String firstName, String lastName, String phone) {
        JSONObject newPasswordDto = new JSONObject();
        newPasswordDto.put("firstName", firstName);
        newPasswordDto.put("lastName", lastName);
        newPasswordDto.put("phone", phone);
        return newPasswordDto;
    }
}