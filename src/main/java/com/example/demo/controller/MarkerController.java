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

    // ✅ 마커 등록 요청 (접속자가 마커 요청 시 pending_markers 테이블에 저장)
    @PostMapping("/request")
    public ResponseEntity<?> requestMarker(@RequestBody Map<String, Object> markerData) {
        System.out.println("마커 등록 요청: " + markerData);
        markerService.requestMarker(markerData);
        return ResponseEntity.ok("마커 등록 요청 완료. 관리자의 승인을 기다려 주세요.");
    }

    // ✅ 관리자 - 등록 요청된 마커 리스트 조회
    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> getPendingMarkers() {
        return ResponseEntity.ok(markerService.getPendingMarkers());
    }

    // ✅ 관리자 - 마커 승인 (pending_markers → markers 이동)
    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveMarker(@PathVariable("id") Integer id) {
        try {
            markerService.approveMarker(id);
            return ResponseEntity.ok("마커 승인 완료.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "마커 승인 중 오류 발생", "message", e.getMessage()));
        }
    }

    // ✅ 관리자 - 마커 삭제 (pending_markers 또는 markers에서 삭제)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMarker(@PathVariable("id") Integer id) {
        try {
            markerService.deleteMarker(id);
            return ResponseEntity.ok("마커 삭제 완료.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "마커 삭제 중 오류 발생", "message", e.getMessage()));
        }
    }

    // ✅ 승인된 전체 마커 리스트 조회 (지도에 표시될 마커들)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMarkers() {
        return ResponseEntity.ok(markerService.getMarkers());
    }

    // ✅ 특정 마커 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getMarkerDetail(@PathVariable("id") Integer id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 ID"));
            }

            System.out.println("📌 요청된 마커 ID: " + id);

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
}
