package ru.ads_online.controller;

import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ads_online.AdsOnlineApplication;
import ru.ads_online.controller.utils.TestUtils;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.repository.UserRepository;
import ru.ads_online.service.ImageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdsOnlineApplication.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureMockMvc
@RequiredArgsConstructor
public class UserControllerTests {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final static int NUMBER_OF_TEST_USERS = 10;
    private static List<UserEntity> users;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void beforeAll(@Autowired PasswordEncoder passwordEncoder, @Autowired UserRepository userRepository) {
        users = TestUtils.createUniqueUsers(NUMBER_OF_TEST_USERS, passwordEncoder);
        userRepository.saveAll(users);
    }

    @AfterAll
    static void afterAll(@Autowired ImageRepository imageRepository, @Autowired UserRepository userRepository) {
        imageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("Password update by authenticated user with USER role")
    @Test
    void setPassword_UserRole_Ok() throws Exception {
        List<String> passwords = TestUtils.getPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);

        UserEntity user = TestUtils.getRandomUserFrom(users);
        user.setPassword(passwordEncoder.encode(currentPassword))
                .setRole(Role.USER);
        userRepository.save(user);

        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JSONObject newData = new JSONObject();
        newData.put("currentPassword", currentPassword);
        newData.put("newPassword", newPassword);

        mockMvc.perform(post("/users/set_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk());

        String updatedPassword = userRepository.findById(user.getId()).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches(newPassword, updatedPassword));
    }

    @DisplayName("Password update by authenticated user with ADMIN role")
    @Test
    void setPassword_AdminRole_Ok() throws Exception {
        List<String> passwords = TestUtils.getPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);

        UserEntity user = TestUtils.getRandomUserFrom(users);
        user.setPassword(passwordEncoder.encode(currentPassword))
                .setRole(Role.ADMIN);
        userRepository.save(user);

        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JSONObject newData = new JSONObject();
        newData.put("currentPassword", currentPassword);
        newData.put("newPassword", newPassword);

        mockMvc.perform(post("/users/set_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk());

        String updatedPassword = userRepository.findById(user.getId()).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches(newPassword, updatedPassword));
    }

    @DisplayName("Password change by authorized user with wrong current password")
    @Test
    void setPassword_Forbidden() throws Exception {
        List<String> passwords = TestUtils.getPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);

        UserEntity user = TestUtils.getRandomUserFrom(users);
        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JSONObject newData = new JSONObject();
        newData.put("currentPassword", currentPassword);
        newData.put("newPassword", newPassword);

        mockMvc.perform(post("/users/set_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Password change by unauthorized user")
    @Test
    void setPassword_Unauthorized() throws Exception {
        List<String> passwords = TestUtils.getPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);

        JSONObject newData = new JSONObject();
        newData.put("currentPassword", currentPassword);
        newData.put("newPassword", newPassword);

        mockMvc.perform(post("/users/set_password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Obtain user information by an authorized user with the USER role")
    @Test
    void getData_AuthorizedUser() throws Exception {
        UserEntity user = TestUtils.getRandomUserFrom(users)
                .setRole(Role.USER);
        userRepository.save(user);
        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity authUser = userRepository.findByUsername(authentication.getName()).orElseThrow();

        mockMvc.perform(get("/users/me"))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(authUser.getId()),
                        jsonPath("$.username").value(authUser.getUsername()),
                        jsonPath("$.firstName").value(authUser.getFirstName()),
                        jsonPath("$.lastName").value(authUser.getLastName()),
                        jsonPath("$.phone").value(authUser.getPhone()),
                        jsonPath("$.role").value(authUser.getRole().toString()),
                        jsonPath("$.image").value(authUser.getImage()));
    }

    @DisplayName("Obtain user information by an authorized user with the ADMIN role")
    @Test
    void getData_AuthorizedAdmin() throws Exception {
        UserEntity user = TestUtils.getRandomUserFrom(users)
                .setRole(Role.ADMIN);
        userRepository.save(user);
        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity authUser = userRepository.findByUsername(authentication.getName()).orElseThrow();

        mockMvc.perform(get("/users/me"))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(authUser.getId()),
                        jsonPath("$.username").value(authUser.getUsername()),
                        jsonPath("$.firstName").value(authUser.getFirstName()),
                        jsonPath("$.lastName").value(authUser.getLastName()),
                        jsonPath("$.phone").value(authUser.getPhone()),
                        jsonPath("$.role").value(authUser.getRole().toString()),
                        jsonPath("$.image").value(authUser.getImage()));
    }

    @DisplayName("Obtain user information by a non-authorized user")
    @Test
    void getData_Unauthorized() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Update information of an authorized user")
    @Test
    void updateData_AuthorizedUser() throws Exception {
        UserEntity user = TestUtils.getRandomUserFrom(users);
        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String firstName = TestUtils.getFirstNames(1).get(0);
        String LastName = TestUtils.getLastNames(1).get(0);
        String phone = TestUtils.getPhones(1).get(0);

        JSONObject newData = new JSONObject();
        newData.put("firstName", firstName);
        newData.put("lastName", LastName);
        newData.put("phone", phone);

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpectAll(status().isOk(),
                        authenticated().withAuthenticationName(authentication.getName()),
                        jsonPath("$.firstName").value(firstName),
                        jsonPath("$.lastName").value(LastName),
                        jsonPath("$.phone").value(phone));
    }

    @DisplayName("Update information about by non-authorized user.")
    @Test
    void updateData_UnAuthorizedUser() throws Exception {
        String firstName = TestUtils.getFirstNames(1).get(0);
        String LastName = TestUtils.getLastNames(1).get(0);
        String phone = TestUtils.getPhones(1).get(0);

        JSONObject newData = new JSONObject();
        newData.put("firstName", firstName);
        newData.put("lastName", LastName);
        newData.put("phone", phone);

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Profile image update for an authorized user")
    @Test
    void updateImage_AuthorizedUser() throws Exception {
        UserEntity user = TestUtils.getRandomUserFrom(users);
        Authentication authentication = TestUtils.createAuthenticationFor(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockMultipartFile imageFile = new MockMultipartFile("image", "file1.png", MediaType.IMAGE_PNG_VALUE, "mockImageContent".getBytes());

        mockMvc.perform(multipart("/users/me/image")
                        .file(imageFile)
                        .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                }
                        )
                )
                .andExpect(status().isOk());

        UserEntity updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new AssertionError("User not found for ID: " + user.getId()));

        Assertions.assertNotNull(updatedUser.getImage());

        Integer imageId = Integer.parseInt(updatedUser.getImage().replaceAll(ImageService.IMAGE_URL_PREFIX, ""));
        byte[] imageBytesFromDb = imageRepository.findById(imageId)
                .orElseThrow(() -> new AssertionError("Image not found for ID: " + imageId))
                .getImage();

        assertArrayEquals(imageFile.getBytes(), imageBytesFromDb);
    }

    @DisplayName("Profile image update for an non-authorized user")
    @Test
    void updateImage_UnauthorizedUser() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("image", "file1.png", MediaType.IMAGE_PNG_VALUE, "mockImageContent".getBytes());
        mockMvc.perform(multipart("/users/me/image")
                        .file(imageFile)
                        .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                }
                        )
                )
                .andExpect(status().isUnauthorized());
    }
}