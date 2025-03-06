package com.example.demo.controller;

import com.example.demo.service.MarkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markers")
public class MarkerController {
    private final MarkerService markerService;

    public MarkerController(MarkerService markerService) {
        this.markerService = markerService;
    }

    // ✅ 마커 저장 후 전체 데이터 반환
    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> addMarker(@RequestBody Map<String, Object> markerData) {
        System.out.println("저장할 데이터 확인: " + markerData);
        markerService.addMarker(markerData);
        return ResponseEntity.ok(markerService.getMarkers());
    }

    // ✅ 전체 마커 리스트 조회
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMarkers() {
        return ResponseEntity.ok(markerService.getMarkers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMarkerDetail(@PathVariable("id") Integer id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 ID"));
            }

            System.out.println("📌 요청된 마커 ID: " + id);  // ✅ 서버에서 요청 확인

            Map<String, Object> marker = markerService.getMarkerById(id);

            if (marker == null) {
                return ResponseEntity.status(404).body(Map.of("error", "해당 ID의 마커를 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(marker);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "마커 상세 조회 중 오류 발생", "message", e.getMessage()));
        }
    }

    // ✅ 특정 마커 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<List<Map<String, Object>>> deleteMarker(@PathVariable("id") Integer id) {
        markerService.deleteMarker(id);
        return ResponseEntity.ok(markerService.getMarkers()); // 삭제 후 전체 데이터 반환
    }
}
