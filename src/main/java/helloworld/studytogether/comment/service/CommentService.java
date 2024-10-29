package helloworld.studytogether.comment.service;

import helloworld.studytogether.answer.entity.Answer;
import helloworld.studytogether.answer.repository.AnswerRepository;
import helloworld.studytogether.comment.entity.Comment;
import helloworld.studytogether.comment.repository.CommentRepository;
import helloworld.studytogether.user.entity.Count;
import helloworld.studytogether.user.entity.User;
import helloworld.studytogether.user.repository.CountRepository;
import helloworld.studytogether.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final CountRepository countRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository, AnswerRepository answerRepository, UserRepository userRepository, CountRepository countRepository) {
        this.commentRepository = commentRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.countRepository = countRepository;
    }

    public Comment createComment(Long answerId, Long userId, String content, Long parentCommentId) {
        Comment parentComment = null;
        if (parentCommentId != null) {
            parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        }

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Comment comment = new Comment(answer, user, content, parentComment);  // 생성자 사용

        Comment savedComment = commentRepository.save(comment);
        updateCommentCount(userId);

        return savedComment;
    }

    public List<Comment> getCommentsForAnswer(Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
        return commentRepository.findByAnswerAndParentCommentIsNull(answer);
    }

    public List<Comment> getRepliesForComment(Long commentId) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        return commentRepository.findByParentComment(parentComment);
    }

    // 댓글 수정 메서드
    public Comment updateComment(Long commentId, Long userId, String content) {
        // commentId로 해당 댓글을 찾음
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        // 댓글 작성자 확인
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 댓글만 수정할 수 있습니다.");
        }

        // 댓글 내용 수정 (setter 대신 비즈니스 메서드 사용)
        comment.updateContent(content);

        return commentRepository.save(comment); // 수정된 댓글 저장
    }

    // 댓글 삭제
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("본인의 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    private void updateCommentCount(Long userId) {
        // userId로 Count 조회
        Count userCount = countRepository.findByUser_UserId(userId);
        userCount.setCommentCount(userCount.getCommentCount() + 1);  // 댓글 수 증가
        countRepository.save(userCount);
    }

    public Comment createReply(Long parentCommentId, Long userId, String content) {
        // 부모 댓글 조회
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Comment replyComment = new Comment(parentComment.getAnswer(), user, content, parentComment);
        return commentRepository.save(replyComment);
    }
}
