package com.nowakArtur97.myMoments.followerService.feature.node;

import com.nowakArtur97.myMoments.followerService.feature.UserTestBuilder;
import com.nowakArtur97.myMoments.followerService.feature.resource.UserModel;
import com.nowakArtur97.myMoments.followerService.feature.resource.UsersAcquaintancesModel;
import com.nowakArtur97.myMoments.followerService.testUtil.enums.ObjectType;
import com.nowakArtur97.myMoments.followerService.testUtil.generator.NameWithSpacesGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(NameWithSpacesGenerator.class)
@Tag("FollowerService_Tests")
class FollowerServiceTest {

    private FollowerService followerService;

    @Mock
    private UserService userService;

    private static MockedStatic<UUID> mocked;

    private static UserTestBuilder userTestBuilder;

    @BeforeAll
    static void setUpBuilderAndUUID() {

        userTestBuilder = new UserTestBuilder();

        UUID uuid = UUID.randomUUID();
        mocked = mockStatic(UUID.class);
        mocked.when(UUID::randomUUID).thenReturn(uuid);
    }

    @AfterAll
    static void cleanUp() {

        if (!mocked.isClosed()) {
            mocked.close();
        }
    }

    @BeforeEach
    void setUp() {

        followerService = new FollowerService(userService);
    }

    @Nested
    class FindUserTest {

        @Test
        void when_find_existing_user_followers_should_return_followers() {

            String username = "user";
            String followerName = "follower";
            String followerName2 = "follower2";

            UserModel follower = (UserModel) userTestBuilder.withUsername(followerName).build(ObjectType.MODEL);
            UserNode userNodeExpected = (UserNode) userTestBuilder.withUsername(followerName).build(ObjectType.NODE);
            FollowingRelationship followingRelationshipExpected = new FollowingRelationship(userNodeExpected);
            UserModel follower2 = (UserModel) userTestBuilder.withUsername(followerName2)
                    .withFollowing(Set.of(followingRelationshipExpected)).build(ObjectType.MODEL);
            List<UserModel> followers = List.of(follower, follower2);

            when(userService.findFollowers(username)).thenReturn(Flux.fromIterable(followers));

            Mono<UsersAcquaintancesModel> acquaintancesActualMono = followerService.findFollowers(username);

            StepVerifier.create(acquaintancesActualMono)
                    .thenConsumeWhile(
                            acquaintancesActual -> {
                                UserModel actualUser = acquaintancesActual.getUsers().stream()
                                        .filter(user -> user.getUsername().equals(followerName))
                                        .findFirst().get();
                                UserModel actualUser2 = acquaintancesActual.getUsers().stream()
                                        .filter(user -> user.getUsername().equals(followerName2))
                                        .findFirst().get();
                                assertAll(
                                        () -> assertEquals(followers.size(), acquaintancesActual.getUsers().size(),
                                                () -> "should return followers: " + followers.size()
                                                        + ", but was: " + acquaintancesActual.getUsers()),
                                        () -> assertTrue(acquaintancesActual.getUsers().stream()
                                                        .anyMatch(user -> user.getUsername().equals(followerName)),
                                                () -> "should return follower with name: " + followerName
                                                        + ", but was: " + acquaintancesActual.getUsers()),
                                        () -> assertEquals(follower.getNumberOfFollowers(), actualUser.getNumberOfFollowers(),
                                                () -> "should return follower with number of followers: " + follower.getNumberOfFollowers()
                                                        + ", but was: " + actualUser.getNumberOfFollowers()),
                                        () -> assertEquals(follower.getNumberOfFollowing(), actualUser.getNumberOfFollowing(),
                                                () -> "should return follower with number of following: " + follower.getNumberOfFollowing()
                                                        + ", but was: " + actualUser.getNumberOfFollowing()),

                                        () -> assertTrue(acquaintancesActual.getUsers().stream()
                                                        .anyMatch(user -> user.getUsername().equals(followerName2)),
                                                () -> "should return follower with name: " + followerName2
                                                        + ", but was: " + acquaintancesActual.getUsers()),
                                        () -> assertEquals(follower2.getNumberOfFollowers(), actualUser2.getNumberOfFollowers(),
                                                () -> "should return follower with number of followers: " + follower2.getNumberOfFollowers()
                                                        + ", but was: " + actualUser2.getNumberOfFollowers()),
                                        () -> assertEquals(follower2.getNumberOfFollowing(), actualUser2.getNumberOfFollowing(),
                                                () -> "should return follower with number of following: " + follower2.getNumberOfFollowing()
                                                        + ", but was: " + actualUser2.getNumberOfFollowing()),
                                        () -> verify(userService, times(1)).findFollowers(username),
                                        () -> verifyNoMoreInteractions(userService));
                                return true;
                            }
                    ).verifyComplete();
        }

        @Test
        void when_find_existing_user_followers_of_user_without_followers_should_return_empty_list() {

            String username = "user";

            when(userService.findFollowers(username)).thenReturn(Flux.empty());

            Mono<UsersAcquaintancesModel> acquaintancesActualMono = followerService.findFollowers(username);

            StepVerifier.create(acquaintancesActualMono)
                    .thenConsumeWhile(
                            acquaintancesActual -> {
                                assertAll(
                                        () -> assertTrue(acquaintancesActual.getUsers().isEmpty(),
                                                () -> "should not return any followers, but was: " + acquaintancesActual.getUsers()),
                                        () -> verify(userService, times(1)).findFollowers(username),
                                        () -> verifyNoMoreInteractions(userService));
                                return true;
                            }
                    ).verifyComplete();
        }

        @Test
        void when_find_existing_user_following_should_return_following() {

            String username = "user";
            String followedName = "followed";
            String followedName2 = "followed2";

            UserModel followed = (UserModel) userTestBuilder.withUsername(followedName).build(ObjectType.MODEL);
            UserNode userNodeExpected = (UserNode) userTestBuilder.withUsername(followedName).build(ObjectType.NODE);
            FollowingRelationship followingRelationshipExpected = new FollowingRelationship(userNodeExpected);
            UserModel followed2 = (UserModel) userTestBuilder.withUsername(followedName2)
                    .withFollowers(Set.of(followingRelationshipExpected)).build(ObjectType.MODEL);
            List<UserModel> following = List.of(followed, followed2);

            when(userService.findFollowed(username)).thenReturn(Flux.fromIterable(following));

            Mono<UsersAcquaintancesModel> acquaintancesActualMono = followerService.findFollowed(username);

            StepVerifier.create(acquaintancesActualMono)
                    .thenConsumeWhile(
                            acquaintancesActual -> {
                                UserModel actualUser = acquaintancesActual.getUsers().stream()
                                        .filter(user -> user.getUsername().equals(followedName))
                                        .findFirst().get();
                                UserModel actualUser2 = acquaintancesActual.getUsers().stream()
                                        .filter(user -> user.getUsername().equals(followedName2))
                                        .findFirst().get();
                                assertAll(
                                        () -> assertEquals(following.size(), acquaintancesActual.getUsers().size(),
                                                () -> "should return following: " + following.size()
                                                        + ", but was: " + acquaintancesActual.getUsers()),
                                        () -> assertTrue(acquaintancesActual.getUsers().stream()
                                                        .anyMatch(user -> user.getUsername().equals(followedName)),
                                                () -> "should return following with name: " + followedName
                                                        + ", but was: " + acquaintancesActual.getUsers()),
                                        () -> assertEquals(followed.getNumberOfFollowers(), actualUser.getNumberOfFollowers(),
                                                () -> "should return followed with number of followers: " + followed.getNumberOfFollowers()
                                                        + ", but was: " + actualUser.getNumberOfFollowers()),
                                        () -> assertEquals(followed.getNumberOfFollowing(), actualUser.getNumberOfFollowing(),
                                                () -> "should return followed with number of following: " + followed.getNumberOfFollowing()
                                                        + ", but was: " + actualUser.getNumberOfFollowing()),

                                        () -> assertTrue(acquaintancesActual.getUsers().stream()
                                                        .anyMatch(user -> user.getUsername().equals(followedName2)),
                                                () -> "should return following with name: " + followedName2
                                                        + ", but was: " + acquaintancesActual.getUsers()),
                                        () -> assertEquals(followed2.getNumberOfFollowers(), actualUser2.getNumberOfFollowers(),
                                                () -> "should return followed with number of followers: " + followed2.getNumberOfFollowers()
                                                        + ", but was: " + actualUser.getNumberOfFollowers()),
                                        () -> assertEquals(followed2.getNumberOfFollowing(), actualUser2.getNumberOfFollowing(),
                                                () -> "should return followed with number of following: " + followed2.getNumberOfFollowing()
                                                        + ", but was: " + actualUser2.getNumberOfFollowing()),
                                        () -> verify(userService, times(1)).findFollowed(username),
                                        () -> verifyNoMoreInteractions(userService));
                                return true;
                            }
                    ).verifyComplete();
        }

        @Test
        void when_find_existing_user_following_of_user_without_following_should_return_empty_list() {

            String username = "user";

            when(userService.findFollowed(username)).thenReturn(Flux.empty());

            Mono<UsersAcquaintancesModel> acquaintancesActualMono = followerService.findFollowed(username);

            StepVerifier.create(acquaintancesActualMono)
                    .thenConsumeWhile(
                            acquaintancesActual -> {
                                assertAll(
                                        () -> assertTrue(acquaintancesActual.getUsers().isEmpty(),
                                                () -> "should not return any following, but was: " + acquaintancesActual.getUsers()),
                                        () -> verify(userService, times(1)).findFollowed(username),
                                        () -> verifyNoMoreInteractions(userService));
                                return true;
                            }
                    ).verifyComplete();
        }
    }

    @Nested
    class FollowUserTest {

        @Test
        void when_follow_not_existing_user_by_not_existing_user_should_create_users_and_follow_user() {

            String notExistingUser = "notExistingUser";
            String notExistingUserToFollow = "notExistingUserToFollow";

            UserNode followerExpected = (UserNode) userTestBuilder.withUsername(notExistingUser).build(ObjectType.NODE);
            UserNode followingExpected = (UserNode) userTestBuilder.withUsername(notExistingUserToFollow).build(ObjectType.NODE);

            when(userService.findUserByUsername(notExistingUser)).thenReturn(Mono.empty());
            when(userService.findUserByUsername(notExistingUserToFollow)).thenReturn(Mono.empty());
            when(userService.saveUser(followerExpected)).thenReturn(Mono.just(followerExpected));
            when(userService.saveUser(followingExpected)).thenReturn(Mono.just(followingExpected));
            Void mock = mock(Void.class);
            when(userService.followUser(notExistingUser, notExistingUserToFollow)).thenReturn(Mono.just(mock));

            Mono<Void> voidActualMono = followerService.followUser(notExistingUser, notExistingUserToFollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(notExistingUser),
                                    () -> verify(userService, times(1))
                                            .findUserByUsername(notExistingUserToFollow),
                                    () -> verify(userService, times(2)).saveUser(followerExpected),
                                    () -> verify(userService, times(2)).saveUser(followingExpected),
                                    () -> verify(userService, times(1))
                                            .followUser(notExistingUser, notExistingUserToFollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyComplete();
        }

        @Test
        void when_follow_existing_user_by_not_existing_user_should_create_user_and_follow_user() {

            String notExistingUser = "notExistingUser";
            String usernameToFollow = "userToFollow";

            UserNode followerExpected = (UserNode) userTestBuilder.withUsername(notExistingUser).build(ObjectType.NODE);
            UserNode followingExpected = (UserNode) userTestBuilder.withUsername(usernameToFollow).build(ObjectType.NODE);

            FollowingRelationship followingRelationshipExpected = new FollowingRelationship(followingExpected);
            FollowingRelationship followedRelationshipExpected = new FollowingRelationship(followerExpected);

            UserNode followingWithFollowerExpected = (UserNode) userTestBuilder.withUsername(usernameToFollow)
                    .withFollowers(Set.of(followedRelationshipExpected)).build(ObjectType.NODE);
            UserNode followerWithFollowingExpected = (UserNode) userTestBuilder.withUsername(usernameToFollow)
                    .withFollowing(Set.of(followingRelationshipExpected)).build(ObjectType.NODE);

            when(userService.findUserByUsername(notExistingUser)).thenReturn(Mono.empty());
            when(userService.findUserByUsername(usernameToFollow)).thenReturn(Mono.just(followingExpected));
            when(userService.saveUser(followingExpected)).thenReturn(Mono.just(followingExpected));
            when(userService.saveUser(followingWithFollowerExpected)).thenReturn(Mono.just(followingWithFollowerExpected));
            Void mock = mock(Void.class);
            when(userService.followUser(notExistingUser, usernameToFollow)).thenReturn(Mono.just(mock));

            Mono<Void> voidActualMono = followerService.followUser(notExistingUser, usernameToFollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(notExistingUser),
                                    () -> verify(userService, times(1)).findUserByUsername(usernameToFollow),
                                    () -> verify(userService, times(1)).saveUser(followerWithFollowingExpected),
                                    () -> verify(userService, times(1)).saveUser(followingWithFollowerExpected),
                                    () -> verify(userService, times(1))
                                            .followUser(notExistingUser, usernameToFollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyComplete();
        }

        @Test
        void when_follow_not_existing_user_by_existing_user_should_create_user_and_follow_user() {

            String username = "user";
            String notExistingUserToFollow = "notExistingUserToFollow";

            UserNode followerExpected = (UserNode) userTestBuilder.withUsername(username).build(ObjectType.NODE);
            UserNode followingExpected = (UserNode) userTestBuilder.withUsername(notExistingUserToFollow).build(ObjectType.NODE);

            FollowingRelationship followingRelationshipExpected = new FollowingRelationship(followingExpected);
            FollowingRelationship followedRelationshipExpected = new FollowingRelationship(followerExpected);

            UserNode followingWithFollowerExpected = (UserNode) userTestBuilder.withUsername(notExistingUserToFollow)
                    .withFollowers(Set.of(followedRelationshipExpected)).build(ObjectType.NODE);
            UserNode followerWithFollowingExpected = (UserNode) userTestBuilder.withUsername(notExistingUserToFollow)
                    .withFollowing(Set.of(followingRelationshipExpected)).build(ObjectType.NODE);

            when(userService.findUserByUsername(username)).thenReturn(Mono.just(followerExpected));
            when(userService.findUserByUsername(notExistingUserToFollow)).thenReturn(Mono.empty());
            when(userService.saveUser(followingExpected)).thenReturn(Mono.just(followingExpected));
            when(userService.saveUser(followingWithFollowerExpected)).thenReturn(Mono.just(followingWithFollowerExpected));
            Void mock = mock(Void.class);
            when(userService.followUser(username, notExistingUserToFollow)).thenReturn(Mono.just(mock));

            Mono<Void> voidActualMono = followerService.followUser(username, notExistingUserToFollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(username),
                                    () -> verify(userService, times(1))
                                            .findUserByUsername(notExistingUserToFollow),
                                    () -> verify(userService, times(1)).saveUser(followerWithFollowingExpected),
                                    () -> verify(userService, times(1)).saveUser(followingWithFollowerExpected),
                                    () -> verify(userService, times(1))
                                            .followUser(username, notExistingUserToFollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyComplete();
        }

        @Test
        void when_follow_existing_user_by_existing_user_should_follow_user() {

            String username = "user";
            String usernameToFollow = "userToFollow";

            UserNode followerExpected = (UserNode) userTestBuilder.withUsername(username).build(ObjectType.NODE);
            UserNode followingExpected = (UserNode) userTestBuilder.withUsername(usernameToFollow).build(ObjectType.NODE);

            when(userService.findUserByUsername(username)).thenReturn(Mono.just(followerExpected));
            when(userService.findUserByUsername(usernameToFollow)).thenReturn(Mono.just(followingExpected));
            Void mock = mock(Void.class);
            when(userService.followUser(username, usernameToFollow)).thenReturn(Mono.just(mock));

            Mono<Void> voidActualMono = followerService.followUser(username, usernameToFollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(username),
                                    () -> verify(userService, times(1)).findUserByUsername(usernameToFollow),
                                    () -> verify(userService, times(1)).followUser(username, usernameToFollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyComplete();
        }

        @Test
        void when_follow_user_for_a_second_time_should_throw_exception() {

            String username = "user";
            String usernameToFollow = "userToFollow";

            UserNode followingWithFollowerExpected = (UserNode) userTestBuilder.withUsername(username).build(ObjectType.NODE);
            UserNode followerWithFollowingExpected = (UserNode) userTestBuilder.withUsername(usernameToFollow).build(ObjectType.NODE);

            FollowingRelationship followingRelationshipExpected = new FollowingRelationship(followingWithFollowerExpected);
            FollowingRelationship followedRelationshipExpected = new FollowingRelationship(followerWithFollowingExpected);

            followingWithFollowerExpected.getFollowing().add(followingRelationshipExpected);
            followerWithFollowingExpected.getFollowing().add(followedRelationshipExpected);

            when(userService.findUserByUsername(username)).thenReturn(Mono.just(followingWithFollowerExpected));
            when(userService.findUserByUsername(usernameToFollow)).thenReturn(Mono.just(followerWithFollowingExpected));

            Mono<Void> voidActualMono = followerService.followUser(username, usernameToFollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(username),
                                    () -> verify(userService, times(1)).findUserByUsername(usernameToFollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyErrorMessage("User with username: '" + username + "' is already following: " + usernameToFollow + ".");
        }

        @Test
        void when_follow_user_own_account_should_throw_exception() {

            String username = "user";

            Mono<Void> voidActualMono = followerService.followUser(username, username);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verifyNoInteractions(userService))
                    ).verifyErrorMessage("User with username: '" + username + "' cannot follow himself.");
        }
    }

    @Nested
    class UnfollowUserTest {

        @Test
        void when_unfollow_user_should_unfollow_user() {

            String username = "user";
            String usernameToUnfollow = "userToUnfollow";

            UserNode followingWithFollowerExpected = (UserNode) userTestBuilder.withUsername(username).build(ObjectType.NODE);
            UserNode followerWithFollowingExpected = (UserNode) userTestBuilder.withUsername(usernameToUnfollow)
                    .build(ObjectType.NODE);

            followingWithFollowerExpected.getFollowing().add(new FollowingRelationship(followerWithFollowingExpected));
            followerWithFollowingExpected.getFollowers().add(new FollowingRelationship(followingWithFollowerExpected));

            when(userService.findUserByUsername(username)).thenReturn(Mono.just(followingWithFollowerExpected));
            when(userService.findUserByUsername(usernameToUnfollow)).thenReturn(Mono.just(followerWithFollowingExpected));
            Void mock = mock(Void.class);
            when(userService.unfollowUser(username, usernameToUnfollow)).thenReturn(Mono.just(mock));

            Mono<Void> voidActualMono = followerService.unfollowUser(username, usernameToUnfollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(username),
                                    () -> verify(userService, times(1)).findUserByUsername(usernameToUnfollow),
                                    () -> verify(userService, times(1))
                                            .unfollowUser(username, usernameToUnfollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyComplete();
        }

        @Test
        void when_unfollow_by_not_existing_user_should_throw_exception() {

            String notExistingUser = "notExistingUser";
            String usernameToUnfollow = "userToUnfollow";

            UserNode followerWithFollowingExpected = (UserNode) userTestBuilder.withUsername(usernameToUnfollow).build(ObjectType.NODE);
            FollowingRelationship followedRelationshipExpected = new FollowingRelationship(followerWithFollowingExpected);

            followerWithFollowingExpected.getFollowing().add(followedRelationshipExpected);

            when(userService.findUserByUsername(notExistingUser)).thenReturn(Mono.empty());
            when(userService.findUserByUsername(usernameToUnfollow)).thenReturn(Mono.just(followerWithFollowingExpected));

            Mono<Void> voidActualMono = followerService.unfollowUser(notExistingUser, usernameToUnfollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(notExistingUser),
                                    () -> verify(userService, times(1)).findUserByUsername(usernameToUnfollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyErrorMessage("User with username: '" + notExistingUser + "' not found.");
        }

        @Test
        void when_unfollow_not_existing_user_should_throw_exception() {

            String username = "user";
            String notExistingUserToUnfollow = "notExistingUserToUnfollow";

            UserNode followingWithFollowerExpected = (UserNode) userTestBuilder.withUsername(username).build(ObjectType.NODE);
            FollowingRelationship followingRelationshipExpected = new FollowingRelationship(followingWithFollowerExpected);

            followingWithFollowerExpected.getFollowing().add(followingRelationshipExpected);

            when(userService.findUserByUsername(username)).thenReturn(Mono.just(followingWithFollowerExpected));
            when(userService.findUserByUsername(notExistingUserToUnfollow)).thenReturn(Mono.empty());

            Mono<Void> voidActualMono = followerService.unfollowUser(username, notExistingUserToUnfollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(username),
                                    () -> verify(userService, times(1)).findUserByUsername(notExistingUserToUnfollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyErrorMessage("Follower with username: '" + notExistingUserToUnfollow + "' not found.");
        }

        @Test
        void when_unfollow_user_own_account_should_throw_exception() {

            String username = "user";

            Mono<Void> voidActualMono = followerService.unfollowUser(username, username);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verifyNoInteractions(userService))
                    ).verifyErrorMessage("User with username: '" + username + "' cannot unfollow himself.");
        }

        @Test
        void when_unfollow_not_followed_user_should_throw_exception() {

            String username = "user";
            String usernameToUnfollow = "userToUnfollow";

            UserNode followingWithFollowerExpected = (UserNode) userTestBuilder.withUsername(username).build(ObjectType.NODE);
            UserNode followerWithFollowingExpected = (UserNode) userTestBuilder.withUsername(usernameToUnfollow).build(ObjectType.NODE);

            when(userService.findUserByUsername(username)).thenReturn(Mono.just(followingWithFollowerExpected));
            when(userService.findUserByUsername(usernameToUnfollow)).thenReturn(Mono.just(followerWithFollowingExpected));

            Mono<Void> voidActualMono = followerService.unfollowUser(username, usernameToUnfollow);

            StepVerifier.create(voidActualMono)
                    .then(() ->
                            assertAll(
                                    () -> verify(userService, times(1)).findUserByUsername(username),
                                    () -> verify(userService, times(1)).findUserByUsername(usernameToUnfollow),
                                    () -> verifyNoMoreInteractions(userService))
                    ).verifyErrorMessage("User with name: '" + username + "' is not following: '" + usernameToUnfollow + "'.");
        }
    }
}
