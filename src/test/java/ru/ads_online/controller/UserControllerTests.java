package ru.ads_online.controller;

import net.minidev.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ads_online.AdsOnlineApplication;
import ru.ads_online.controller.utils.TestUtils;
import ru.ads_online.pojo.dto.user.UpdateUser;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.repository.UserRepository;
import ru.ads_online.service.ImageService;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdsOnlineApplication.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@Testcontainers
@Transactional
@AutoConfigureMockMvc
public class UserControllerTests {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final static String URL_PATCH_PASSWORD = "/users/set_password";
    private final static String URL_GET_DATA = "/users/me";
    private final static String URL_UPDATE_DATA = "/users/me";
    private final static String URL_PATCH_IMAGE = "/users/me/image";
    private final static int NUMBER_OF_TEST_USERS = 10;
    private static UserEntity predefinedAdmin;
    private static List<UserEntity> predefinedUsers;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void beforeAll(@Autowired PasswordEncoder passwordEncoder,
                          @Autowired UserRepository userRepository) {
        predefinedUsers = TestUtils.createUniqueUsers(NUMBER_OF_TEST_USERS, passwordEncoder);
        userRepository.saveAll(predefinedUsers);

        predefinedAdmin = TestUtils.createAdmin(passwordEncoder);
        userRepository.save(predefinedAdmin);
    }

    @AfterAll
    static void afterAll(@Autowired ImageRepository imageRepository,
                         @Autowired UserRepository userRepository) {
        imageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("Authorized user's password update with invalid NewPassword dto")
    @ParameterizedTest(name = "{2}")
    @MethodSource("getInvalidNewPassword")
    void setPassword_shouldReturn400_whenInvalidNewPasswordDto(String currentPassword, String newPassword, String description) throws Exception {
        JSONObject newPasswordDto = TestUtils.createNewPasswordDto(currentPassword, newPassword);

        UserEntity user = TestUtils.getRandomUserFrom(predefinedUsers);
        userRepository.save(user.setPassword(passwordEncoder.encode(currentPassword)));
        TestUtils.getAuthenticationFor(user);

        mockMvc.perform(patch(URL_PATCH_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPasswordDto.toString()))
                .andExpectAll(
                        status().isBadRequest()
                );
        String updatedPassword = userRepository.findById(user.getId()).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches(currentPassword, updatedPassword));
    }

    private static Stream<Arguments> getInvalidNewPassword() {
        List<String> passwords = TestUtils.getDistinctPasswords(4);
        return Stream.of(
                Arguments.of(
                        String.valueOf('a').repeat(TestUtils.passwordMinSize - 1),
                        passwords.get(0),
                        "Current password too short"),
                Arguments.of(
                        String.valueOf('a').repeat(TestUtils.passwordMaxSize + 1),
                        passwords.get(1),
                        "Current password too long"),
                Arguments.of(
                        passwords.get(2),
                        String.valueOf('a').repeat(TestUtils.passwordMinSize - 1),
                        "New password too short"),
                Arguments.of(
                        passwords.get(3),
                        String.valueOf('a').repeat(TestUtils.passwordMaxSize + 1),
                        "New password too long")
        );
    }

    @DisplayName("Authorized user's password update")
    @Test
    void setPassword_shouldReturn200_whenUserPasswordSuccessfullyUpdated() throws Exception {
        List<String> passwords = TestUtils.getDistinctPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);
        JSONObject newPasswordDto = TestUtils.createNewPasswordDto(currentPassword, newPassword);

        UserEntity user = TestUtils.getRandomUserFrom(predefinedUsers);
        userRepository.save(user.setPassword(passwordEncoder.encode(currentPassword)));
        Authentication authentication = TestUtils.getAuthenticationFor(user);

        mockMvc.perform(patch(URL_PATCH_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPasswordDto.toString()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk()
                );
        String updatedPassword = userRepository.findById(user.getId()).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches(newPassword, updatedPassword));
    }

    @DisplayName("Authorized admin's password update")
    @Test
    void setPassword_shouldReturn200_whenAdminPasswordSuccessfullyUpdated() throws Exception {
        List<String> passwords = TestUtils.getDistinctPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);
        JSONObject newPasswordDto = TestUtils.createNewPasswordDto(currentPassword, newPassword);

        UserEntity admin = userRepository.save(predefinedAdmin.setPassword(passwordEncoder.encode(currentPassword)));
        Authentication authentication = TestUtils.getAuthenticationFor(admin);

        mockMvc.perform(patch(URL_PATCH_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPasswordDto.toString()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk());

        String updatedPassword = userRepository.findById(admin.getId()).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches(newPassword, updatedPassword));
    }

    @DisplayName("Authorized user's password update with wrong current password")
    @Test
    void setPassword_shouldReturn403_whenWrongCurrentPassword() throws Exception {
        List<String> passwords = TestUtils.getDistinctPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);
        JSONObject newPasswordDto = TestUtils.createNewPasswordDto(currentPassword, newPassword);

        TestUtils.getRandomUserAuthentication(predefinedUsers);

        mockMvc.perform(patch(URL_PATCH_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPasswordDto.toString()))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Password change by unauthorized user")
    @Test
    void setPassword_shouldReturn401_whenUnauthorizedUser() throws Exception {
        List<String> passwords = TestUtils.getDistinctPasswords(2);
        String currentPassword = passwords.get(0);
        String newPassword = passwords.get(1);
        JSONObject newPasswordDto = TestUtils.createNewPasswordDto(currentPassword, newPassword);

        mockMvc.perform(patch(URL_PATCH_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPasswordDto.toString()))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Fetch user information as an authorized user")
    @Test
    void getData_shouldReturnUser_whenRequestFromAuthorizedUser() throws Exception {
        UserEntity user = TestUtils.getRandomUserFrom(predefinedUsers);
        TestUtils.getAuthenticationFor(user);

        mockMvc.perform(get(URL_GET_DATA))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(user.getId()),
                        jsonPath("$.username").value(user.getUsername()),
                        jsonPath("$.firstName").value(user.getFirstName()),
                        jsonPath("$.lastName").value(user.getLastName()),
                        jsonPath("$.phone").value(user.getPhone()),
                        jsonPath("$.role").value(user.getRole().toString()),
                        jsonPath("$.image").value(user.getImage()));
    }

    @DisplayName("Fetch user information as an authorized admin")
    @Test
    void getData_shouldReturnUser_whenRequestFromAuthorizedAdmin() throws Exception {
        TestUtils.getAuthenticationFor(predefinedAdmin);

        mockMvc.perform(get(URL_GET_DATA))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(predefinedAdmin.getId()),
                        jsonPath("$.username").value(predefinedAdmin.getUsername()),
                        jsonPath("$.firstName").value(predefinedAdmin.getFirstName()),
                        jsonPath("$.lastName").value(predefinedAdmin.getLastName()),
                        jsonPath("$.phone").value(predefinedAdmin.getPhone()),
                        jsonPath("$.role").value(predefinedAdmin.getRole().toString()),
                        jsonPath("$.image").value(predefinedAdmin.getImage()));
    }

    @DisplayName("Fetch user information by a non-authorized user")
    @Test
    void getData_shouldReturn401_whenRequestFromAuthorizedUser() throws Exception {
        mockMvc.perform(get(URL_GET_DATA))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Authorized user's data update with invalid UpdateUser dto")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidUpdateUserDto")
    void updateData_shouldReturn400_whenInvalidUpdateUserDto(UpdateUser updateUser, String description) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);

        String firstName = updateUser.getFirstName();
        String LastName = updateUser.getLastName();
        String phone = updateUser.getPhone();
        JSONObject newUpdateUserDto = TestUtils.createNewUpdateUserDto(firstName, LastName, phone);

        mockMvc.perform(patch(URL_UPDATE_DATA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUpdateUserDto.toString()))
                .andExpectAll(
                        status().isBadRequest(),
                        authenticated().withAuthenticationName(authentication.getName())
                );
    }

    private static Stream<Arguments> getInvalidUpdateUserDto() {
        List<String> firstNames = TestUtils.getFirstNames(3);
        List<String> lastNames = TestUtils.getLastNames(3);
        List<String> phones = TestUtils.getPhones(4);

        return Stream.of(
                Arguments.of(
                        new UpdateUser()
                                .setFirstName(String.valueOf('a').repeat(TestUtils.firstNameMinSize - 1))
                                .setLastName(lastNames.get(0))
                                .setPhone(phones.get(0)),
                        "First name too short"),
                Arguments.of(
                        new UpdateUser()
                                .setFirstName(String.valueOf('a').repeat(TestUtils.firstNameMaxSize + 1))
                                .setLastName(lastNames.get(1))
                                .setPhone(phones.get(1)),
                        "First name too long"),
                Arguments.of(
                        new UpdateUser()
                                .setFirstName(firstNames.get(0))
                                .setLastName(String.valueOf('a').repeat(TestUtils.lastNameMinSize - 1))
                                .setPhone(phones.get(2)),
                        "Last name too short"),
                Arguments.of(
                        new UpdateUser()
                                .setFirstName(firstNames.get(1))
                                .setLastName(String.valueOf('a').repeat(TestUtils.lastNameMaxSize + 1))
                                .setPhone(phones.get(3)),
                        "Last name too long"),
                Arguments.of(
                        new UpdateUser()
                                .setFirstName(firstNames.get(2))
                                .setLastName(lastNames.get(2))
                                .setPhone("7(959)742-65-56"),
                        "Phone doesn't match the pattern")
        );
    }

    @DisplayName("Update information of an authorized user")
    @Test
    void updateData_shouldReturnUpdateUser_whenRequestFromAuthorizedUser() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);

        String firstName = TestUtils.getFirstNames(1).getFirst();
        String LastName = TestUtils.getLastNames(1).getFirst();
        String phone = TestUtils.getPhones(1).getFirst();
        JSONObject newUpdateUserDto = TestUtils.createNewUpdateUserDto(firstName, LastName, phone);

        mockMvc.perform(patch(URL_UPDATE_DATA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUpdateUserDto.toString()))
                .andExpectAll(status().isOk(),
                        authenticated().withAuthenticationName(authentication.getName()),
                        jsonPath("$.firstName").value(firstName),
                        jsonPath("$.lastName").value(LastName),
                        jsonPath("$.phone").value(phone));
    }

    @DisplayName("Update information about by non-authorized user")
    @Test
    void updateData_shouldReturn401_whenRequestFromUnauthorizedUser() throws Exception {
        String firstName = TestUtils.getFirstNames(1).getFirst();
        String LastName = TestUtils.getLastNames(1).getFirst();
        String phone = TestUtils.getPhones(1).getFirst();

        JSONObject newData = new JSONObject();
        newData.put("firstName", firstName);
        newData.put("lastName", LastName);
        newData.put("phone", phone);

        mockMvc.perform(patch(URL_PATCH_IMAGE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newData.toString()))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Profile image update for an authorized user")
    @Test
    void updateImage_shouldReturn200AndImage_whenImageSuccessfullyUpdated() throws Exception {
        UserEntity user = TestUtils.getRandomUserFrom(predefinedUsers);
        TestUtils.getAuthenticationFor(user);

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
    void updateImage_shouldReturn401_whenRequestFromUnauthorizedUser() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("image", "file1.png", MediaType.IMAGE_PNG_VALUE, "mockImageContent".getBytes());
        mockMvc.perform(multipart(URL_PATCH_IMAGE)
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