package com.lets.domain.post;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.lets.config.QueryDslConfig;
import com.lets.domain.user.User;
import com.lets.domain.user.UserRepository;
import com.lets.security.AuthProvider;

@DataJpaTest
@Import(QueryDslConfig.class)
public class PostRepositoryTest {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PostRepository postRepository;

  private User user;
  private Post post;

  @BeforeEach
  void setup() {
    user = User.createUser("user1", "123", AuthProvider.google, "default");
    userRepository.save(user);

    post = Post.createPost(user, "title1", "content1");
    postRepository.save(post);
  }

  @DisplayName("deleteAllById메서드는 id로 모든 글을 삭제한다")
  @Test
  public void deleteAllById() {
    //given

    //when
    postRepository.deleteAllById(Arrays.asList(post.getId()));

    //then
    assertThat(postRepository.count()).isEqualTo(0);
  }

  @DisplayName("findAllByUser메서드는 유저가 작성한 모든 글을 조회한다")
  @Test
  public void findAllByUser() {
    //given

    //when
    List<Post> result = postRepository.findAllByUser(user);

    //then
    assertThat(result.size()).isEqualTo(1);
    assertThat(result
                   .get(0)
                   .getTitle()).isEqualTo(post.getTitle());
  }

  @DisplayName("findOneById메서드는 id로 글 단건을 조회한다")
  @Test
  public void findOneById() {
    //given

    //when
    Optional<Post> result = postRepository.findOneById(post.getId());

    //then
    assertThat(result
                   .get()
                   .getTitle()).isEqualTo(post.getTitle());
  }
}
