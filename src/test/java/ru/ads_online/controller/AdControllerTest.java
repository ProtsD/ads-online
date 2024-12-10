package ru.ads_online.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ads_online.AdsOnlineApplication;
import ru.ads_online.controller.utils.TestUtils;
import ru.ads_online.mapper.AdMapper;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;
import ru.ads_online.repository.CommentRepository;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.repository.UserRepository;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.ImageService;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.ads_online.controller.utils.TestUtils.createAuthenticationTokenForUser;
import static ru.ads_online.controller.utils.TestUtils.getRandomAdFrom;

@SpringBootTest(classes = AdsOnlineApplication.class)
@Testcontainers
@AutoConfigureMockMvc
public class AdControllerTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdRepository adRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ImageService imageService;
    @Autowired
    private DataSource dataSource;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:alpine");
    @Autowired
    private AdMapper adMapper;
    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final static int NUMBER_OF_TEST_ADS = 10;
    private final static int NUMBER_OF_TEST_USERS = 10;
    private final static int NUMBER_OF_TEST_COMMENTS = NUMBER_OF_TEST_ADS * 10;
    private static UserEntity predefinedAdmin;
    private static List<UserEntity> predefinedUsers;
    private static List<AdEntity> ads;
    private static List<CommentEntity> comments;
    private static AdRepository adRepositoryStat;


    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void beforeAll(@Autowired PasswordEncoder passwordEncoder,
                          @Autowired UserRepository userRepository,
                          @Autowired CommentRepository commentRepository,
                          @Autowired ImageService imageService,
                          @Autowired AdRepository adRepository) {
        predefinedUsers = TestUtils.createUniqueUsers(NUMBER_OF_TEST_USERS, passwordEncoder);
        userRepository.saveAll(predefinedUsers);

        predefinedAdmin = TestUtils.createAdmin(passwordEncoder);
        userRepository.save(predefinedAdmin);

        ads = TestUtils.createAds(NUMBER_OF_TEST_ADS, predefinedUsers, imageService);
        adRepository.saveAll(ads);

        comments = TestUtils.createComments(NUMBER_OF_TEST_COMMENTS, predefinedUsers, ads);
        commentRepository.saveAll(comments);

        adRepositoryStat = adRepository;
    }

    @AfterAll
    static void afterAll(@Autowired ImageRepository imageRepository,
                         @Autowired UserRepository userRepository,
                         @Autowired AdRepository adRepository,
                         @Autowired CommentRepository commentRepository) {
        commentRepository.deleteAll();
        adRepository.deleteAll();
        imageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("Fetch all ads as an unauthorized user")
    @Test
    void getAllAds_NoAuthorization_Ok() throws Exception {
        ads = adRepository.findAll();
        String expectedJSON = objectMapper.writeValueAsString(adMapper.toAds(ads));

        mockMvc.perform(get("/ads"))
                .andExpectAll(
                        unauthenticated(),
                        status().isOk(),
                        content().json(expectedJSON)
                );
    }

    @DisplayName("Fetch all ads as an authorized user")
    @Test
    void getAllAds_WithAuthorization_Ok() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        ads = adRepository.findAll();
        String expectedJSON = objectMapper.writeValueAsString(adMapper.toAds(ads));

        mockMvc.perform(get("/ads"))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk(),
                        content().json(expectedJSON)
                );
    }

    @DisplayName("Add ad as an unauthorized user")
    @Test
    void addAd_NoAuthorization_Unauthorized() throws Exception {
        CreateOrUpdateAd newAd = TestUtils.getAdUpdate();
        String newAdJson = objectMapper.writeValueAsString(newAd);
        long adNumberBeforeRequest = adRepository.count();

        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart("/ads").file(adProperties).file(adImage))
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );

        assertEquals(adNumberBeforeRequest, adRepository.count(),
                "Ad count should remain unchanged after an unauthorized request.");
    }

    @DisplayName("Add ad by authorised user")
    @Test
    void addAd_withAuthorization_Created() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        CreateOrUpdateAd newAd = TestUtils.getAdUpdate();
        String newAdJson = objectMapper.writeValueAsString(newAd);
        long adNumberBeforeRequest = adRepository.count();

        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());
        mockMvc.perform(multipart("/ads").file(adProperties).file(adImage))
                .andExpectAll(
                        status().isCreated(),
                        authenticated().withAuthenticationName(authentication.getName()),
                        jsonPath("$.author").value(((UserPrincipal) authentication.getPrincipal()).getUser().getId()),
                        jsonPath("$.image").exists(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.price").value(newAd.getPrice()),
                        jsonPath("$.title").value(newAd.getTitle())
                );

        assertEquals(adNumberBeforeRequest + 1, adRepository.count(),
                "Ad count should be increased by one after an authorized request.");
    }

    @DisplayName("Add ad with invalid data by authorised user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidArgumentsForCreateOrUpdate")
    void addAd_withAuthorization_InvalidData_BadRequest(CreateOrUpdateAd newAd, String reasonDescription) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        String newAdJson = objectMapper.writeValueAsString(newAd);
        long adNumberBeforeRequest = adRepository.count();

        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart("/ads").file(adProperties).file(adImage))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );

        assertEquals(adNumberBeforeRequest, adRepository.count(),
                "Ad count should remain unchanged after an authorized request.");
    }

    @DisplayName("Fetch ad as an unauthorised user")
    @Test
    void getAd_NoAuthorization_Unauthorized() throws Exception {
        int existedAdId = TestUtils.getRandomAdFrom(ads).getId();

        assertTrue(adRepository.findById(existedAdId).isPresent(),
                "Ad with this ID should exist for the test");

        mockMvc.perform(get("/ads/{id}", existedAdId))
                .andExpectAll(unauthenticated(),
                        status().isUnauthorized(),
                        content().string(""));
    }

    @DisplayName("Fetch non-existent ad as authorised user")
    @Test
    void getAd_withAuthorization_NotFound() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        int nonExistentId = TestUtils.getRandomNonExistentId(adRepository);

        mockMvc.perform(get("/ads/{id}", nonExistentId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
    }

    @DisplayName("Delete ad as unauthorized user.")
    @Test
    void deleteAd_withoutAuthorization_Unauthorized() throws Exception {
        AdEntity existedAd = TestUtils.getRandomAdFrom(ads);

        Optional<AdEntity> adEntity = adRepository.findById(existedAd.getId());
        assertTrue(adEntity.isPresent(), "The ad should exist before deletion attempt.");

        long countBefore = adRepository.count();

        mockMvc.perform(delete("/ads/{id}", existedAd.getId()))
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );

        assertEquals(countBefore, adRepository.count(),
                "The ad count should remain the same after an unauthorized delete attempt.");
    }

    @DisplayName("Delete ad by authorized owner")
    @Test
    void deleteAd_withAuthorization_ownAd_NoContent() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        Authentication authentication = TestUtils.getAuthenticationFor(existingAd.getAuthor());

        assertEquals(existingAd.getAuthor().getUsername(), authentication.getName(),
                "User must be the owner of the ad");

        long adsCountBefore = adRepository.count();

        mockMvc.perform(delete("/ads/{id}", existingAd.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNoContent()
                );
        assertEquals(adsCountBefore - 1, adRepository.count(),
                "Ad count should decrease by 1 after deletion.");
    }

    @DisplayName("An authorized user cannot delete someone else's ad")
    @Test
    void deleteAd_AuthorizedUserCannotDeleteSomeoneElseAd_Forbidden() throws Exception {
        UserEntity user = ads.stream()
                .map(AdEntity::getAuthor)
                .filter(author -> !author.getRole().equals(Role.ADMIN))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No suitable user found for testing"));
        Authentication authentication = TestUtils.getAuthenticationFor(user);

        AdEntity anyAd = ads.stream()
                .filter(ad -> !ad.getAuthor().equals(user))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No suitable ad found for testing"));

        long countBefore = adRepository.count();
        mockMvc.perform(delete("/ads/{id}", anyAd.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isForbidden()
                );
        assertEquals(countBefore, adRepository.count(),
                "Ad count should remain unchanged when deleting someone else's ad.");
    }

    @DisplayName("An authorized admin can delete someone else's ad")
    @Test
    void deleteAd_AuthorizedUserCanDeleteSomeoneElseAd_NoContent() throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        AdEntity adOfAnyOtherUser = ads.stream()
                .filter(ad -> !(ad.getAuthor().getId() == predefinedAdmin.getId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No suitable ad found for testing"));

        long adsCountBefore = adRepository.count();
        mockMvc.perform(delete("/ads/{id}", adOfAnyOtherUser.getId()))
                .andExpect(authenticated().withAuthenticationName(authentication.getName()).withRoles("ADMIN"))
                .andExpect(status().isNoContent());
        assertEquals(adsCountBefore - 1, adRepository.count(),
                "Ad count should decrease by 1 when an admin deletes someone else's ad");
    }

    @DisplayName("Delete ad with invalid data by authorised user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("provideInvalidDataForDeleteAd")
    void deleteAd_withAuthorization_InvalidData_BadRequest(int adId, String reasonDescription) throws Exception {
        TestUtils.getRandomUserAuthentication(predefinedUsers);
        long adNumberBeforeRequest = adRepository.count();

        MvcResult result = mockMvc.perform(delete("/ads/{id}", adId)).
                andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.NOT_FOUND.value(),
                "Status should be either 400 (Bad Request) or 404 (Not Found)");

        assertEquals(adNumberBeforeRequest, adRepository.count(),
                "Ad count should remain unchanged after an authorized request.");
    }

    static Stream<Arguments> provideInvalidDataForDeleteAd() {
        return Stream.of(
                Arguments.of(-1, "ID is -1"),
                Arguments.of(TestUtils.getRandomNonExistentId(adRepositoryStat), "ID does not exist")
        );
    }

    @DisplayName("Update ad by unauthorised user.")
    @Test
    void updateAd_NoAuthorization_Unauthorized() throws Exception {
        CreateOrUpdateAd updateAd = TestUtils.getAdUpdate();
        int existingAdId = TestUtils.getRandomAdFrom(ads).getId();

        mockMvc.perform(patch("/ads/{id}", existingAdId)
                        .content(objectMapper.writeValueAsString(updateAd))
                )
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );
    }

    @DisplayName("Update ad by authorized owner")
    @Test
    void updateAd_withAuthorization_ownAd_Ok() throws Exception {
        CreateOrUpdateAd updateAd = TestUtils.getAdUpdate();
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        Ad expectedAd = new Ad()
                .setAuthor(existingAd.getAuthor().getId())
                .setImage(existingAd.getImage())
                .setId(existingAd.getId())
                .setPrice(updateAd.getPrice())
                .setTitle(updateAd.getTitle());
        Authentication authentication = TestUtils.getAuthenticationFor(existingAd.getAuthor());

        String expectedJson = objectMapper.writeValueAsString(expectedAd);
        mockMvc.perform(
                        patch("/ads/{id}", existingAd.getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(updateAd))
                )
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        content().json(expectedJson),
                        status().isOk()
                );
    }

    @DisplayName("Update another user's ad as an authorized user")
    @Test
    void updateAd_withAuthorization_someoneElseAd_Forbidden() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        CreateOrUpdateAd updateAd = TestUtils.getAdUpdate();

        UserEntity user;
        do {
            user = TestUtils.getRandomUserFrom(predefinedUsers);
        } while (existingAd.getAuthor().equals(user) || user.getRole().equals(Role.ADMIN));
        TestUtils.getAuthenticationFor(user);

        mockMvc.perform(
                        patch("/ads/{id}", existingAd.getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(updateAd))
                )
                .andExpect(status().isForbidden());
    }

    @DisplayName("Update someone else's ad by authorized admin.")
    @Test
    void updateAd_AuthorizedUser_someoneElseAd_Ok() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        CreateOrUpdateAd updateAd = TestUtils.getAdUpdate();
        TestUtils.getAuthenticationFor(predefinedAdmin);

        Ad expectedAd = new Ad()
                .setAuthor(existingAd.getAuthor().getId())
                .setImage(existingAd.getImage())
                .setId(existingAd.getId())
                .setPrice(updateAd.getPrice())
                .setTitle(updateAd.getTitle());

        String expectedJson = objectMapper.writeValueAsString(expectedAd);

        mockMvc.perform(
                        patch("/ads/{id}", existingAd.getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(updateAd))
                )
                .andExpect(content().json(expectedJson))
                .andExpect(status().isOk());
    }

    @DisplayName("Update nonexistent ad by authorized user")
    @Test
    void updateAd_withAuthorization_nonExistentAd_NotFound() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        int nonExistingAdId = TestUtils.getRandomNonExistentId(adRepository);
        CreateOrUpdateAd updateAd = TestUtils.getAdUpdate();

        mockMvc.perform(
                        patch("/ads/{id}", nonExistingAdId)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(updateAd))
                )
                .andExpect(authenticated().withAuthenticationName(authentication.getName()))
                .andExpect(status().isNotFound());
    }

    @DisplayName("Update ad with wrong data by authorized owner")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidArgumentsForCreateOrUpdate")
    void updateAd_withAuthorization_InvalidData_BadRequest(CreateOrUpdateAd updateAd, String reasonDescription) throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        Authentication authentication = TestUtils.getAuthenticationFor(existingAd.getAuthor());

        mockMvc.perform(
                        patch("/ads/{id}", existingAd.getId())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(updateAd))
                )
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    static Stream<Arguments> getInvalidArgumentsForCreateOrUpdate() {
        return Stream.of(
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle(null)
                        .setPrice(100)
                        .setDescription("Valid description"), "Title is null"),
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle("Ad")
                        .setPrice(100)
                        .setDescription("Valid description"), "Title is too short"),
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle("Too long title, Too long title!!!")
                        .setPrice(100)
                        .setDescription("Valid description"), "Title is too long"),
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle("Valid Title")
                        .setPrice(-1)
                        .setDescription("Valid Title"), "Price is negative"),
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle("Valid Title")
                        .setPrice(10000001)
                        .setDescription("Valid Title"), "Price exceeds maximum"),
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle("Valid Title")
                        .setPrice(100)
                        .setDescription(null), "Description is null"),
                Arguments.of(new CreateOrUpdateAd()
                        .setTitle("Valid Title")
                        .setPrice(100)
                        .setDescription("Short"), "Description is too short"),
                Arguments.of(new CreateOrUpdateAd()
                                .setTitle("Valid Title")
                                .setPrice(100)
                                .setDescription("Too long description. Too long description. Too long description."),
                        "Description is too long")
        );
    }

    @DisplayName("Fetch ads by authorized user")
    @Test
    void getAds_withAuthorization_Ok() throws Exception {
        AdEntity randomAd = getRandomAdFrom(ads);
        UserEntity userOfRandomAd = randomAd.getAuthor();
        Authentication authentication = TestUtils.getAuthenticationFor(userOfRandomAd);


        List<AdEntity> userAds = ads.stream()
                .filter(ad -> Objects.equals(ad.getAuthor(), userOfRandomAd))
                .collect(Collectors.toList());
        String expectedJson = objectMapper.writeValueAsString(adMapper.toAds(userAds));

        mockMvc.perform(get("/ads/me"))
                .andExpect(authenticated().withAuthenticationName(authentication.getName()))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @DisplayName("Fetch ads of unauthorised user")
    @Test
    void getAds_NoAuthorization_Unauthorized() throws Exception {
        mockMvc.perform(get("/ads/me"))
                .andExpect(unauthenticated())
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("An Ad picture update by unauthorized user")
    @Test
    void updateAdImage_withoutAuthorization_thenUnauthorized() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(
                        multipart("/ads/{id}/image", existingAd.getId())
                                .file(adImage)
                                .with(
                                        request -> {
                                            request.setMethod("PATCH");
                                            return request;
                                        }
                                )
                )
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );
    }

    @DisplayName("Update the image of someone else's ad by an authorized user")
    @Test
    void updateAdImage_withAuthorization_someoneElseAd_thenForbidden() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        UserEntity userEntity = predefinedUsers.stream()
                .filter(user -> user.getId() != existingAd.getAuthor().getId())
                .filter(user -> user.getRole() != Role.ADMIN)
                .findAny()
                .orElseThrow();
        Authentication authentication = createAuthenticationTokenForUser(userEntity);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(
                        multipart("/ads/{id}/image", existingAd.getId())
                                .file(imageFile)
                                .with(
                                        request -> {
                                            request.setMethod("PATCH");
                                            return request;
                                        }
                                )
                )
                .andExpect(authenticated().withAuthenticationName(authentication.getName()))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Update the image of a nonexistent ad by an authorized admin")
    @Test
    void updateAdImage_withAuthorization_nonExistentAd_withAdminRole_thenNotFound() throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        int nonExistentAdId = TestUtils.getRandomNonExistentId(adRepository);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(
                        multipart("/ads/{id}/image", nonExistentAdId)
                                .file(imageFile)
                                .with(
                                        request -> {
                                            request.setMethod("PATCH");
                                            return request;
                                        }
                                )
                )
                .andExpect(authenticated().withAuthenticationName(authentication.getName()).withRoles("ADMIN"))
                .andExpect(status().isNotFound());
    }
}