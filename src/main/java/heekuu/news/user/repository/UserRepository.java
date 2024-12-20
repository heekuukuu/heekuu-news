package heekuu.news.user.repository;

import heekuu.news.user.entity.User;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 소셜로그인사용자
    Optional<User> findByEmail(String email); // 이메일로 사용자 검색
    Optional<User> findByProviderId(String providerId); // 소셜 로그인용 provider ID로 검색

    boolean existsByUsername(String username);

    //boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    Optional<User> findByUserId(Long userId);// 메서드 이름을 카멜케이스로 변경
    Optional<User> findByUsername(String username);



}