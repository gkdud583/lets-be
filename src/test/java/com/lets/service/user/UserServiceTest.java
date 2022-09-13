package com.lets.service.user;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lets.domain.tag.Tag;
import com.lets.domain.user.User;
import com.lets.domain.user.UserRepository;
import com.lets.domain.userTechStack.UserTechStack;
import com.lets.domain.userTechStack.UserTechStackRepository;
import com.lets.exception.CustomException;
import com.lets.security.AuthProvider;
import com.lets.util.CloudinaryUtil;
import com.lets.web.dto.auth.SignupRequestDto;
import com.lets.web.dto.user.SettingResponseDto;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;

    @Mock
    UserTechStackRepository userTechStackRepository;

    @Mock
    CloudinaryUtil cloudinaryUtil;

    SignupRequestDto signupRequestDto = new SignupRequestDto(null, "user1", "1234", AuthProvider.google, new ArrayList<>());
    User user = User.createUser("nickname", "1234", AuthProvider.google, "PUBLIC");

    Tag tag = Tag.createTag("spring");

    UserTechStack userTechStack = UserTechStack.createUserTechStack(tag, user);

    @Test
    void validateName_실패(){
        //given
        given(userRepository.existsByNickname(any()))
                .willReturn(true);
        //when
        Exception exception  = Assertions.assertThrows(CustomException.class, () -> userService.validateNickname(any()));

        //then
        assertEquals("중복된 닉네임입니다.", exception.getMessage());


    }
    @Test
    void validateName_성공(){
        //given
        given(userRepository.existsByNickname(any()))
                .willReturn(false);
        //when
        userService.validateNickname(any());
        //then


    }


    @Test
    void findBySocialLoginIdAndAuthProvider_성공(){
        //given
        User user = User.createUser("user1", "123", AuthProvider.google, "123");
        given(userRepository.findBySocialLoginIdAndAuthProvider(any(), any()))
                .willReturn(Optional.of(user));
        //when
        User findUser = userService.findBySocialLoginIdAndAuthProvider(any(), any());

        //then
        assertThat(findUser).isNotNull();
    }
    @Test
    void findBySocialLoginIdAndAuthProvider_실패(){
        //given
        given(userRepository.findBySocialLoginIdAndAuthProvider(any(), any()))
                .willReturn(Optional.ofNullable(null));
        //when
        Exception exception  = Assertions.assertThrows(CustomException.class, () -> userService.findBySocialLoginIdAndAuthProvider(any(), any()));

        //then
        assertThat(exception.getMessage()).isEqualTo("로그인 정보[SOCIAL_LOGIN_ID, AUTH_PROVIDER]가 올바르지 않습니다.");


    }

    @Test
    void existsById_성공() {
        //given
        given(userRepository.existsById(any()))
                .willReturn(true);
        //when
        boolean result = userService.existsById(any());

        //then
        assertThat(result).isTrue();
    }
    @Test
    void existsById_실패() {
        //given
        given(userRepository.existsById(any()))
                .willReturn(false);
        //when
        Exception exception  = Assertions.assertThrows(CustomException.class, () -> userService.existsById(any()));

        //then
        assertThat(exception.getMessage()).isEqualTo("해당 유저 정보를 찾을 수 없습니다.");

    }
    @Test
    void findOneById_성공(){
        //given
        User user = User.createUser("user1", "123", AuthProvider.google, "123");
        given(userRepository.findById(any()))
                .willReturn(Optional.of(user));
        //when
        User findUser = userService.findById(user.getId());

        //then
        assertThat(findUser).isNotNull();

    }
    @Test
    void findOneById_실패(){
        //given
        given(userRepository.findById(any()))
                .willReturn(Optional.ofNullable(null));
        //when
        Exception exception  = Assertions.assertThrows(CustomException.class, () -> userService.findById(any()));

        //then
        assertEquals("해당 유저 정보를 찾을 수 없습니다.", exception.getMessage());

    }

    @Test
    @DisplayName("getSetting메서드는 유저 설정 정보를 조회한다")
    void getSetting() {
        //given
        String profile = "profile";
        long userId = 1l;
        given(userRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(userTechStackRepository.findAllByUser(any(User.class)))
            .willReturn(List.of(userTechStack));
        given(cloudinaryUtil.findFileURL(anyString()))
            .willReturn(profile);

        //when
        SettingResponseDto result = userService.getSetting(userId);

        //then
        assertThat(result.getNickname()).isEqualTo(user.getNickname());
        assertThat(result.getProfile()).isEqualTo(profile);
        assertThat(result.getTags().size()).isEqualTo(1);
    }
}
