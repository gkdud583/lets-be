package com.lets.web;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import javax.servlet.http.Cookie;
import javax.validation.constraints.Null;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.lets.domain.tag.TagRepository;
import com.lets.domain.user.User;
import com.lets.domain.user.UserRepository;
import com.lets.domain.userTechStack.UserTechStackRepository;
import com.lets.exception.ErrorResponse;
import com.lets.security.AuthProvider;
import com.lets.security.JwtAuthentication;
import com.lets.security.JwtTokenProvider;
import com.lets.security.UserPrincipal;
import com.lets.util.CloudinaryUtil;
import com.lets.util.CookieUtil;
import com.lets.util.RedisUtil;
import com.lets.web.dto.ApiResponseDto;
import com.lets.web.dto.auth.AuthResponseDto;
import com.lets.web.dto.auth.LoginRequestDto;
import com.lets.web.dto.auth.SignupRequestDto;
import com.lets.web.dto.auth.SignupResponseDto;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerTest {
  @LocalServerPort
  private int port;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TagRepository tagRepository;

  @Autowired
  private UserTechStackRepository userTechStackRepository;

  @Autowired
  private CloudinaryUtil cloudinaryUtil;

  @Autowired
  private CookieUtil cookieUtil;

  @Autowired
  private RedisUtil redisUtil;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private TestRestTemplate testRestTemplate;

  private User user;
  private UserPrincipal principal;
  private Authentication authentication;
  private String accessToken = "Bearer ";
  private String refreshToken;
  private Cookie refreshTokenCookie;

  @BeforeEach
  void before() {
    user = User.createUser("test1", "1234", AuthProvider.google, "default");

    userRepository.save(user);

    principal = UserPrincipal.create(user);

    authentication = new JwtAuthentication(principal);
    accessToken += jwtTokenProvider.generateRefreshToken(authentication);
    refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
    refreshTokenCookie = cookieUtil.createCookie("refreshToken", refreshToken);
  }

  @AfterEach
  void after() {
    userTechStackRepository.deleteAllInBatch();
    tagRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("getAccessToken???????????? accessToken??? ???????????????")
  void getAccessToken() {
    //given
    redisUtil.setData(refreshToken, String.valueOf(user.getId()));

    String url = "http://localhost:" + port + "/api/auth/silent-refresh";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "refreshToken=" + refreshTokenCookie.getValue());

    //when
    ResponseEntity<AuthResponseDto> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        AuthResponseDto.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(!responseEntity
        .getBody()
        .getAccessToken()
        .isBlank());

  }

  @Test
  @DisplayName("getAccessToken???????????? refreshToken??? ???????????? ???????????? 400??? ????????????")
  void getAccessTokenWithNonexistentRefreshToken() {
    //given
    String url = "http://localhost:" + port + "/api/auth/silent-refresh";

    HttpHeaders headers = new HttpHeaders();

    //when
    ResponseEntity<ErrorResponse> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        ErrorResponse.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity
                   .getBody()
                   .getMessage()).isEqualTo("????????? REFRESH_TOKEN??? ???????????? ????????????.");

  }

  @Test
  @DisplayName("getAccessToken???????????? ????????? ???????????? 401??? ????????????")
  void getAccessTokenWithNonexistentUser() {
    //given
    userRepository.delete(user);

    String url = "http://localhost:" + port + "/api/auth/silent-refresh";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "refreshToken=" + refreshTokenCookie.getValue());

    //when
    ResponseEntity<ErrorResponse> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        ErrorResponse.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(responseEntity
                   .getBody()
                   .getMessage()).isEqualTo("REFRESH_TOKEN??? ???????????? ????????????.");
  }

  @Test
  @DisplayName("getAccessToken???????????? ???????????? ?????? refreshToken????????? 401??? ????????????")
  void getAccessTokenWithInvalidRefreshToken() {
    //given

    String refreshToken = "ABC";
    Cookie refreshTokenCookie = cookieUtil.createCookie("refreshToken", refreshToken);

    String url = "http://localhost:" + port + "/api/auth/silent-refresh";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "refreshToken=" + refreshTokenCookie.getValue());

    //when
    ResponseEntity<ErrorResponse> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        ErrorResponse.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(responseEntity
                   .getBody()
                   .getMessage()).isEqualTo("REFRESH_TOKEN??? ???????????? ????????????.");

  }

  @Test
  @DisplayName("validateNickname???????????? ?????? ?????????????????? 409??? ????????????")
  void validateNicknameWithDuplicateNickname() {
    //given
    String url = "http://localhost:" + port + "/api/auth/exists?nickname=" + user.getNickname();

    //when
    ResponseEntity<ErrorResponse> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        ErrorResponse.class
    );

    //then
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res
                   .getBody()
                   .getMessage()).isEqualTo("????????? ??????????????????.");
  }

  @Test
  @DisplayName("validateNickname???????????? ?????? ???????????? ???????????? 200??? ????????????")
  void validateNickname() {
    //given
    String url = "http://localhost:" + port + "/api/auth/exists?nickname=user1";

    //when
    ResponseEntity<ApiResponseDto> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        ApiResponseDto.class
    );

    //then
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("signup???????????? ?????? ??????????????? 409??? ????????????")
  void signupWithDuplicate() {
    //given
    SignupRequestDto signupRequest = new SignupRequestDto(
        "PUBLIC",
        user.getNickname(),
        user.getSocialLoginId(),
        user.getAuthProvider(),
        Arrays.asList("spring")
    );

    HttpEntity<SignupRequestDto> requestEntity = new HttpEntity<>(signupRequest);
    String url = "http://localhost:" + port + "/api/auth/signup";

    //when
    ResponseEntity<ErrorResponse> res = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        requestEntity,
        ErrorResponse.class
    );

    //then
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res
                   .getBody()
                   .getMessage()).isEqualTo("?????? ????????? ????????? ????????????.");
  }

  @Test
  @DisplayName("signup???????????? ????????? ??????????????????")
  void signup() {
    //given
    String fileFullPath = "./src/test/java/com/lets/tea.jpg";

    File file = new File(fileFullPath);

    String encodedImage = null;
    try {
      FileInputStream fis = new FileInputStream(file);
      encodedImage = Base64.encodeBase64String(fis.readAllBytes());
    } catch (Exception e) {
      throw new RuntimeException();
    }

    SignupRequestDto signupRequest = new SignupRequestDto(
        encodedImage,
        "user1",
        "123",
        AuthProvider.google,
        Arrays.asList("spring")
    );

    HttpEntity<SignupRequestDto> requestEntity = new HttpEntity<>(signupRequest);
    String url = "http://localhost:" + port + "/api/auth/signup";

    //when
    ResponseEntity<SignupResponseDto> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        requestEntity,
        SignupResponseDto.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity
                   .getBody()
                   .getNickname()
                   .equals(user.getNickname()));
    assertThat(responseEntity
                   .getBody()
                   .getProfile()).doesNotContain("default");

  }

  @Test
  @DisplayName("signup???????????? ????????? ??????????????????")
  void signup1() {
    //given
    String nickName = "test2";
    SignupRequestDto signupRequest = new SignupRequestDto(
        "PUBLIC",
        nickName,
        "12345",
        AuthProvider.google,
        Arrays.asList("spring")
    );

    HttpEntity<SignupRequestDto> requestEntity = new HttpEntity<>(signupRequest);
    String url = "http://localhost:" + port + "/api/auth/signup";

    //when
    ResponseEntity<SignupResponseDto> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        requestEntity,
        SignupResponseDto.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseEntity
                   .getBody()
                   .getNickname()).isEqualTo(nickName);
    assertThat(responseEntity
                   .getBody()
                   .getProfile()
                   .contains("default"));

  }

  @Test
  @DisplayName("login???????????? ????????? ?????????????????????")
  void login() {
    //given
    LoginRequestDto loginRequestDto = new LoginRequestDto(
        user.getSocialLoginId(),
        user.getAuthProvider()
    );

    HttpEntity<LoginRequestDto> requestEntity = new HttpEntity<>(loginRequestDto);
    String url = "http://localhost:" + port + "/api/auth/signin";

    //when
    ResponseEntity<AuthResponseDto> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        requestEntity,
        AuthResponseDto.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(!responseEntity
        .getBody()
        .getAccessToken()
        .isBlank());
    assertThat(!responseEntity
        .getHeaders()
        .getFirst(HttpHeaders.SET_COOKIE)
        .isBlank());

  }

  @Test
  @DisplayName("logout???????????? refreshToken??? ????????? 400??? ????????????")
  void logoutWithNotRefreshToken() {
    //given

    String url = "http://localhost:" + port + "/api/auth/logout";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    //when
    ResponseEntity<ErrorResponse> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        ErrorResponse.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity
                   .getBody()
                   .getMessage()).isEqualTo("????????? REFRESH_TOKEN??? ???????????? ????????????.");
  }

  @Test
  @DisplayName("logout???????????? ????????? ???????????? ????????????")
  void logout() {
    //given
    String url = "http://localhost:" + port + "/api/auth/logout";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);
    headers.add("Cookie", "refreshToken=" + refreshTokenCookie.getValue());


    //when
    ResponseEntity<Null> response = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Null.class
    );

    //then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("signout???????????? refreshToken??? ?????? ?????? 400??? ????????????")
  void signoutWithNonexistentRefreshToken() {
    //given

    String url = "http://localhost:" + port + "/api/auth/signout";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    //when
    ResponseEntity<ErrorResponse> responseEntity = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        ErrorResponse.class
    );

    //then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity
                   .getBody()
                   .getMessage()).isEqualTo("????????? REFRESH_TOKEN??? ???????????? ????????????.");
  }

  @Test
  @DisplayName("signout???????????? ????????? ??????????????????.")
  void signout() {
    //given
    String url = "http://localhost:" + port + "/api/auth/signout";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);
    headers.add("Cookie", "refreshToken=" + refreshTokenCookie.getValue());

    //when
    ResponseEntity<Null> response = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Null.class
    );

    //then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("signout???????????? ????????? ??????????????????.")
  void signout1() {
    //given
    String fileFullPath = "./src/test/java/com/lets/tea.jpg";

    File file = new File(fileFullPath);

    String publicId = cloudinaryUtil.saveFile(file);

    user = User.createUser("user1", "1234", AuthProvider.google, publicId);

    userRepository.save(user);

    principal = UserPrincipal.create(user);

    authentication = new JwtAuthentication(principal);
    accessToken = "Bearer " + jwtTokenProvider.generateRefreshToken(authentication);
    refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
    refreshTokenCookie = cookieUtil.createCookie("refreshToken", refreshToken);

    String url = "http://localhost:" + port + "/api/auth/signout";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);
    headers.add("Cookie", "refreshToken=" + refreshTokenCookie.getValue());

    //when
    ResponseEntity<Null> response = testRestTemplate.exchange(
        url,
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Null.class
    );

    //then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
