package heekuu.table.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

// 어드민  업데이트페이지
public class AdminUpdateRequestDTO {



  private String nickname;
  private String password;
}