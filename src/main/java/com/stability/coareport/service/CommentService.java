package com.stability.coareport.service;

import com.stability.coareport.dto.CommentDto;
import com.stability.coareport.entity.Report;
import com.stability.coareport.entity.ReportComment;
import com.stability.coareport.entity.User;
import com.stability.coareport.repository.ReportCommentRepository;
import com.stability.coareport.repository.ReportRepository;
import com.stability.coareport.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final ReportCommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public CommentService(ReportCommentRepository commentRepository,
                          ReportRepository reportRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentDto addComment(Long reportId, Long userId, String commentText) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        ReportComment comment = new ReportComment();
        comment.setReport(report);
        comment.setUser(user);
        comment.setUsername(user.getUsername());
        comment.setCommentText(commentText);

        ReportComment savedComment = commentRepository.save(comment);

        return convertToDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByReportId(Long reportId) {
        List<ReportComment> comments = commentRepository.findByReportIdOrderByCreatedAtDesc(reportId);
        return comments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getCommentCount(Long reportId) {
        return commentRepository.countByReportId(reportId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ReportComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private CommentDto convertToDto(ReportComment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getReport().getId(),
                comment.getUser().getId(),
                comment.getUsername(),
                comment.getCommentText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
