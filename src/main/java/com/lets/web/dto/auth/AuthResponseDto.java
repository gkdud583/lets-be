package com.lets.web.dto.auth;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class AuthResponseDto {
  private final String nickname;
  private final String accessToken;
  private final String tokenType;
  private final String message;

  public static AuthResponseDto from(
      String nickname,
      String accessToken,
      String message
  ) {
    return AuthResponseDto
        .builder()
        .nickname(nickname)
        .accessToken(accessToken)
        .message(message)
        .build();
  }
}
