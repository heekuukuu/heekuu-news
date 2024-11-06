package heekuu.news.user.service;


import static heekuu.news.user.entity.LoginType.GENERAL;

import heekuu.news.user.dto.JoinDTO;
import heekuu.news.user.entity.Role;
import heekuu.news.user.entity.User;
import heekuu.news.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class JoinService {

  @Autowired
  private final UserRepository userRepository;

  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  public JoinService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {

    this.userRepository = userRepository;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;

  }

  public void joinProcess(JoinDTO joinDTO) {
    String username = joinDTO.getUsername();
    String password = joinDTO.getPassword();
    String email = joinDTO.getEmail();
    String nickname = joinDTO.getNickname();

    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("비밀번호는 필수 입력 사항입니다.");
    }
    boolean isUsernameExist = userRepository.existsByUsername(username);
    boolean isEmailExist = userRepository.existsByEmail(email);
    //boolean isNicknameExist = userRepository.existsByNickname(nickname);

    if (isUsernameExist) {
      throw new RuntimeException("이미 등록된 아이디입니다.");
    }
    if (isEmailExist) {
      throw new RuntimeException("이미 등록된 이메일입니다.");
    }
//    if (isNicknameExist) {
//      throw new RuntimeException("이미 등록된 닉네임입니다.");
//    }
    User data = new User();
    data.setUsername(username);
    data.setPassword(bCryptPasswordEncoder.encode(password));
    data.setEmail(email);
    data.setNickname(nickname);
    data.setRole(Role.USER);
    data.setLoginType(GENERAL);

    userRepository.save(data);
  }
}