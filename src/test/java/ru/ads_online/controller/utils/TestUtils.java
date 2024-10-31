package ru.ads_online.controller.utils;

import com.github.javafaker.Faker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.security.DatabaseUserDetails;

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
    static final Faker fakerRu = new Faker(Locale.forLanguageTag("ru-RU"));
    static final Faker fakerEn = new Faker(Locale.forLanguageTag("en-US"));
    static final Random random = new Random();

    public static List<UserEntity> createUniqueUsers(int numberOfUsers, PasswordEncoder passwordEncoder) {
        List<String> uniqueEmails = Stream
                .generate(() -> fakerEn.internet().emailAddress())
                .distinct()
                .filter(email -> email.length() >= usernameMinSize && email.length() <= usernameMaxSize)
                .limit(numberOfUsers)
                .toList();

        List<String> passwords = getPasswords(numberOfUsers);
        List<String> firstNames = getFirstNames(numberOfUsers);
        List<String> lastNames = getLastNames(numberOfUsers);
        List<String> uniquePhones = getPhones(numberOfUsers);

        return IntStream.range(0, numberOfUsers)
                .mapToObj(i -> new UserEntity()
                        .setUsername(uniqueEmails.get(i))
                        .setPassword(passwordEncoder.encode(passwords.get(i)))
                        .setFirstName(firstNames.get(i))
                        .setLastName(lastNames.get(i))
                        .setPhone(uniquePhones.get(i))
                        .setRole(Role.USER)
                        .setImage(null))
                .toList();
    }

    public static UserEntity getRandomUserFrom(List<UserEntity> users) {
        return users.get(random.nextInt(users.size()));
    }

    public static List<String> getPasswords(int quantity) {
        return Stream.generate(() -> fakerRu.internet().password(passwordMinSize, passwordMaxSize))
                .limit(quantity)
                .toList();
    }

    public static List<String> getFirstNames(int Quantity) {
        return Stream.generate(() -> fakerRu.name().firstName())
                .filter(firstName -> firstName.length() >= firstNameMinSize && firstName.length() <= firstNameMaxSize)
                .limit(Quantity)
                .toList();
    }

    public static List<String> getLastNames(int Quantity) {
        return Stream.generate(() -> fakerRu.name().lastName())
                .filter(lastName -> lastName.length() >= lastNameMinSize && lastName.length() <= lastNameMaxSize)
                .limit(Quantity)
                .toList();
    }

    public static List<String> getPhones(int Quantity) {
        return Stream.generate(() -> fakerRu.phoneNumber().phoneNumber())
                .distinct()
                .limit(Quantity)
                .toList();
    }

    public static Authentication createAuthenticationFor(UserEntity user) {
        return Optional.ofNullable(user)
                .map(DatabaseUserDetails::new)
                .map(securityUserPrincipal -> new UsernamePasswordAuthenticationToken(
                        securityUserPrincipal,
                        null,
                        securityUserPrincipal.getAuthorities()
                ))
                .orElse(null);
    }
}