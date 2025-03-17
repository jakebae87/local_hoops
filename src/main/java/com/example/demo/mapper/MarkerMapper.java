package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface MarkerMapper {
    // ✅ 마커 등록 요청 (pending_markers 테이블에 저장)
    void insertPendingMarker(Map<String, Object> markerData);

    // ✅ 등록 요청된 마커 리스트 조회 (관리자가 승인하기 전 리스트)
    List<Map<String, Object>> selectPendingMarkers();

    // ✅ 특정 등록 요청된 마커 조회 (승인 또는 거절할 때 사용)
    Map<String, Object> getPendingMarkerById(int id);

    // ✅ 마커 승인 (pending_markers → markers 테이블로 이동)
    void insertMarker(Map<String, Object> markerData);

    // ✅ 등록 요청 삭제 (승인되지 않은 요청을 거절할 때 사용)
    void deletePendingMarker(int id);

    // ✅ 전체 승인된 마커 리스트 조회
    List<Map<String, Object>> selectMarkers();

    // ✅ 특정 마커 상세 조회
    Map<String, Object> getMarkerById(int id);

    // ✅ 특정 마커 삭제
    void deleteMarker(int id);

	Map<String, Object> selectMarkerById(int id);

	List<Map<String, Object>> getPendingMarkers();

	List<Map<String, Object>> getMarkers();

	void deleteRequestdMarker(Integer id);

    List<Map<String, Object>> findMarkersWithinRadius(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon
    );
}
