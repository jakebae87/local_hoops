package com.example.demo.controller;

import com.example.demo.service.MarkerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

@RestController
@RequestMapping("/api/markers")
public class MarkerController {
    private final MarkerService markerService;

    public MarkerController(MarkerService markerService) {
        this.markerService = markerService;
    }

    @PostMapping(value = "/request", consumes = { "multipart/form-data" })
    public ResponseEntity<?> requestMarker(@RequestParam("title") String title,
                                           @RequestParam("latitude") double latitude,
                                           @RequestParam("longitude") double longitude,
                                           @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            markerService.requestMarker(title, latitude, longitude, images);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "마커 등록 요청 완료. 관리자의 승인을 기다려 주세요.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "마커 등록 불가");
            error.put("message", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "마커 등록 요청 실패");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> getPendingMarkers() {
        return ResponseEntity.ok(markerService.getPendingMarkers());
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveMarker(@PathVariable("id") Integer id) {
        try {
            markerService.approveMarker(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "마커 승인 완료.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "마커 승인 중 오류 발생");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/reject/{id}")
    public ResponseEntity<?> deleteRequestdMarker(@PathVariable("id") Integer id) {
        try {
            markerService.deleteRequestdMarker(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "승인 요청 마커 삭제 완료.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "승인 요청 마커 삭제 중 오류 발생");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMarker(@PathVariable("id") Integer id) {
        try {
            markerService.deleteMarker(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "마커 삭제 완료.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "마커 삭제 중 오류 발생");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/approve")
    public ResponseEntity<List<Map<String, Object>>> getMarkers() {
        return ResponseEntity.ok(markerService.getMarkers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMarkerDetail(@PathVariable("id") Integer id) {
        try {
            if (id == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "유효하지 않은 ID");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> marker = markerService.getMarkerById(id);
            if (marker == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "해당 ID의 마커를 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(error);
            }

            Object imagesObj = marker.get("images");
            if (imagesObj instanceof String) {
                String imagesStr = (String) imagesObj;
                List<String> imageList = Arrays.asList(imagesStr.split(","));
                marker.put("images", imageList);
            }

            return ResponseEntity.ok(marker);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "마커 상세 조회 중 오류 발생");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
