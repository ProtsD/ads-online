package ru.ads_online.controller.utils;

import com.github.javafaker.Faker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.ImageEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.ImageService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestUtils {
    static final int usernameMinSize = 4, usernameMaxSize = 32;
    static final int passwordMinSize = 8, passwordMaxSize = 16;
    static final int firstNameMinSize = 2, firstNameMaxSize = 16;
    static final int lastNameMinSize = 2, lastNameMaxSize = 16;
    static final int titleMinSize = 4, titleMaxSize = 32;
    static final int descriptionMinSize = 8, descriptionMaxSize = 64;
    static final int maxPrice = 10000000;
    static final int maxImageSize = 10485760;
    static final Faker fakerRu = new Faker(Locale.forLanguageTag("ru-RU"));
    static final Faker fakerEn = new Faker(Locale.forLanguageTag("en-US"));
    static final Random random = new Random();

    public static List<UserEntity> createUniqueUsers(int numberOfUsers, PasswordEncoder passwordEncoder) {
        List<String> emails = getEmails(numberOfUsers);
        List<String> passwords = getPasswords(numberOfUsers);
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

    public static UserEntity createAdmin(List<UserEntity> users) {
        int randomUser = new Random().nextInt(users.size());
        users.get(randomUser).setRole(Role.ADMIN);
        return users.get(randomUser);
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

    public static List<String> getPasswords(int quantity) {
        return Stream.generate(() -> fakerRu.internet().password(passwordMinSize, passwordMaxSize))
                .limit(quantity)
                .toList();
    }

    public static List<String> getFirstNames(int quantity) {
        return Stream.generate(() -> fakerRu.name().firstName())
                .filter(firstName -> firstName.length() >= firstNameMinSize)
                .map(firstName -> firstName.substring(0, Math.min(firstName.length(), firstNameMaxSize)))
                .limit(quantity)
                .toList();
    }

    public static List<String> getLastNames(int Quantity) {
        return Stream.generate(() -> fakerRu.name().lastName())
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

    public static Authentication createAuthenticationFor(UserEntity user) {
        return Optional.ofNullable(user)
                .map(UserPrincipal::new)
                .map(securityUserPrincipal -> new UsernamePasswordAuthenticationToken(
                        securityUserPrincipal,
                        null,
                        securityUserPrincipal.getAuthorities()
                ))
                .orElse(null);
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
                        .setAuthor(users.get(random.nextInt(users.size()))))
                .toList();
    }

    public static CreateOrUpdateAd getAdUpdate() {
        List<String> titles = getTitle(1);
        List<String> description = getDescriptions(10, 1);

        return new CreateOrUpdateAd()
                .setTitle(titles.get(0))
                .setPrice(random.nextInt(maxPrice))
                .setDescription(description.get(0));
    }

    public static List<String> getTitle(int quantity) {
        return Stream.generate(() -> fakerRu.commerce().productName())
                .filter(title -> title.length() >= titleMinSize && title.length() <= titleMaxSize)
                .distinct()
                .limit(quantity)
                .toList();
    }

    public static List<String> getDescriptions(int maxWords, int quantity) {
        return Stream.generate(() -> fakerRu.lorem().sentence(maxWords))
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

    public static Authentication createAuthenticationTokenForUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for authentication");
        }
        UserPrincipal userPrincipal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()
        );
    }

    public static AdEntity getRandomAdFrom(List<AdEntity> ads) {
        return ads.get(random.nextInt(ads.size()));
    }

    public static int getRandomNonExistentAd(List<AdEntity> ads) {
        int randomNonExistentId;
        List<Integer> allIds = ads.stream().map(AdEntity::getId).toList();
        do {
            randomNonExistentId = random.nextInt(Integer.MAX_VALUE);
        } while (allIds.contains(randomNonExistentId));

        return randomNonExistentId;
    }
}