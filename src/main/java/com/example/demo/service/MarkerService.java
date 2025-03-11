package com.example.demo.service;

import com.example.demo.mapper.MarkerMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class MarkerService {
    private final MarkerMapper markerMapper;

    public MarkerService(MarkerMapper markerMapper) {
        this.markerMapper = markerMapper;
    }

    // ✅ 마커 등록 요청 (pending_markers에 저장)
    public void requestMarker(Map<String, Object> markerData) {
        markerMapper.insertPendingMarker(markerData);
    }

    // ✅ 관리자 - 등록 요청된 마커 리스트 조회
    public List<Map<String, Object>> getPendingMarkers() {
        return markerMapper.selectPendingMarkers();
    }

    // ✅ 관리자 - 마커 승인 (pending_markers → markers 이동)
    public void approveMarker(int id) {
        Map<String, Object> marker = markerMapper.getPendingMarkerById(id);
        if (marker != null) {
            markerMapper.insertMarker(marker);
            markerMapper.deletePendingMarker(id);
        }
    }

    // ✅ 승인된 전체 마커 조회
    public List<Map<String, Object>> getMarkers() {
        return markerMapper.selectMarkers();
    }

    // ✅ 특정 마커 조회
    public Map<String, Object> getMarkerById(int id) {
        return markerMapper.selectMarkerById(id);
    }

    // ✅ 관리자 - 마커 삭제 (pending_markers 또는 markers)
    public void deleteMarker(int id) {
        markerMapper.deleteMarker(id);
        markerMapper.deletePendingMarker(id);
    }
}
