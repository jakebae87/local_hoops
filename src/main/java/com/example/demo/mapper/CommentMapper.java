package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper {
    void insertComment(@Param("markerId") int markerId, @Param("content") String content);

    List<Map<String, Object>> getCommentsByMarkerId(@Param("markerId") int markerId,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
