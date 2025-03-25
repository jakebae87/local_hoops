package com.example.demo.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.demo.mapper.CommentMapper;

@Service
public class CommentService {
    private final CommentMapper commentMapper;

    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public void addComment(int markerId, String content) {
        if (content.length() > 40) {
            throw new IllegalArgumentException("댓글은 40자 이하만 가능합니다.");
        }
        commentMapper.insertComment(markerId, content);
    }

    public List<Map<String, Object>> getComments(int markerId, int page, int size) {
        int offset = (page - 1) * size;
        return commentMapper.getCommentsByMarkerId(markerId, offset, size);
    }
}
