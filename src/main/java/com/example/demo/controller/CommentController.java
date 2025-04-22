package com.example.demo.controller;

import com.example.demo.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        commentService.addComment(markerId, content);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "댓글 등록 완료");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{markerId}")
    public ResponseEntity<?> getComments(@PathVariable("markerId") int markerId,
                                         @RequestParam(name = "page", defaultValue = "1") int page,
                                         @RequestParam(name = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(commentService.getComments(markerId, page, size));
    }
}
