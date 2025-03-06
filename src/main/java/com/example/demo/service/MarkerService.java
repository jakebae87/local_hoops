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

    public void addMarker(Map<String, Object> markerData) {
        markerMapper.insertMarker(markerData);
    }

    public List<Map<String, Object>> getMarkers() {
        return markerMapper.selectMarkers();
    }

    // ✅ 특정 마커 상세 조회
    public Map<String, Object> getMarkerById(int id) {
        return markerMapper.getMarkerById(id);
    }

    // ✅ 특정 마커 삭제
    public void deleteMarker(int id) {
        markerMapper.deleteMarker(id);
    }
}
