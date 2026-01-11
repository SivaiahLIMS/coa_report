package com.stability.coareport.controller;

import com.stability.coareport.dto.CommentDto;
import com.stability.coareport.entity.User;
import com.stability.coareport.repository.UserRepository;
import com.stability.coareport.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    public CommentController(CommentService commentService, UserRepository userRepository) {
        this.commentService = commentService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> addComment(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long reportId = Long.valueOf(request.get("reportId").toString());
            String commentText = request.get("commentText").toString();
            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CommentDto comment = commentService.addComment(reportId, user.getId(), commentText);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<List<CommentDto>> getCommentsByReportId(@PathVariable Long reportId) {
        List<CommentDto> comments = commentService.getCommentsByReportId(reportId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/report/{reportId}/count")
    public ResponseEntity<Map<String, Long>> getCommentCount(@PathVariable Long reportId) {
        long count = commentService.getCommentCount(reportId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            commentService.deleteComment(commentId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
