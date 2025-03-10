package com.nowakArtur97.myMoments.userService.feature.testBuilder;

import com.nowakArtur97.myMoments.userService.feature.authentication.AuthenticationResponse;
import com.nowakArtur97.myMoments.userService.feature.common.User;
import com.nowakArtur97.myMoments.userService.feature.common.UserProfile;
import com.nowakArtur97.myMoments.userService.feature.document.RoleDocument;
import com.nowakArtur97.myMoments.userService.feature.document.UserDocument;
import com.nowakArtur97.myMoments.userService.feature.document.UserProfileDocument;
import com.nowakArtur97.myMoments.userService.feature.resource.*;
import com.nowakArtur97.myMoments.userService.feature.authentication.AuthenticationRequest;
import com.nowakArtur97.myMoments.userService.testUtil.enums.ObjectType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserTestBuilder {

    private String username = "user123";

    private String password = "SecretPassword123!@";

    private String matchingPassword = "SecretPassword123!@";

    private String email = "userEmail123@email.com";

    private String token = "token";

    private long expirationTime = 36000000;

    private UserProfile profile;

    private Set<RoleDocument> roles = new HashSet<>(Collections.singletonList(RoleTestBuilder.DEFAULT_ROLE_ENTITY));

    public UserTestBuilder withUsername(String username) {

        this.username = username;

        return this;
    }

    public UserTestBuilder withPassword(String password) {

        this.password = password;

        return this;
    }

    public UserTestBuilder withMatchingPassword(String matchingPassword) {

        this.matchingPassword = matchingPassword;

        return this;
    }

    public UserTestBuilder withEmail(String email) {

        this.email = email;

        return this;
    }

    public UserTestBuilder withToken(String token) {

        this.token = token;

        return this;
    }

    public UserTestBuilder withExpirationTime(long expirationTime) {

        this.expirationTime = expirationTime;

        return this;
    }

    public UserTestBuilder withRoles(Set<RoleDocument> roles) {

        this.roles = roles;

        return this;
    }

    public UserTestBuilder withProfile(UserProfile profile) {

        this.profile = profile;

        return this;
    }

    public User build(ObjectType type) {

        User user;

        switch (type) {

            case CREATE_DTO:

                user = new UserRegistrationDTO(username, email, password, matchingPassword, (UserProfileDTO) profile);

                break;

            case UPDATE_DTO:

                user = new UserUpdateDTO(username, email, password, matchingPassword, (UserProfileDTO) profile);

                break;

            case DOCUMENT:

                user = new UserDocument(username, email, password, (UserProfileDocument) profile, roles);

                break;

            case REQUEST:

                user = new AuthenticationRequest(username, password, email);

                break;

            case MODEL:

                List<RoleModel> rolesModels = roles.stream()
                        .map(role -> new RoleModel(role.getName()))
                        .collect(Collectors.toList());
                user = new UserModel(username, email, new AuthenticationResponse(token, expirationTime),
                        (UserProfileModel) profile, rolesModels);

                break;

            default:
                throw new RuntimeException("The specified type does not exist");
        }

        resetProperties();

        return user;
    }

    private void resetProperties() {

        username = "user123";

        password = "SecretPassword123!@";

        matchingPassword = "SecretPassword123!@";

        email = "userEmail123@email.com";

        profile = null;

        token = "token";

        expirationTime = 36000000;

        roles = new HashSet<>();
    }
}
