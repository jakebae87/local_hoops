package com.example.demo.controller;

import com.example.demo.service.MarkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markers")
public class MarkerController {
    private final MarkerService markerService;

    public MarkerController(MarkerService markerService) {
        this.markerService = markerService;
    }

    // ✅ 마커 등록 요청 (이미지 포함) → pending_markers 테이블에 저장
    @PostMapping(value = "/request", consumes = {"multipart/form-data"})
    public ResponseEntity<?> requestMarker(
    	    @RequestParam("title") String title,
    	    @RequestParam("latitude") double latitude,
    	    @RequestParam("longitude") double longitude,
    	    @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            System.out.println("📌 마커 등록 요청 - 제목: " + title + ", 위도: " + latitude + ", 경도: " + longitude + ", 이미지: " + images);

            if (images != null) {
                System.out.println("📌 업로드된 이미지 개수: " + images.size());
                for (MultipartFile file : images) {
                    System.out.println("📌 파일명: " + file.getOriginalFilename() + ", 크기: " + file.getSize() + " 바이트, 타입: " + file.getContentType());
                }
            }
            
            // ✅ 데이터 변환 및 처리
            markerService.requestMarker(title, latitude, longitude, images);
            return ResponseEntity.ok(Map.of("message", "마커 등록 요청 완료. 관리자의 승인을 기다려 주세요."));
        } catch (IllegalArgumentException e) { // ✅ 500m 내 중복 예외 처리
            return ResponseEntity.status(400).body(Map.of("error", "마커 등록 불가", "message", e.getMessage()));
        } catch (Exception e) { // ✅ 기타 예외 처리
            return ResponseEntity.status(500).body(Map.of("error", "마커 등록 요청 실패", "message", e.getMessage()));
        }
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
            return ResponseEntity.ok(Map.of("message", "마커 승인 완료."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "마커 승인 중 오류 발생", "message", e.getMessage()));
        }
    }

    // ✅ 관리자 - 승인 요청 마커 삭제 (pending_markers 삭제)
    @DeleteMapping("/reject/{id}")
    public ResponseEntity<?> deleteRequestdMarker(@PathVariable("id") Integer id) {
        try {
            markerService.deleteRequestdMarker(id);
            return ResponseEntity.ok(Map.of("message", "승인 요청 마커 삭제 완료."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "승인 요청 마커 삭제 중 오류 발생", "message", e.getMessage()));
        }
    }
    
    // ✅ 관리자 - 마커 삭제 (markers에서 삭제)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMarker(@PathVariable("id") Integer id) {
        try {
            markerService.deleteMarker(id);
            return ResponseEntity.ok(Map.of("message", "마커 삭제 완료."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "마커 삭제 중 오류 발생", "message", e.getMessage()));
        }
    }

    // ✅ 승인된 전체 마커 리스트 조회 (지도에 표시될 마커들)
    @GetMapping("/approve")
    public ResponseEntity<List<Map<String, Object>>> getMarkers() {
        return ResponseEntity.ok(markerService.getMarkers());
    }

    // ✅ 선택된 등록 마커 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getMarkerDetail(@PathVariable("id") Integer id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 ID"));
            }

            System.out.println("📌 요청된 마커 ID: " + id);

            Map<String, Object> marker = markerService.getMarkerById(id);
            System.out.println("marker: "+marker);

            if (marker == null) {
                return ResponseEntity.status(404).body(Map.of("error", "해당 ID의 마커를 찾을 수 없습니다."));
            }

            // ✅ 이미지 필드 변환 (String → List)
            if (marker.get("images") instanceof String) {
                String imagesStr = (String) marker.get("images");
                marker.put("images", List.of(imagesStr.split(",")));
            }

            return ResponseEntity.ok(marker);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "마커 상세 조회 중 오류 발생", "message", e.getMessage()));
        }
    }

}
