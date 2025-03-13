package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.mapper.MarkerMapper;

@Service
public class MarkerService {
    private final MarkerMapper markerMapper;

    public MarkerService(MarkerMapper markerMapper) {
        this.markerMapper = markerMapper;
    }

    // ✅ 마커 등록 요청 (pending_markers 테이블에 저장)
    public void requestMarker(String title, double latitude, double longitude, List<MultipartFile> images) {
        Map<String, Object> markerData = new HashMap<>();
        markerData.put("title", title);
        markerData.put("latitude", latitude);
        markerData.put("longitude", longitude);
        markerData.put("approved", false);

        if (images != null && !images.isEmpty()) {
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);

            // ✅ uploads 폴더가 없으면 생성
            if (!Files.exists(uploadPath)) {
                try {
                    Files.createDirectories(uploadPath);
                    System.out.println("📁 uploads 폴더 생성됨: " + uploadPath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("🚨 uploads 폴더 생성 실패: " + e.getMessage());
                    return;
                }
            }

            StringBuilder imagePaths = new StringBuilder();

            for (MultipartFile image : images) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // ✅ 저장된 파일 경로를 DB에 저장 (클라이언트에서 접근할 URL)
                    imagePaths.append("/uploads/").append(fileName).append(",");
                    System.out.println("📌 저장된 파일: " + filePath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("🚨 파일 저장 중 오류 발생: " + e.getMessage());
                }
            }

            markerData.put("image", imagePaths.length() > 0 ? imagePaths.substring(0, imagePaths.length() - 1) : null);
        } else {
            markerData.put("image", null);
        }

        markerMapper.insertPendingMarker(markerData);
    }

    // ✅ 관리자 - 등록 요청된 마커 리스트 조회
    public List<Map<String, Object>> getPendingMarkers() {
        return markerMapper.getPendingMarkers();
    }

    // ✅ 관리자 - 마커 승인 (pending_markers → markers 이동)
    public void approveMarker(int id) {
        Map<String, Object> marker = markerMapper.getPendingMarkerById(id);
        if (marker != null) {
            // ✅ 이미지 경로 유지
            markerMapper.insertMarker(marker);
            markerMapper.deletePendingMarker(id);
        }
    }

    // ✅ 마커 삭제 (관리자가 요청 거절)
    public void deleteMarker(int id) {
        markerMapper.deleteMarker(id);
    }

    // ✅ 승인된 마커 리스트 조회
    public List<Map<String, Object>> getMarkers() {
        List<Map<String, Object>> markers = markerMapper.getMarkers();
        for (Map<String, Object> marker : markers) {
            if (marker.get("image") != null) {
                List<String> imagePaths = List.of(marker.get("image").toString().split(","));
                List<String> fullUrls = imagePaths.stream()
                        .map(img -> "/uploads/" + img.trim()) // 이미지 URL 변환
                        .toList();
                marker.put("images", fullUrls);
            }
        }
        return markers;
    }

    // ✅ 특정 마커 상세 조회
    public Map<String, Object> getMarkerById(int id) {
        Map<String, Object> marker = markerMapper.getMarkerById(id);
        if (marker != null && marker.get("image") != null) {
            List<String> imagePaths = List.of(marker.get("image").toString().split(","));
            List<String> fullUrls = imagePaths.stream()
                    .map(img -> "/uploads/" + img.trim()) // 이미지 URL 변환
                    .toList();
            marker.put("images", fullUrls);
        }
        return marker;
    }

    // ✅ 승인 요청 마커 삭제
	public void deleteRequestdMarker(Integer id) {
		markerMapper.deletePendingMarker(id);
	}
}
