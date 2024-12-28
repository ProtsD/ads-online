package ru.ads_online.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ads_online.AdsOnlineApplication;
import ru.ads_online.controller.utils.TestUtils;
import ru.ads_online.mapper.AdMapper;
import ru.ads_online.pojo.dto.ad.Ad;
import ru.ads_online.pojo.dto.ad.CreateOrUpdateAd;
import ru.ads_online.pojo.dto.ad.ExtendedAd;
import ru.ads_online.pojo.dto.user.Role;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;
import ru.ads_online.repository.CommentRepository;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.repository.UserRepository;
import ru.ads_online.security.UserPrincipal;
import ru.ads_online.service.AdService;
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
import static ru.ads_online.controller.utils.TestUtils.getAuthenticationFor;
import static ru.ads_online.controller.utils.TestUtils.getRandomAdFrom;

@SpringBootTest(classes = AdsOnlineApplication.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@Testcontainers
@Transactional
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
    private AdService adService;
    @Autowired
    private DataSource dataSource;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:alpine");
    @Autowired
    private AdMapper adMapper;
    @Autowired
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final static int NUMBER_OF_TEST_ADS = 10;
    private final static int NUMBER_OF_TEST_USERS = 10;
    private final static int NUMBER_OF_TEST_COMMENTS = NUMBER_OF_TEST_ADS * 10;
    private final static String URL_GET_ALL_ADS = "/ads";
    private final static String URL_ADD_AD = "/ads";
    private final static String URL_GET_AD = "/ads/{id}";
    private final static String URL_DELETE_AD = "/ads/{id}";
    private final static String URL_UPDATE_AD = "/ads/{id}";
    private final static String URL_GET_ADS = "/ads/me";
    private final static String URL_UPDATE_AD_IMAGE = "/ads/{id}/image";
    private final static String REMAIN_COMMENT_COUNT = "Comment count should remain unchanged";
    private static UserEntity predefinedAdmin;
    private static List<UserEntity> predefinedUsers;
    private static List<AdEntity> ads;
    private static List<CommentEntity> comments;

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

    @DisplayName("Fetch all ads as an authorized user")
    @Test
    void getAllAds_shouldReturnAds_whenRequestFromAuthorizedUser() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        ads = adRepository.findAll();
        String expectedJSON = objectMapper.writeValueAsString(adMapper.toAds(ads));

        mockMvc.perform(get(URL_GET_ALL_ADS))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk(),
                        content().json(expectedJSON)
                );
    }

    @DisplayName("Fetch all ads as an unauthorized user")
    @Test
    void getAllAds_shouldReturnAds_whenRequestFromUnauthorizedUser() throws Exception {
        ads = adRepository.findAll();
        String expectedJSON = objectMapper.writeValueAsString(adMapper.toAds(ads));

        mockMvc.perform(get(URL_GET_ALL_ADS))
                .andExpectAll(
                        unauthenticated(),
                        status().isOk(),
                        content().json(expectedJSON)
                );
    }

    @DisplayName("Add ad with null data as an unauthorized user")
    @ParameterizedTest(name = "{4}")
    @MethodSource("getNullValuesForAddAd")
    void addAd_shouldReturn400Or401_whenNullValues(UserEntity userEntity,
                                                   MockMultipartFile properties,
                                                   MockMultipartFile image,
                                                   int expectedStatus,
                                                   String reasonDescription) throws Exception {
        MockMultipartHttpServletRequestBuilder requestBuilder = multipart(URL_ADD_AD);

        if (userEntity != null) TestUtils.getAuthenticationFor(userEntity);
        if (properties != null) requestBuilder.file(properties);
        if (image != null) requestBuilder.file(image);

        mockMvc.perform(requestBuilder)
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> getNullValuesForAddAd() throws JsonProcessingException {
        UserEntity userEntity = TestUtils.getRandomUserFrom(predefinedUsers);

        CreateOrUpdateAd newAd = TestUtils.getUpdateForAd();
        MockMultipartFile mockProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsString(newAd).getBytes());

        MockMultipartFile mockImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        return Stream.of(
                Arguments.of(null, mockProperties, mockImage, 401, "User is null"),
                Arguments.of(userEntity, null, mockImage, 400, "Ad body is null"),
                Arguments.of(userEntity, mockProperties, null, 400, "Image is null")
        );
    }

    @DisplayName("Add ad with invalid image as an unauthorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidImages")
    void addAd_shouldReturn400_whenInvalidImage(MockMultipartFile image, String description) throws Exception {
        TestUtils.getRandomUserAuthentication(predefinedUsers);

        CreateOrUpdateAd newAd = TestUtils.getUpdateForAd();
        String newAdJson = objectMapper.writeValueAsString(newAd);
        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());

        MockMultipartHttpServletRequestBuilder requestBuilder = multipart(URL_ADD_AD);
        requestBuilder.file(adProperties);
        if (image != null) requestBuilder.file(image);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }

    static Stream<Arguments> getInvalidImages() {
        MockMultipartFile mockImageWithWrongMime = new MockMultipartFile(
                "image", "image.png",
                MediaType.TEXT_PLAIN_VALUE, TestUtils.generateRandomImageBytes());

        MockMultipartFile mockImageExceedingMaxSize = new MockMultipartFile(
                "image", "largeImage.png",
                MediaType.IMAGE_PNG_VALUE, new byte[10_485_761]);

        return Stream.of(
                Arguments.of(null, "Image is null"),
                Arguments.of(mockImageWithWrongMime, "Invalid MIME type"),
                Arguments.of(mockImageExceedingMaxSize, "Image exceeds max size")
        );
    }

    @DisplayName("Add ad with invalid data by authorised user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidArgumentsForCreateOrUpdate")
    void addAd_shouldTReturn400_whenInvalidCreateOrUpdateAd(CreateOrUpdateAd newAd, String reasonDescription) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        String newAdJson = objectMapper.writeValueAsString(newAd);
        long adNumberBeforeRequest = adRepository.count();

        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_ADD_AD).file(adProperties).file(adImage))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );

        assertEquals(adNumberBeforeRequest, adRepository.count(),
                "Ad count should remain unchanged after an authorized request.");
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

    @DisplayName("Add ad by authorised user")
    @Test
    void addAd_shouldTReturn201AndAd_whenAdSuccessfullyCreated() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        CreateOrUpdateAd newAd = TestUtils.getUpdateForAd();
        String newAdJson = objectMapper.writeValueAsString(newAd);
        long adNumberBeforeRequest = adRepository.count();

        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());
        mockMvc.perform(multipart(URL_ADD_AD).file(adProperties).file(adImage))
                .andExpectAll(
                        status().isCreated(),
                        authenticated().withAuthenticationName(authentication.getName()),
                        jsonPath("$.author").value(((UserPrincipal) authentication.getPrincipal()).getUser().getId()),
                        jsonPath("$.image").exists(),
                        jsonPath("$.pk").exists(),
                        jsonPath("$.price").value(newAd.getPrice()),
                        jsonPath("$.title").value(newAd.getTitle())
                );

        assertEquals(adNumberBeforeRequest + 1, adRepository.count(),
                "Ad count should be increased by one after an authorized request.");
    }

    @DisplayName("Add ad as an unauthorized user")
    @Test
    void addAd_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        CreateOrUpdateAd newAd = TestUtils.getUpdateForAd();
        String newAdJson = objectMapper.writeValueAsString(newAd);
        long adNumberBeforeRequest = adRepository.count();

        MockMultipartFile adProperties = new MockMultipartFile(
                "properties", "adProperties.json",
                MediaType.APPLICATION_JSON_VALUE, newAdJson.getBytes());
        MockMultipartFile adImage = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_ADD_AD).file(adProperties).file(adImage))
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );

        assertEquals(adNumberBeforeRequest, adRepository.count(),
                "Ad count should remain unchanged after an unauthorized request.");
    }

    @DisplayName("Fetch ad as authorised user")
    @Test
    void getAd_shouldTReturnExtendedAd_whenAdIdExist() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);

        AdEntity ad = TestUtils.getRandomAdFrom(ads);
        ExtendedAd extendedAd = adMapper.toExtendedAd(ad);
        String extendedAdJson = objectMapper.writeValueAsString(extendedAd);

        mockMvc.perform(get(URL_GET_AD, ad.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        content().json(extendedAdJson),
                        status().isOk()
                );
    }

    @DisplayName("Fetch ad with invalid id as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void getAd_shouldTReturn400_whenInvalidId(String id, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);

        mockMvc.perform(get(URL_GET_AD, id))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Fetch ad as an unauthorised user")
    @Test
    void getAd_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        int existedAdId = TestUtils.getRandomAdFrom(ads).getId();

        assertTrue(adRepository.findById(existedAdId).isPresent(),
                String.format("Ad with id=%d should exist for the test", existedAdId));

        mockMvc.perform(get(URL_GET_AD, existedAdId))
                .andExpectAll(unauthenticated(),
                        status().isUnauthorized(),
                        content().string(""));
    }

    @DisplayName("Fetch non-existent ad as authorised user")
    @Test
    void getAd_shouldTReturn404_whenAdIdDoesNotExist() throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        int nonExistentId = TestUtils.getRandomNonExistentId(adRepository);

        mockMvc.perform(get(URL_GET_AD, nonExistentId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
    }

    @DisplayName("Delete ad by authorized owner")
    @Test
    void deleteAd_shouldTReturn204_whenAdSuccessfullyDeletedByUser() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        Authentication authentication = TestUtils.getAuthenticationFor(existingAd.getAuthor());

        assertEquals(existingAd.getAuthor().getUsername(), authentication.getName(),
                "User must be the owner of the ad");

        long adsCountBefore = adRepository.count();
        long commentsCountBefore = commentRepository.count();
        long currentAdCommentsCount = commentRepository.findAllByAdId(existingAd.getId()).size();

        mockMvc.perform(delete(URL_DELETE_AD, existingAd.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNoContent()
                );
        assertEquals(adsCountBefore - 1, adRepository.count(),
                "Ad count should decrease by 1 after deletion.");
        assertEquals(commentsCountBefore - currentAdCommentsCount, commentRepository.count(),
                "Ad count should decrease by 1 after deletion.");
    }

    @DisplayName("An authorized admin can delete someone else's ad")
    @Test
    void deleteAd_shouldTReturn204_whenAdSuccessfullyDeletedByAdmin() throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        AdEntity adOfAnyOtherUser = ads.stream()
                .filter(ad -> !(ad.getAuthor().getId() == predefinedAdmin.getId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No suitable ad found for testing"));

        long adsCountBefore = adRepository.count();
        long commentsCountBefore = commentRepository.count();
        long currentAdCommentsCount = commentRepository.findAllByAdId(adOfAnyOtherUser.getId()).size();

        mockMvc.perform(delete(URL_DELETE_AD, adOfAnyOtherUser.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()).withRoles("ADMIN"),
                        status().isNoContent()
                );
        assertEquals(adsCountBefore - 1, adRepository.count(),
                "Ad count should decrease by 1 when an admin deletes someone else's ad");
        assertEquals(commentsCountBefore - currentAdCommentsCount, commentRepository.count(),
                "Ad count should decrease by 1 when an admin deletes someone else's ad");
    }

    @DisplayName("Delete ad with invalid data as authorised admin")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void deleteAd_shouldTReturn400_whenInvalidId(String id, String caseName) throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        mockMvc.perform(delete(URL_DELETE_AD, id))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Delete non-existent ad as authorised admin")
    @Test
    void deleteAd_shouldTReturn404_whenAdIdDoesNotExist() throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        int nonExistentId = TestUtils.getRandomNonExistentId(adRepository);

        mockMvc.perform(delete(URL_DELETE_AD, nonExistentId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
    }

    @DisplayName("Delete ad as unauthorized user")
    @Test
    void deleteAd_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        AdEntity existedAd = TestUtils.getRandomAdFrom(ads);

        Optional<AdEntity> adEntity = adRepository.findById(existedAd.getId());
        assertTrue(adEntity.isPresent(), "The ad should exist before deletion attempt.");

        long countBefore = adRepository.count();

        mockMvc.perform(delete(URL_DELETE_AD, existedAd.getId()))
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );

        assertEquals(countBefore, adRepository.count(),
                "The ad count should remain the same after an unauthorized delete attempt.");
    }

    @DisplayName("An authorized user cannot delete someone else's ad")
    @Test
    void deleteAd_shouldTReturn403_whenAuthorizedUserIsNotAdAuthor() throws Exception {
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
        mockMvc.perform(delete(URL_DELETE_AD, anyAd.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isForbidden()
                );
        assertEquals(countBefore, adRepository.count(),
                "Ad count should remain unchanged when deleting someone else's ad.");
    }

    @DisplayName("Update ad by authorized owner")
    @Test
    void updateAd_shouldReturnAd_whenAdSuccessfullyUpdatedByAuthor() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        CreateOrUpdateAd updateForAd = TestUtils.getUpdateForAd();
        Ad expectedAd = adMapper.toAd(existingAd)
                .setPrice(updateForAd.getPrice())
                .setTitle(updateForAd.getTitle());
        Authentication authentication = TestUtils.getAuthenticationFor(existingAd.getAuthor());
        String expectedAdJson = objectMapper.writeValueAsString(expectedAd);

        mockMvc.perform(patch(URL_UPDATE_AD, existingAd.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(updateForAd)))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        content().json(expectedAdJson),
                        status().isOk()
                );
    }

    @DisplayName("Update someone else's ad by authorized admin")
    @Test
    void updateAd_shouldReturnAd_whenAdSuccessfullyUpdatedByAdmin() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        CreateOrUpdateAd updateForAd = TestUtils.getUpdateForAd();
        Ad expectedAd = adMapper.toAd(existingAd)
                .setPrice(updateForAd.getPrice())
                .setTitle(updateForAd.getTitle());
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        String expectedJson = objectMapper.writeValueAsString(expectedAd);

        mockMvc.perform(patch(URL_UPDATE_AD, existingAd.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(updateForAd)))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        content().json(expectedJson),
                        status().isOk()
                );
    }

    @DisplayName("Update ad with wrong data as authorized owner")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidArgumentsForCreateOrUpdate")
    void updateAd_shouldTReturn400_whenInvalidCreateOrUpdateAd(CreateOrUpdateAd updateForAd, String reasonDescription) throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        Authentication authentication = TestUtils.getAuthenticationFor(existingAd.getAuthor());

        mockMvc.perform(patch(URL_UPDATE_AD, existingAd.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(updateForAd)))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Update ad with invalid data as authorised admin")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void updateAd_shouldTReturn400_whenInvalidId(String id, String caseName) throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        mockMvc.perform(patch(URL_UPDATE_AD, id))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Update non-existent ad by authorized admin")
    @Test
    void updateAd_shouldTReturn404_whenAdIdDoesNotExist() throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        int nonExistingAdId = TestUtils.getRandomNonExistentId(adRepository);
        CreateOrUpdateAd updateForAd = TestUtils.getUpdateForAd();

        mockMvc.perform(patch(URL_UPDATE_AD, nonExistingAdId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(updateForAd)))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
    }

    @DisplayName("Update ad as unauthorised user")
    @Test
    void updateAd_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        CreateOrUpdateAd updateForAd = TestUtils.getUpdateForAd();
        int existingAdId = TestUtils.getRandomAdFrom(ads).getId();

        mockMvc.perform(patch(URL_UPDATE_AD, existingAdId)
                        .content(objectMapper.writeValueAsString(updateForAd))
                )
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );
    }

    @DisplayName("Update another user's ad as an authorized user")
    @Test
    void updateAd_shouldTReturn403_whenAuthorizedUserIsNotAdAuthor() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        CreateOrUpdateAd updateForAd = TestUtils.getUpdateForAd();

        UserEntity userEntity = predefinedUsers.stream()
                .filter(user -> user.getId() != existingAd.getAuthor().getId())
                .findAny()
                .orElseThrow();
        TestUtils.getAuthenticationFor(userEntity);

        mockMvc.perform(patch(URL_UPDATE_AD, existingAd.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(updateForAd)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("Fetch all ads of authorized user")
    @Test
    void getAds_shouldReturnAds_whenExists() throws Exception {
        AdEntity existingAd = getRandomAdFrom(ads);
        UserEntity user = existingAd.getAuthor();
        Authentication authentication = TestUtils.getAuthenticationFor(user);


        List<AdEntity> userAds = ads.stream()
                .filter(ad -> Objects.equals(ad.getAuthor(), user))
                .collect(Collectors.toList());
        String expectedJson = objectMapper.writeValueAsString(adMapper.toAds(userAds));

        mockMvc.perform(get(URL_GET_ADS))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk(),
                        content().json(expectedJson)
                );
    }

    @DisplayName("Fetch ads of unauthorised user")
    @Test
    void getAds_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        mockMvc.perform(get(URL_GET_ADS))
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );
    }

    @DisplayName("An Ad picture update by unauthorized user")
    @Test
    void updateAdImage_shouldTReturnNewImageUrl_whenAdSuccessfullyUpdatedByAuthor() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        Authentication authentication = getAuthenticationFor(existingAd.getAuthor());
        String url = existingAd.getImage();

        MockMultipartFile newImageForAd = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_UPDATE_AD_IMAGE, existingAd.getId())
                        .file(newImageForAd)
                        .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                }
                        ))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk()
                );
    }

    @DisplayName("An Ad picture update by unauthorized user")
    @Test
    void updateAdImage_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        MockMultipartFile newImageForAd = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_UPDATE_AD_IMAGE, existingAd.getId())
                        .file(newImageForAd)
                        .with(request -> {
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
    void updateAdImage_shouldTReturn403_whenAuthorizedUserIsNotAdAuthor() throws Exception {
        AdEntity existingAd = TestUtils.getRandomAdFrom(ads);
        UserEntity userEntity = predefinedUsers.stream()
                .filter(user -> user.getId() != existingAd.getAuthor().getId())
                .findAny()
                .orElseThrow();
        Authentication authentication = getAuthenticationFor(userEntity);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_UPDATE_AD_IMAGE, existingAd.getId())
                        .file(imageFile)
                        .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                }
                        )
                )
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isForbidden()
                );
    }

    @DisplayName("Update the image of a nonexistent ad by an authorized admin")
    @Test
    void updateAdImage_withAuthorization_nonExistentAd_withAdminRole_thenNotFound() throws Exception {
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        int nonExistentAdId = TestUtils.getRandomNonExistentId(adRepository);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_UPDATE_AD_IMAGE, nonExistentAdId)
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

    @DisplayName("Update ad with invalid data as authorised admin")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void updateAdImage_shouldTReturn400_whenInvalidId(String id, String caseName) throws Exception {
        TestUtils.getAuthenticationFor(predefinedAdmin);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_UPDATE_AD_IMAGE, id)
                .file(imageFile)
                .with(
                        request -> {
                            request.setMethod("PATCH");
                            return request;
                        }
                )
        );
    }

    @DisplayName("Fetch non-existent ad as authorised user")
    @Test
    void updateAdImage_shouldTReturn404_whenAdIdDoesNotExist() throws Exception {
        TestUtils.getRandomUserAuthentication(predefinedUsers);
        int nonExistentId = TestUtils.getRandomNonExistentId(adRepository);

        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "image.png",
                MediaType.IMAGE_PNG_VALUE, TestUtils.generateRandomImageBytes());

        mockMvc.perform(multipart(URL_UPDATE_AD_IMAGE, nonExistentId)
                .file(imageFile)
                .with(
                        request -> {
                            request.setMethod("PATCH");
                            return request;
                        }
                )
        );
    }

    static Stream<Arguments> getInvalidId() {
        return Stream.of(
                Arguments.of("-1", "id is negative"),
                Arguments.of("t7", "id contains character")
        );
    }
}