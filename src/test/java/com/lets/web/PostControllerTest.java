package com.lets.web;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lets.domain.comment.Comment;
import com.lets.domain.comment.CommentRepository;
import com.lets.domain.likePost.LikePost;
import com.lets.domain.likePost.LikePostRepository;
import com.lets.domain.likePost.LikePostStatus;
import com.lets.domain.post.Post;
import com.lets.domain.post.PostRepository;
import com.lets.domain.post.PostStatus;
import com.lets.domain.postTechStack.PostTechStack;
import com.lets.domain.postTechStack.PostTechStackRepository;
import com.lets.domain.tag.Tag;
import com.lets.domain.tag.TagRepository;
import com.lets.domain.user.User;
import com.lets.domain.user.UserRepository;
import com.lets.security.AuthProvider;
import com.lets.security.JwtAuthentication;
import com.lets.security.JwtTokenProvider;
import com.lets.security.UserPrincipal;
import com.lets.service.likePost.LikePostService;
import com.lets.service.post.PostService;
import com.lets.service.postTechStack.PostTechStackService;
import com.lets.service.tag.TagService;
import com.lets.service.user.UserService;
import com.lets.util.CookieUtil;
import com.lets.util.RedisUtil;
import com.lets.web.dto.ApiResponseDto;
import com.lets.web.dto.likepost.ChangeLikePostStatusResponseDto;
import com.lets.web.dto.post.PostRecommendResponseDto;
import com.lets.web.dto.post.PostResponseDto;
import com.lets.web.dto.post.PostSaveRequestDto;
import com.lets.web.dto.post.PostUpdateRequestDto;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class PostControllerTest {
  private Long userId;
  @LocalServerPort
  private int port;

  @Autowired
  TagService tagService;

  @Autowired
  TagRepository tagRepository;

  @Autowired
  UserService userService;

  @Autowired
  UserRepository userRepository;

  @Autowired
  PostService postService;

  @Autowired
  PostRepository postRepository;

  @Autowired
  PostTechStackService postTechStackService;

  @Autowired
  PostTechStackRepository postTechStackRepository;

  @Autowired
  LikePostService likePostService;

  @Autowired
  LikePostRepository likePostRepository;

  @Autowired
  CommentRepository commentRepository;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  CookieUtil cookieUtil;

  @Autowired
  RedisUtil redisUtil;

  @SpyBean
  private JwtTokenProvider jwtTokenProvider;

  private User user;
  private User user2;
  private UserPrincipal principal;
  private Authentication authentication;
  private String accessToken = "Bearer ";
  private String refreshToken;
  private Cookie refreshTokenCookie;

  @BeforeEach
  void before() {
    user = User.createUser("user2", "123", AuthProvider.google, "default");
    user2 = User.createUser("user3", "321", AuthProvider.google, "default");
    userRepository.save(user);
    userRepository.save(user2);

    Tag tag1 = Tag.createTag("spring");
    Tag tag2 = Tag.createTag("java");
    Tag tag3 = Tag.createTag("python");
    Tag tag4 = Tag.createTag("c");

    tagRepository.save(tag1);
    tagRepository.save(tag2);
    tagRepository.save(tag3);
    tagRepository.save(tag4);

    Post post1 = Post.createPost(user, "title1", "content1");
    Post post2 = Post.createPost(user2, "title2", "content2");
    Post post3 = Post.createPost(user2, "title3", "content3");
    Post post4 = Post.createPost(user2, "title4", "content4");
    Post post5 = Post.createPost(user2, "title5", "content5");
    Post post6 = Post.createPost(user, "title6", "content6");
    post6.addView();

    postRepository.save(post1);
    postRepository.save(post2);
    postRepository.save(post3);
    postRepository.save(post4);
    postRepository.save(post5);
    postRepository.save(post6);
    likePostRepository.save(LikePost.createLikePost(user, post1));
    commentRepository.save(Comment.createComment(user, post1, "content1"));

    PostTechStack postTechStack1 = PostTechStack.createPostTechStack(tag1, post1);
    //검색할 대상
    PostTechStack postTechStack2 = PostTechStack.createPostTechStack(tag2, post2);
    PostTechStack postTechStack3 = PostTechStack.createPostTechStack(tag3, post2);
    PostTechStack postTechStack4 = PostTechStack.createPostTechStack(tag4, post2);
    //검색될 대상
    PostTechStack postTechStack5 = PostTechStack.createPostTechStack(tag2, post3);
    PostTechStack postTechStack6 = PostTechStack.createPostTechStack(tag3, post4);
    PostTechStack postTechStack7 = PostTechStack.createPostTechStack(tag4, post5);
    PostTechStack postTechStack8 = PostTechStack.createPostTechStack(tag4, post6);

    postTechStackService.save(postTechStack1);
    postTechStackService.save(postTechStack2);
    postTechStackService.save(postTechStack3);
    postTechStackService.save(postTechStack4);
    postTechStackService.save(postTechStack5);
    postTechStackService.save(postTechStack6);
    postTechStackService.save(postTechStack7);
    postTechStackService.save(postTechStack8);

    principal = UserPrincipal.create(user);

    authentication = new JwtAuthentication(principal);
    accessToken += jwtTokenProvider.generateRefreshToken(authentication);
    refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
    refreshTokenCookie = cookieUtil.createCookie("refreshToken", refreshToken);

  }

  @AfterEach
  void after() {
    commentRepository.deleteAllInBatch();
    postTechStackRepository.deleteAllInBatch();
    likePostRepository.deleteAllInBatch();
    postRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
    tagRepository.deleteAllInBatch();

  }

  @Test
  @DisplayName("findPost메서드는 아이디로 포스트를 조회한다")
  void findPost() {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();
    //LogIn
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    String url = "http://localhost:" + port + "/api/posts/" + id;
    ResponseEntity<PostResponseDto> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<PostResponseDto>() {
        }
    );

    assertThat(res
                   .getBody()
                   .getId()).isEqualTo(id);
    assertThat(res
                   .getBody()
                   .getViewCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("findPost메서드는 아이디로 포스트를 조회한다")
  void findPostWithoutLogin() {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();

    String url = "http://localhost:" + port + "/api/posts/" + id;
    ResponseEntity<PostResponseDto> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<PostResponseDto>() {
        }
    );

    assertThat(res
                   .getBody()
                   .getId()).isEqualTo(id);
    assertThat(res
                   .getBody()
                   .getViewCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("savePost메서드는 포스트 한건을 저장한다")
  void savePost() throws Exception {
    String title = "title444";
    String content = "content";
    List<String> tags = List.of("spring", "java");

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    //post등록
    String url = "http://localhost:" + port + "/api/posts";

    ObjectMapper objectMapper = new ObjectMapper();

    RequestEntity<PostSaveRequestDto> body = RequestEntity
        .post(new URI(url))
        .accept(MediaType.APPLICATION_JSON)
        .headers(headers)
        .body(new PostSaveRequestDto(title, content, tags));
    ResponseEntity<PostResponseDto> res = testRestTemplate.exchange(
        body,
        new ParameterizedTypeReference<PostResponseDto>() {
        }
    );
    System.out.println("_____________________________");
    System.out.println(res.getStatusCode());
    assertThat(res
                   .getBody()
                   .getTitle()).isEqualTo("title444");
  }

  @Test
  @DisplayName("savePost메서드는 존재하지 않는 유저라면 401을 반환한다")
  void 포스트_등록_유저정보없음() throws Exception {
    String title = "title444";
    String content = "content";
    List<String> tags = List.of("spring", "java");

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    //        headers.add("Authorization",  accessToken);

    //post등록
    String url = "http://localhost:" + port + "/api/posts";

    RequestEntity<PostSaveRequestDto> body = RequestEntity
        .post(new URI(url))
        .accept(MediaType.APPLICATION_JSON)
        .headers(headers)
        .body(new PostSaveRequestDto(title, content, tags));
    ResponseEntity<Object> res = testRestTemplate.exchange(body, Object.class);
    System.out.println("_____________________________");
    System.out.println(res.getStatusCode());
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("updatePost메서드는 포스트를 수정한다")
  void updatePost() throws Exception {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();

    String title = "title44";
    String content = "content44";
    List<String> tags = List.of("spring");

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    //post등록
    String url = "http://localhost:" + port + "/api/posts/" + id;

    ObjectMapper objectMapper = new ObjectMapper();

    RequestEntity<PostUpdateRequestDto> body = RequestEntity
        .put(new URI(url))
        .accept(MediaType.APPLICATION_JSON)
        .headers(headers)
        .body(new PostUpdateRequestDto(title, content, tags, PostStatus.RECRUITING));
    ResponseEntity<PostResponseDto> res = testRestTemplate.exchange(
        body,
        new ParameterizedTypeReference<PostResponseDto>() {
        }
    );
    System.out.println("_____________________________");
    System.out.println(res.getStatusCode());
    assertThat(res
                   .getBody()
                   .getTitle()).isEqualTo("title44");
  }

  @Test
  @DisplayName("updatePost메서드는 수정권한이 없는 유저라면 401을 반환한다")
  void updatePostWithUnauthorizedUser() throws Exception {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();

    String title = "title44";
    String content = "content44";
    List<String> tags = List.of("spring");

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    //        headers.add("Authorization",  accessToken);

    //post등록
    String url = "http://localhost:" + port + "/api/posts/" + id;
    RequestEntity<PostUpdateRequestDto> body = RequestEntity
        .put(new URI(url))
        .accept(MediaType.APPLICATION_JSON)
        .headers(headers)
        .body(new PostUpdateRequestDto(title, content, tags, PostStatus.RECRUITING));
    ResponseEntity<Object> res = testRestTemplate.exchange(body, Object.class);
    System.out.println("_____________________________");
    System.out.println(res.getStatusCode());
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("deletePost메서드는 포스트를 삭제한다")
  void deletePost() {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);
    headers.add("contentType", "application/json");

    //post등록
    String url = "http://localhost:" + port + "/api/posts/" + id;
    ResponseEntity<ApiResponseDto> res = testRestTemplate.exchange(
        url,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        ApiResponseDto.class
    );
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("deletePost메서드는 삭제 권한이 없는 유저라면 401을 반환한다")
  void deletePostWithUnauthorizedUser() {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    //        headers.add("Authorization",  accessToken);
    headers.add("contentType", "application/json");

    //post등록
    String url = "http://localhost:" + port + "/api/posts/" + id;
    ResponseEntity<ApiResponseDto> res = testRestTemplate.exchange(
        url,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        ApiResponseDto.class
    );
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("changeLikeStatus메서드는 좋아요 상태를 변경한다")
  void changeLikeStatus() throws URISyntaxException {
    Long id = postRepository
        .findAll()
        .get(0)
        .getId();

    //LogIn
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    //post등록
    String searchUrl = "http://localhost:" + port + "/api/posts/" + id;
    ResponseEntity<PostResponseDto> searchRes = testRestTemplate.exchange(
        searchUrl,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<PostResponseDto>() {
        }
    );

    String url = "http://localhost:" + port + "/api/posts/" + id + "/likes";

    RequestEntity<Long> body = RequestEntity
        .post(new URI(url))
        .headers(headers)
        .body(null);
    ResponseEntity<ChangeLikePostStatusResponseDto> res = testRestTemplate.exchange(
        body,
        ChangeLikePostStatusResponseDto.class
    );

    System.out.println("_____________________________");
    System.out.println(res.getStatusCode());
    assertThat(res
                   .getBody()
                   .getLikeCount()).isEqualTo(1);
    assertThat(res
                   .getBody()
                   .getLikePostStatus()).isEqualTo(LikePostStatus.ACTIVE);
  }

  @Test
  @DisplayName("searchPosts메서드는 조건에 맞는 글을 검색한다")
  void searchPosts() {
    //given
    String url = "http://localhost:" + port
        + "/api/posts/filter?status=RECRUITING&page=0&sort=createdDate,DESC&tags=spring,java";

    //when
    ResponseEntity<List<PostResponseDto>> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<List<PostResponseDto>>() {
        }
    );

    //then
    assertThat(res
                   .getBody()
                   .size()).isEqualTo(3);
  }

  @Test
  @DisplayName("recommendedPosts메서드는 추천 포스트 목록을 생성한다")
  void 추천포스트_조회() {
    //given
    String url = "http://localhost:" + port + "/api/posts/2/recommends?tags=java,c,python";
    //LogIn
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", accessToken);

    //when
    ResponseEntity<List<PostRecommendResponseDto>> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<List<PostRecommendResponseDto>>() {
        }
    );

    //then
    assertThat(res
                   .getBody()
                   .size()).isEqualTo(4);

    res
        .getBody()
        .stream()
        .forEach(r -> {
          System.out.println("r.getTitle() = " + r.getTitle());
        });
  }

  @Test
  @DisplayName("recommendedPosts메서드는 추천 포스트 목록을 생성한다")
  void recommendedPostsWithNonUser() {
    //given
    String url = "http://localhost:" + port + "/api/posts/2/recommends?tags=java,c,python";

    //when
    ResponseEntity<List<PostRecommendResponseDto>> res = testRestTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<List<PostRecommendResponseDto>>() {
        }
    );

    //then
    assertThat(res
                   .getBody()
                   .size()).isEqualTo(4);

    res
        .getBody()
        .stream()
        .forEach(r -> {
          System.out.println("r.getTitle() = " + r.getTitle());
        });
  }
}
