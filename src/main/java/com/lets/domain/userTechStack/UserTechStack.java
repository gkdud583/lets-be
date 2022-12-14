package com.lets.domain.userTechStack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lets.domain.BaseTimeEntity;
import com.lets.domain.tag.Tag;
import com.lets.domain.user.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class UserTechStack extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_tech_stack_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id")
  private Tag tag;

  private UserTechStack(
      Tag tag,
      User user
  ) {
    this.user = user;
    this.tag = tag;
  }

  public static UserTechStack createUserTechStack(
      Tag tag,
      User user
  ) {
    UserTechStack userTechStack = new UserTechStack(tag, user);

    return userTechStack;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
