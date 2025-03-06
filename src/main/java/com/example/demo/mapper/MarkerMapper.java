package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface MarkerMapper {
    void insertMarker(Map<String, Object> markerData);
    List<Map<String, Object>> selectMarkers();

    // ✅ 특정 마커 상세 조회
    Map<String, Object> getMarkerById(int id);

    // ✅ 특정 마커 삭제
    void deleteMarker(int id);
}
