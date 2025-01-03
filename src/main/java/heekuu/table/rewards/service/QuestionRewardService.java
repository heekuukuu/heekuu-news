package heekuu.table.rewards.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionRewardService {

  private final RewardService rewardService;


  public void rewardForQuestion(Long userId) {
    int rewardPoints = 5; // 보상 포인트를 동적으로 가져오도록 설정 가능
    String actionDescription = "질문 등록";

    rewardService.addReward(userId, actionDescription, rewardPoints);
  }

  public void rewardForAcceptedAnswer(Long userId) {
    int rewardPoints = 20; // 답변 채택 보상 포인트
    String actionDescription = "답변 채택";

    rewardService.addReward(userId, actionDescription, rewardPoints);
  }

}