package com.nowakArtur97.myMoments.userService.feature.resource;

import com.nowakArtur97.myMoments.userService.feature.document.Gender;
import com.nowakArtur97.myMoments.userService.feature.testBuilder.UserProfileTestBuilder;
import com.nowakArtur97.myMoments.userService.feature.testBuilder.UserTestBuilder;
import com.nowakArtur97.myMoments.userService.testUtil.enums.ObjectType;
import com.nowakArtur97.myMoments.userService.testUtil.generator.NameWithSpacesGenerator;
import com.nowakArtur97.myMoments.userService.testUtil.mapper.ObjectTestMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayNameGeneration(NameWithSpacesGenerator.class)
@Tag("UserRegistrationController_Tests")
class UserRegistrationControllerTest {

    @Value("${my-moments.jwt.validity:36000000}")
    private Long validity;

    @Value("${my-moments.default-user-role:USER_ROLE}")
    private String userRole;

    @LocalServerPort
    private int serverPort;

    private final String REGISTRATION_BASE_PATH = "http://localhost:" + serverPort + "/api/v1/registration/register";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static UserProfileTestBuilder userProfileTestBuilder;
    private static UserTestBuilder userTestBuilder;

    @BeforeAll
    static void setUpBuilders() {

        userProfileTestBuilder = new UserProfileTestBuilder();
        userTestBuilder = new UserTestBuilder();
    }

    @AfterEach
    void cleanUpDatabase() {

        mongoTemplate.getDb().drop();
    }

    @Test
    void when_register_valid_user_without_profile_should_register_user() {

        UserRegistrationDTO userRegistrationDTO = (UserRegistrationDTO) userTestBuilder.withUsername("validUser")
                .withEmail("validUser123@email.com").withPassword("ValidPassword123!").withMatchingPassword("ValidPassword123!")
                .build(ObjectType.CREATE_DTO);

        String userAsString = ObjectTestMapper.asJsonString(userRegistrationDTO);

        MockMultipartFile userData = new MockMultipartFile("user", "request",
                MediaType.MULTIPART_FORM_DATA_VALUE, userAsString.getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> mockMvc
                        .perform(multipart(REGISTRATION_BASE_PATH)
                                .file(userData)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("username", is(userRegistrationDTO.getUsername())))
                        .andExpect(jsonPath("email", is(userRegistrationDTO.getEmail())))
                        .andExpect(jsonPath("password").doesNotExist())
                        .andExpect(jsonPath("authenticationResponse.token", notNullValue()))
                        .andExpect(jsonPath("authenticationResponse.expirationTimeInMilliseconds",
                                is(validity.intValue())))
                        .andExpect(jsonPath("profile.about").isEmpty())
                        .andExpect(jsonPath("profile.gender", is(Gender.UNSPECIFIED.toString())))
                        .andExpect(jsonPath("profile.interests").isEmpty())
                        .andExpect(jsonPath("profile.languages").isEmpty())
                        .andExpect(jsonPath("profile.location").isEmpty())
                        .andExpect(jsonPath("profile.image").isEmpty())
                        .andExpect(jsonPath("roles[0].name", is(userRole)))
        );
    }

    @Test
    void when_register_valid_user_with_profile_should_register_user() {

        UserProfileDTO userProfileDTO = (UserProfileDTO) userProfileTestBuilder.withAbout("about")
                .withInterests("interests").withLanguages("languages").withLocation("location")
                .withGender(Gender.FEMALE).build(ObjectType.UPDATE_DTO);
        UserRegistrationDTO userRegistrationDTO = (UserRegistrationDTO) userTestBuilder.withUsername("validUserWithProfile")
                .withEmail("validUser123Profile@email.com").withPassword("ValidPassword123!")
                .withMatchingPassword("ValidPassword123!").withProfile(userProfileDTO).build(ObjectType.CREATE_DTO);

        String userAsString = ObjectTestMapper.asJsonString(userRegistrationDTO);

        MockMultipartFile userData = new MockMultipartFile("user", "request",
                MediaType.MULTIPART_FORM_DATA_VALUE, userAsString.getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> mockMvc
                        .perform(multipart(REGISTRATION_BASE_PATH)
                                .file(userData)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("username", is(userRegistrationDTO.getUsername())))
                        .andExpect(jsonPath("email", is(userRegistrationDTO.getEmail())))
                        .andExpect(jsonPath("password").doesNotExist())
                        .andExpect(jsonPath("authenticationResponse.token", notNullValue()))
                        .andExpect(jsonPath("authenticationResponse.expirationTimeInMilliseconds",
                                is(validity.intValue())))
                        .andExpect(jsonPath("profile.about", is(userProfileDTO.getAbout())))
                        .andExpect(jsonPath("profile.gender", is(userProfileDTO.getGender())))
                        .andExpect(jsonPath("profile.interests", is(userProfileDTO.getInterests())))
                        .andExpect(jsonPath("profile.languages", is(userProfileDTO.getLanguages())))
                        .andExpect(jsonPath("profile.location", is(userProfileDTO.getLocation())))
                        .andExpect(jsonPath("profile.image").isEmpty())
                        .andExpect(jsonPath("roles[0].name", is(userRole)))
        );
    }

    @Test
    void when_register_valid_user_with_null_profile_fields_should_register_user() {

        UserProfileDTO userProfileDTO = (UserProfileDTO) userProfileTestBuilder.withAbout(null).withInterests(null)
                .withLanguages(null).withLocation(null).build(ObjectType.CREATE_DTO);
        UserRegistrationDTO userRegistrationDTO = (UserRegistrationDTO) userTestBuilder.withUsername("validUserWithProfile")
                .withEmail("validUser123Profile@email.com").withPassword("ValidPassword123!")
                .withMatchingPassword("ValidPassword123!").withProfile(userProfileDTO)
                .build(ObjectType.CREATE_DTO);

        String userAsString = ObjectTestMapper.asJsonString(userRegistrationDTO);

        MockMultipartFile userData = new MockMultipartFile("user", "request",
                MediaType.MULTIPART_FORM_DATA_VALUE, userAsString.getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> mockMvc
                        .perform(multipart(REGISTRATION_BASE_PATH)
                                .file(userData)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("username", is(userRegistrationDTO.getUsername())))
                        .andExpect(jsonPath("email", is(userRegistrationDTO.getEmail())))
                        .andExpect(jsonPath("password").doesNotExist())
                        .andExpect(jsonPath("authenticationResponse.token", notNullValue()))
                        .andExpect(jsonPath("authenticationResponse.expirationTimeInMilliseconds",
                                is(validity.intValue())))
                        .andExpect(jsonPath("profile.about").isEmpty())
                        .andExpect(jsonPath("profile.gender", is(Gender.UNSPECIFIED.toString())))
                        .andExpect(jsonPath("profile.interests").isEmpty())
                        .andExpect(jsonPath("profile.languages").isEmpty())
                        .andExpect(jsonPath("profile.location").isEmpty())
                        .andExpect(jsonPath("profile.image").isEmpty())
                        .andExpect(jsonPath("roles[0].name", is(userRole)))
        );
    }

    @Test
    void when_register_valid_user_with_empty_profile_fields_should_register_user() {

        UserProfileDTO userProfileDTO = (UserProfileDTO) userProfileTestBuilder.withAbout("").withInterests("")
                .withLanguages("").withLocation("").build(ObjectType.CREATE_DTO);
        UserRegistrationDTO userRegistrationDTO = (UserRegistrationDTO) userTestBuilder.withUsername("validUserWithProfile")
                .withEmail("validUser123Profile@email.com").withPassword("ValidPassword123!")
                .withMatchingPassword("ValidPassword123!").withProfile(userProfileDTO)
                .build(ObjectType.CREATE_DTO);

        String userAsString = ObjectTestMapper.asJsonString(userRegistrationDTO);

        MockMultipartFile userData = new MockMultipartFile("user", "request",
                MediaType.MULTIPART_FORM_DATA_VALUE, userAsString.getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> mockMvc
                        .perform(multipart(REGISTRATION_BASE_PATH)
                                .file(userData)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("username", is(userRegistrationDTO.getUsername())))
                        .andExpect(jsonPath("email", is(userRegistrationDTO.getEmail())))
                        .andExpect(jsonPath("password").doesNotExist())
                        .andExpect(jsonPath("authenticationResponse.token", notNullValue()))
                        .andExpect(jsonPath("authenticationResponse.expirationTimeInMilliseconds",
                                is(validity.intValue())))
                        .andExpect(jsonPath("profile.about", is(userProfileDTO.getAbout())))
                        .andExpect(jsonPath("profile.gender", is(userProfileDTO.getGender())))
                        .andExpect(jsonPath("profile.interests", is(userProfileDTO.getInterests())))
                        .andExpect(jsonPath("profile.languages", is(userProfileDTO.getLanguages())))
                        .andExpect(jsonPath("profile.location", is(userProfileDTO.getLocation())))
                        .andExpect(jsonPath("profile.image").isEmpty())
                        .andExpect(jsonPath("roles[0].name", is(userRole)))
        );
    }

    @Test
    void when_register_valid_user_with_profile_and_image_should_register_user() {

        UserProfileDTO userProfileDTO = (UserProfileDTO) userProfileTestBuilder.build(ObjectType.CREATE_DTO);
        UserRegistrationDTO userRegistrationDTO = (UserRegistrationDTO) userTestBuilder.withUsername("userWithImage")
                .withEmail("user123Image@email.com").withPassword("ValidPassword123!")
                .withMatchingPassword("ValidPassword123!").withProfile(userProfileDTO)
                .build(ObjectType.CREATE_DTO);

        String userAsString = ObjectTestMapper.asJsonString(userRegistrationDTO);

        MockMultipartFile userData = new MockMultipartFile("user", "request",
                MediaType.MULTIPART_FORM_DATA_VALUE, userAsString.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile image = new MockMultipartFile("image", "image", MediaType.IMAGE_JPEG_VALUE,
                "image.jpg".getBytes());

        assertAll(
                () -> mockMvc
                        .perform(multipart(REGISTRATION_BASE_PATH)
                                .file(userData)
                                .file(image)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("username", is(userRegistrationDTO.getUsername())))
                        .andExpect(jsonPath("email", is(userRegistrationDTO.getEmail())))
                        .andExpect(jsonPath("password").doesNotExist())
                        .andExpect(jsonPath("authenticationResponse.token", notNullValue()))
                        .andExpect(jsonPath("authenticationResponse.expirationTimeInMilliseconds",
                                is(validity.intValue())))
                        .andExpect(jsonPath("profile.about", is(userProfileDTO.getAbout())))
                        .andExpect(jsonPath("profile.gender", is(userProfileDTO.getGender())))
                        .andExpect(jsonPath("profile.interests", is(userProfileDTO.getInterests())))
                        .andExpect(jsonPath("profile.languages", is(userProfileDTO.getLanguages())))
                        .andExpect(jsonPath("profile.location", is(userProfileDTO.getLocation())))
                        .andExpect(jsonPath("profile.image").isNotEmpty())
                        .andExpect(jsonPath("roles[0].name", is(userRole)))
        );
    }
}
