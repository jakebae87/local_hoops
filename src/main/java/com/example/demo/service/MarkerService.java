package com.example.demo.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final double EARTH_RADIUS = 6371000; // 지구 반지름 (미터)

    public MarkerService(MarkerMapper markerMapper) {
        this.markerMapper = markerMapper;
    }

    // ✅ 위도/경도 변환 함수 (500m 범위 내 검색)
    private double degreesToRadians(double degrees) {
        return degrees * (Math.PI / 180);
    }

    private double metersToLatitudeDegrees(double meters) {
        return (meters / EARTH_RADIUS) * (180 / Math.PI);
    }

    private double metersToLongitudeDegrees(double meters, double latitude) {
        return (meters / (EARTH_RADIUS * Math.cos(degreesToRadians(latitude)))) * (180 / Math.PI);
    }

    // ✅ 500m 범위 내 마커 조회 및 거리 계산 + 이미지 업로드 포함
    public void requestMarker(String title, double latitude, double longitude, List<MultipartFile> images) {
        // 📌 위도/경도 범위 계산
        double latRange = metersToLatitudeDegrees(500);
        double lonRange = metersToLongitudeDegrees(500, latitude);

        BigDecimal minLat = BigDecimal.valueOf(latitude - latRange).setScale(6, RoundingMode.HALF_UP);
        BigDecimal maxLat = BigDecimal.valueOf(latitude + latRange).setScale(6, RoundingMode.HALF_UP);
        BigDecimal minLon = BigDecimal.valueOf(longitude - lonRange).setScale(6, RoundingMode.HALF_UP);
        BigDecimal maxLon = BigDecimal.valueOf(longitude + lonRange).setScale(6, RoundingMode.HALF_UP);

        // 📌 DB에서 500m 내 마커 조회
        List<Map<String, Object>> nearbyMarkers = markerMapper.findMarkersWithinRadius(minLat, maxLat, minLon, maxLon);

        // 📌 거리 계산 (DB 필터 후 Java에서 최종 거리 확인)
        for (Map<String, Object> marker : nearbyMarkers) {
            double existingLat = ((Double) marker.get("latitude"));
            double existingLon = ((Double) marker.get("longitude"));

            double distance = calculateDistance(latitude, longitude, existingLat, existingLon);
            if (distance < 500) {
            	throw new IllegalArgumentException("500m 범위 이내에 등록된 농구장이 있습니다."); // 500m 내 중복이 있으면 마커 등록 중단
            }
        }

        // ✅ 마커 데이터 저장 (pending_markers 테이블)
        Map<String, Object> markerData = new HashMap<>();
        markerData.put("title", title);
        markerData.put("latitude", latitude);
        markerData.put("longitude", longitude);
        markerData.put("approved", false);

        // ✅ 이미지 업로드 처리
        if (images != null && !images.isEmpty()) {
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);

            // 📁 uploads 폴더가 없으면 생성
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

        // ✅ 최종 마커 등록
        markerMapper.insertPendingMarker(markerData);
    }

    // ✅ 하버사인 공식 (Haversine Formula)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c; // 미터 단위 거리 반환
    }

    // ✅ 관리자 - 등록 요청된 마커 리스트 조회
    public List<Map<String, Object>> getPendingMarkers() {
        return markerMapper.getPendingMarkers();
    }

    // ✅ 관리자 - 마커 승인 (pending_markers → markers 이동)
    public void approveMarker(int id) {
        Map<String, Object> marker = markerMapper.getPendingMarkerById(id);
        if (marker != null) {
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
                        .map(img -> "/uploads/" + img.trim())
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
                    .map(img -> "/uploads/" + img.trim())
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
