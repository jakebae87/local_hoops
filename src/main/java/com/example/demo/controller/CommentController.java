package com.example.demo.controller;

import com.example.demo.service.CommentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody Map<String, Object> body) {
        int markerId = (int) body.get("markerId");
        String content = (String) body.get("content");
        System.out.println("markerId: " + markerId);
        System.out.println("content: " + content);
        commentService.addComment(markerId, content);
        return ResponseEntity.ok(Map.of("message", "댓글 등록 완료"));
    }

    @GetMapping("/{markerId}")
    public ResponseEntity<?> getComments(@PathVariable int markerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(commentService.getComments(markerId, page, size));
    }
}
