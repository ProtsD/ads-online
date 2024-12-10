package ru.ads_online.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import ru.ads_online.mapper.CommentMapper;
import ru.ads_online.pojo.dto.comment.Comment;
import ru.ads_online.pojo.dto.comment.CreateOrUpdateComment;
import ru.ads_online.pojo.entity.AdEntity;
import ru.ads_online.pojo.entity.CommentEntity;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.AdRepository;
import ru.ads_online.repository.CommentRepository;
import ru.ads_online.repository.ImageRepository;
import ru.ads_online.repository.UserRepository;
import ru.ads_online.service.ImageService;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AdsOnlineApplication.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@Testcontainers
@Transactional
@AutoConfigureMockMvc
public class CommentControllerTest {
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
    @Autowired
    private CommentMapper commentMapper;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:alpine");
    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final static int NUMBER_OF_TEST_ADS = 10;
    private final static int NUMBER_OF_TEST_USERS = 10;
    private final static int NUMBER_OF_TEST_COMMENTS = NUMBER_OF_TEST_ADS * 10;
    private final static String URL_GET_COMMENTS = "/ads/{id}/comments";
    private final static String URL_POST_COMMENT = "/ads/{id}/comments";
    private final static String URL_DELETE_COMMENT = "/ads/{adId}/comments/{commentId}";
    private final static String URL_PATCH_COMMENT = "/ads/{adId}/comments/{commentId}";
    private final static String REMAIN_COMMENT_COUNT = "Comment count should remain unchanged";
    private final static String DECREASE_COMMENT_COUNT = "Comment count should be decreased by 1";
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
        imageRepository.deleteAll();
        commentRepository.deleteAll();
        adRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DisplayName("Fetch all comments for invalid adId as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void getAllCommentsForAd_shouldTReturn400_whenInvalidAdId(String adId, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);

        mockMvc.perform(get(URL_GET_COMMENTS, adId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Fetch all comments for a non-existent ad as an authorized admin")
    @Test
    void getAllCommentsForAd_shouldTReturn404_whenAdIdDoesNotExist() throws Exception {
        int nonExistentAdId = TestUtils.getRandomNonExistentId(adRepository);
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(get(URL_GET_COMMENTS, nonExistentAdId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Fetch all comments for an ad as an authorized user")
    @Test
    void getAllCommentsForAd_shouldReturnComments_whenExists() throws Exception {
        AdEntity ad = TestUtils.getRandomAdFrom(ads);
        Authentication authentication = TestUtils.getAuthenticationFor(ad.getAuthor());
        List<Comment> comments = commentRepository.findAllByAdId(ad.getId());
        String json = objectMapper.writeValueAsString(commentMapper.toComments(comments));

        mockMvc.perform(get(URL_GET_COMMENTS, ad.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk(),
                        content().json(json)
                );
    }

    @DisplayName("Fetch all comments for an ad as an unauthorized user")
    @Test
    void getAllCommentsForAd_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        int randomCommentId = TestUtils.getRandomCommentFrom(comments).getId();

        mockMvc.perform(get(URL_GET_COMMENTS, randomCommentId))
                .andExpectAll(
                        unauthenticated(),
                        status().isUnauthorized()
                );
    }

    @DisplayName("Add comment for invalid adId as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void createComment_shouldTReturn400_whenInvalidAdId(String adId, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(post(URL_POST_COMMENT, adId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Add comment with invalid comment text for an ad as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidArgumentsForCreateComment")
    void createComment_shouldTReturn400_whenInvalidCommentText(String commentText, String caseDescription) throws Exception {
        AdEntity ad = TestUtils.getRandomAdFrom(ads);
        UserEntity commenter = ad.getAuthor();
        Authentication authentication = TestUtils.getAuthenticationFor(commenter);

        CreateOrUpdateComment commentRequest = new CreateOrUpdateComment().setText(commentText);
        String commentJson = objectMapper.writeValueAsString(commentRequest);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(post(URL_POST_COMMENT, ad.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    static Stream<Arguments> getInvalidArgumentsForCreateComment() {
        return Stream.of(
                Arguments.of("Comment", "Text is too short"),
                Arguments.of("Comment text is invalid because it exceeds the allowable length!!", "Text is too long"),
                Arguments.of(null, "Text is null")
        );
    }

    @DisplayName("Add a comment to a non-existent ad as an authorized admin")
    @Test
    void createComment_shouldTReturn404_whenCommentIdDoesNotExist() throws Exception {
        int nonExistentAdId = TestUtils.getRandomNonExistentId(commentRepository);
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        String commentText = TestUtils.getCommentsText(10, 1).get(0);
        CreateOrUpdateComment commentRequest = new CreateOrUpdateComment().setText(commentText);
        String commentJson = objectMapper.writeValueAsString(commentRequest);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(post(URL_POST_COMMENT, nonExistentAdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Add comment for an ad as an authorized user")
    @Test
    void createComment_shouldTReturn201AndComment_whenCommentSuccessfullyCreated() throws Exception {
        AdEntity ad = TestUtils.getRandomAdFrom(ads);
        UserEntity commenter = ad.getAuthor();
        Authentication authentication = TestUtils.getAuthenticationFor(commenter);

        String commentText = TestUtils.getCommentsText(10, 1).get(0);
        CreateOrUpdateComment comment = new CreateOrUpdateComment().setText(commentText);
        String commentJson = objectMapper.writeValueAsString(comment);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(post(URL_POST_COMMENT, ad.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isCreated(),
                        jsonPath("$.text").value(comment.getText()),
                        jsonPath("$.authorFirstName").value(commenter.getFirstName()),
                        jsonPath("$.author").value(commenter.getId())
                );
        assertEquals(commentCountBeforeRequest + 1, commentRepository.count(), DECREASE_COMMENT_COUNT);
    }

    @DisplayName("Add comment for an ad as an unauthorized user")
    @Test
    void createComment_shouldTReturn401_whenUnauthorizedUser() throws Exception {
        AdEntity ad = TestUtils.getRandomAdFrom(ads);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(post(URL_POST_COMMENT, ad.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Delete comment for invalid adId as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void deleteComment_shouldTReturn400_whenInvalidAdId(String adId, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);

        mockMvc.perform(delete(URL_DELETE_COMMENT, adId, comment.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Delete comment with invalid commentId as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void deleteComment_shouldTReturn400_whenInvalidCommentId(String commentId, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        AdEntity ad = TestUtils.getRandomAdFrom(ads);

        mockMvc.perform(delete(URL_DELETE_COMMENT, ad.getId(), commentId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Delete a non-existent comment as an authorized admin")
    @Test
    void deleteComment_shouldTReturn404_whenCommentIdDoesNotExist() throws Exception {
        AdEntity ad = TestUtils.getRandomAdFrom(ads);
        int nonExistentCommentId = TestUtils.getRandomNonExistentId(commentRepository);
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(delete(URL_DELETE_COMMENT, ad.getId(), nonExistentCommentId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Delete comment which doesn't belong to adId as an authorized admin")
    @Test
    void deleteComment_shouldTReturn404_whenCommentDoesNotBelongToAd() throws Exception {
        int nonExistentAdId = TestUtils.getRandomNonExistentId(commentRepository);
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(delete(URL_DELETE_COMMENT, nonExistentAdId, comment.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Delete one's own comment as an authorized user")
    @Test
    void deleteComment_shouldTReturn204_whenCommentSuccessfullyDeleted() throws Exception {
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        AdEntity ad = comment.getAdEntity();
        Authentication authentication = TestUtils.getAuthenticationFor(comment.getAuthor());

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(delete(URL_DELETE_COMMENT, ad.getId(), comment.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNoContent()
                );
        assertEquals(commentCountBeforeRequest - 1, commentRepository.count(), DECREASE_COMMENT_COUNT);
    }

    @DisplayName("Delete someone else's comment as an authorized user")
    @Test
    void deleteComment_shouldTReturn403_whenAuthorizedUserIsNotAuthorOfTheComment() throws Exception {
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        AdEntity ad = comment.getAdEntity();
        UserEntity user = TestUtils.findDistinctElement(predefinedUsers, comment.getAuthor());
        Authentication authentication = TestUtils.getAuthenticationFor(user);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(delete(URL_DELETE_COMMENT, ad.getId(), comment.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isForbidden()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
        assertTrue(commentRepository.existsById(comment.getId()), "The comment should still exist");

    }

    @DisplayName("Delete someone else's comment as an authorized admin")
    @Test
    void deleteComment_shouldTReturn204_whenCommentSuccessfullyDeletedByAdmin() throws Exception {
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        AdEntity ad = comment.getAdEntity();
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(delete(URL_DELETE_COMMENT, ad.getId(), comment.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNoContent()
                );
        assertEquals(commentCountBeforeRequest - 1, commentRepository.count(), DECREASE_COMMENT_COUNT);
    }

    @DisplayName("Update comment for invalid adId as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void updateComment_shouldTReturn400_whenInvalidAdId(String adId, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);

        mockMvc.perform(patch(URL_PATCH_COMMENT, adId, comment.getId()))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Update comment with invalid commentId as an authorized user")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getInvalidId")
    void updateComment_shouldTReturn400_whenInvalidCommentId(String commentId, String caseName) throws Exception {
        Authentication authentication = TestUtils.getRandomUserAuthentication(predefinedUsers);
        AdEntity ad = TestUtils.getRandomAdFrom(ads);

        mockMvc.perform(patch(URL_PATCH_COMMENT, ad.getId(), commentId))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isBadRequest()
                );
    }

    @DisplayName("Update a non-existent comment as an authorized admin")
    @Test
    void updateComment_shouldTReturn404_whenCommentIdDoesNotExist() throws Exception {
        AdEntity ad = TestUtils.getRandomAdFrom(ads);
        int nonExistentCommentId = TestUtils.getRandomNonExistentId(commentRepository);
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        String commentJson = TestUtils.getRandomCommentDtoJson(objectMapper);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(patch(URL_PATCH_COMMENT, ad.getId(), nonExistentCommentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Update comment which doesn't belong to adId as an authorized admin")
    @Test
    void updateComment_shouldTReturn404_whenCommentDoesNotBelongToAd() throws Exception {
        int nonExistentAdId = TestUtils.getRandomNonExistentId(commentRepository);
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        String commentJson = TestUtils.getRandomCommentDtoJson(objectMapper);

        long commentCountBeforeRequest = commentRepository.count();
        mockMvc.perform(patch(URL_PATCH_COMMENT, nonExistentAdId, comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isNotFound()
                );
        assertEquals(commentCountBeforeRequest, commentRepository.count(), REMAIN_COMMENT_COUNT);
    }

    @DisplayName("Updating one's own comment as an authorized user")
    @Test
    void updateComment_shouldReturnComment_whenCommentSuccessfullyUpdated() throws Exception {
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        AdEntity ad = comment.getAdEntity();
        Authentication authentication = TestUtils.getAuthenticationFor(comment.getAuthor());

        String commentText = TestUtils.getCommentsText(10, 1).get(0);
        CreateOrUpdateComment commentRequest = new CreateOrUpdateComment().setText(commentText);
        String commentJson = objectMapper.writeValueAsString(commentRequest);
        String expectedComment = objectMapper.writeValueAsString(commentMapper.toComment(comment).setText(commentText));

        mockMvc.perform(patch(URL_PATCH_COMMENT, ad.getId(), comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        content().json(expectedComment),
                        status().isOk()
                );
    }

    @DisplayName("Updating someone else's comment as an authorized user")
    @Test
    void updateComment_shouldTReturn403_whenAuthorizedUserIsNotAuthorOfTheComment() throws Exception {
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        AdEntity ad = comment.getAdEntity();
        UserEntity user = TestUtils.getRandomUserFrom(predefinedUsers);
        Authentication authentication = TestUtils.getAuthenticationFor(user);
        String commentJson = TestUtils.getRandomCommentDtoJson(objectMapper);

        mockMvc.perform(patch(URL_PATCH_COMMENT, ad.getId(), comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isForbidden()
                );
    }

    @DisplayName("Updating someone else's comment as an authorized admin")
    @Test
    void updateComment_shouldReturnComment_whenCommentSuccessfullyUpdatedByAdmin() throws Exception {
        CommentEntity comment = TestUtils.getRandomCommentFrom(comments);
        AdEntity ad = comment.getAdEntity();
        Authentication authentication = TestUtils.getAuthenticationFor(predefinedAdmin);
        String commentJson = TestUtils.getRandomCommentDtoJson(objectMapper);

        mockMvc.perform(patch(URL_PATCH_COMMENT, ad.getId(), comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpectAll(
                        authenticated().withAuthenticationName(authentication.getName()),
                        status().isOk()
                );
    }

    static Stream<Arguments> getInvalidId() {
        return Stream.of(
                Arguments.of("-1", "id is negative"),
                Arguments.of("t7", "id contains character")
        );
    }
}