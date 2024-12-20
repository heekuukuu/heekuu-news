package heekuu.news.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import heekuu.news.common.exception.CustomException;
import heekuu.news.common.exception.ErrorCode;
import heekuu.news.jwt.util.JWTUtil;
import heekuu.news.token.entity.RefreshToken;
import heekuu.news.token.repository.RefreshTokenRepository;
import heekuu.news.user.dto.CustomUserDetails;
import heekuu.news.user.dto.LoginDTO;
import heekuu.news.user.entity.User;
import heekuu.news.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

  private final JWTUtil jwtUtil;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

  public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil,
      RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
    super.setAuthenticationManager(authenticationManager);
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.setFilterProcessesUrl("/users/login");
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
      HttpServletResponse response) {
    try {
      BufferedReader reader = request.getReader();
      StringBuilder json = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        json.append(line);
      }

      ObjectMapper mapper = new ObjectMapper();
      LoginDTO loginDTO = mapper.readValue(json.toString(), LoginDTO.class);

      String username = loginDTO.getUsername();
      String password = loginDTO.getPassword();

      if (username == null || username.isEmpty()) {
        log.warn("로그인 실패: 아이디가 비어있습니다.");
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, HttpStatus.BAD_REQUEST);
      }

      UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
          username, password);
      return this.getAuthenticationManager().authenticate(authRequest);
    } catch (IOException e) {
      log.error("로그인 요청 데이터 처리 중 오류 발생: {}", e.getMessage());
      try {
        handleException(response, new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      return null;
    } catch (AuthenticationException ex) {
      log.warn("잘못된 로그인 시도: {}", ex.getMessage());
      try {
        handleException(response, new CustomException(ErrorCode.INVALID_CREDENTIALS));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return null;
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain, Authentication authentication) throws IOException {
    try {
      CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
      User user = userDetails.getUser();
      Long userId = user.getUserId();
      Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

      if (user == null || userId == null) {
        log.error("User 객체가 null이거나 userId가 설정되지 않았습니다.");
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
      }

      boolean refreshTokenExists = refreshTokenRepository.existsByUser_UserId(userId);
      if (refreshTokenExists) {
        log.warn("중복된 로그인 시도가 감지되었습니다. 사용자 ID: {}", userId);
        throw new CustomException(ErrorCode.USER_ALREADY_EXISTS, ErrorCode.USER_ALREADY_EXISTS.getStatus());
      }

      String role = authorities.iterator().next().getAuthority();
      if (role == null) {
        log.error("User 권한 정보가 null입니다.");
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
      }

      refreshTokenRepository.deleteByUserId(userId);
      String accessToken = jwtUtil.createJwt("access", user, role,
          Duration.ofMinutes(10).toMillis());
      String refreshToken = jwtUtil.createJwt("refresh", user, role, Duration.ofDays(7).toMillis());

      log.info("Access Token 생성 완료: {}", accessToken);
      log.info("Refresh Token 생성 완료: {}", refreshToken);

      addRefreshToken(user, refreshToken, Duration.ofDays(7).toMillis());

      Map<String, String> tokens = new HashMap<>();
      tokens.put("accessToken", accessToken);

      response.addCookie(createCookie("refresh", refreshToken));
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      new ObjectMapper().writeValue(response.getWriter(), tokens);
      response.setStatus(HttpStatus.OK.value());
    } catch (CustomException ex) {
      handleException(response, ex);
    } catch (Exception e) {
      log.error("예기치 않은 오류 발생: {}", e.getMessage());
      handleException(response,
          new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }
  }

  private void handleException(HttpServletResponse response, CustomException ex)
      throws IOException {
    response.setStatus(ex.getStatus().value());
    response.setContentType("application/json;charset=UTF-8");

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("status", ex.getStatus().value());
    responseBody.put("error", ex.getErrorCode().getMessage());

    String jsonResponse = new ObjectMapper().writeValueAsString(responseBody);
    response.getWriter().write(jsonResponse);
  }

  private void addRefreshToken(User user, String refreshToken, Long expiredMs) {
    Date expirationDate = new Date(System.currentTimeMillis() + expiredMs);

    RefreshToken refreshTokenEntity = new RefreshToken();
    refreshTokenEntity.setUser(user);
    refreshTokenEntity.setRefresh(refreshToken);
    refreshTokenEntity.setExpiration(expirationDate.toString());

    refreshTokenRepository.save(refreshTokenEntity);
  }

  private Cookie createCookie(String key, String value) {
    Cookie cookie = new Cookie(key, value);
    cookie.setMaxAge(24 * 60 * 60);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    return cookie;
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException failed) throws IOException {
    log.warn("로그인 실패: {}", failed.getMessage());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    Map<String, String> error = new HashMap<>();
    error.put("error", "Authentication failed");

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    new ObjectMapper().writeValue(response.getWriter(), error);
  }
}