package heekuu.table.OAuth.service;

import heekuu.table.OAuth.dto.GoogleResponse;
import heekuu.table.OAuth.dto.KakaoResponse;
import heekuu.table.OAuth.dto.NaverResponse;
import heekuu.table.OAuth.dto.OAuth2Response;

import heekuu.table.user.entity.User;
import heekuu.table.user.repository.UserRepository;
import heekuu.table.user.type.LoginType;
import heekuu.table.user.type.Role;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserServiceImpl extends DefaultOAuth2UserService {

  private final UserRepository userRepository;



  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    String provider = userRequest.getClientRegistration().getRegistrationId();
    OAuth2Response oAuth2Response;

    switch (provider) {
      case "naver":
        oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        break;
      case "kakao":
        oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
        break;
      case "google":
      default:
        oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        break;
    }

    String email = oAuth2Response.getEmail();
    String actualUsername = oAuth2Response.getName();

    // 이미 존재하는 사용자 처리
    Optional<User> existingUser = userRepository.findByEmail(email);
    if (existingUser.isPresent()) {
      LoginType existingLoginType = existingUser.get().getLoginType();
      LoginType currentLoginType = LoginType.valueOf(provider.toUpperCase());
      if (!existingLoginType.equals(currentLoginType)) {
        throw new IllegalArgumentException("이미 " + existingLoginType + " 계정으로 가입된 이메일입니다.");
      }
    }

    // 사용자 생성 또는 로드
    User user = userRepository.findByProviderId(oAuth2Response.getProviderId())
        .orElseGet(() -> createUser(oAuth2Response, provider));

    // 권한 설정
    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());

    // 속성에 추가 정보 삽입
    Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
    attributes.put("email", email);
    attributes.put("name", actualUsername);
    attributes.put("provider", provider);
    attributes.put("providerId", oAuth2Response.getProviderId());

    // OAuth2User 반환
    return new DefaultOAuth2User(
        Collections.singleton(authority),
        attributes,
        "email"
    );
  }

  private User createUser(OAuth2Response oAuth2Response, String provider) {
    User newUser = new User();
    newUser.setEmail(oAuth2Response.getEmail());
    newUser.setNickname(oAuth2Response.getName());
    newUser.setProviderId(oAuth2Response.getProviderId());
    newUser.setLoginType(LoginType.valueOf(provider.toUpperCase()));
    newUser.setRole(Role.USER);
    newUser.setUsername(generateUniqueUsername(oAuth2Response.getEmail()));
    return userRepository.save(newUser);
  }
   // 중복된 아이디 X
  private String generateUniqueUsername(String email) {
    String baseUsername = email.split("@")[0];
    String username = baseUsername;
    int counter = 1;
    while (userRepository.existsByUsername(username)) {
      username = baseUsername + counter;
      counter++;
    }
    return username;
  }
}