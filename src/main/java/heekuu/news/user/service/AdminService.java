package heekuu.news.user.service;

import heekuu.news.user.dto.AdminUpdateRequestDTO;
import heekuu.news.user.dto.UserResponseDTO;
import heekuu.news.user.entity.Role;
import heekuu.news.user.entity.User;
import heekuu.news.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

  private final UserRepository userRepository;

  public AdminService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  // 모든 사용자 조회 (Admin 전용)
  @PreAuthorize("hasAuthority('ADMIN')")
  public List<UserResponseDTO> getAllUsers() {
    List<User> users = userRepository.findAll();
    return users.stream().map(user -> {
      UserResponseDTO dto = new UserResponseDTO();
      //dto.setUserId(user.getUserId());
      dto.setUsername(user.getUsername());
      dto.setEmail(user.getEmail());
      dto.setNickname(user.getNickname());
      dto.setRole(user.getRole().toString());
      return dto;
    }).toList();
  }

  // 특정 사용자 정보 조회 (Admin 전용)
  @PreAuthorize("hasAuthority('ADMIN')")
  public UserResponseDTO getUserById(Long userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    if (userOptional.isEmpty()) {
      throw new RuntimeException("User not found with ID: " + userId);
    }
    User user = userOptional.get();
    UserResponseDTO dto = new UserResponseDTO();
    //dto.setUserId(user.getUserId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setNickname(user.getNickname());
    dto.setRole(user.getRole().toString());
    return dto;
  }

  // 사용자 권한 변경 (Admin 전용)
  @PreAuthorize("hasAuthority('ADMIN')")
  public UserResponseDTO updateUserRole(Long userId, String role) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    user.setRole(role.equals("ADMIN") ? Role.ADMIN : Role.USER);
    userRepository.save(user);

    UserResponseDTO dto = new UserResponseDTO();
    //dto.setUserId(user.getUserId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setNickname(user.getNickname());
    dto.setRole(user.getRole().toString());
    return dto;
  }

  public UserResponseDTO updateUserInfo(Long userId, AdminUpdateRequestDTO adminUpdateRequestDTO) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

    // 업데이트할 필드 설정

    // 업데이트할 필드 설정 (null이 아닐 경우에만 업데이트)
    if (adminUpdateRequestDTO.getEmail() != null) {
      user.setEmail(adminUpdateRequestDTO.getEmail());
    }

    if (adminUpdateRequestDTO.getNickname() != null) {
      user.setNickname(adminUpdateRequestDTO.getNickname());
    }

    userRepository.save(user); // 변경된 사용자 정보 저장

    // 수정된 사용자 정보를 반환
    UserResponseDTO userResponseDTO = new UserResponseDTO();
    //userResponseDTO.setUserId(user.getUserId());
    userResponseDTO.setUsername(user.getUsername());
    userResponseDTO.setEmail(user.getEmail());
    userResponseDTO.setNickname(user.getNickname());
    userResponseDTO.setRole(user.getRole().toString());

    return userResponseDTO;
  }
  // 사용자 삭제 (Admin 전용)
  @PreAuthorize("hasAuthority('ADMIN')")
  public void deleteUser(Long userId) {
    userRepository.deleteById(userId);

  }
}