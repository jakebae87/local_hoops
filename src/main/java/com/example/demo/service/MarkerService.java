package com.example.demo.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.mapper.MarkerMapper;

@Service
public class MarkerService {
    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final RestTemplate restTemplate;
    private final MarkerMapper markerMapper;

    private static final double EARTH_RADIUS = 6371000; // 지구 반지름 (미터)

    public MarkerService(RestTemplate restTemplate, MarkerMapper markerMapper) {
        this.restTemplate = restTemplate;
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
        double latRange = metersToLatitudeDegrees(500);
        double lonRange = metersToLongitudeDegrees(500, latitude);

        BigDecimal minLat = BigDecimal.valueOf(latitude - latRange).setScale(6, RoundingMode.HALF_UP);
        BigDecimal maxLat = BigDecimal.valueOf(latitude + latRange).setScale(6, RoundingMode.HALF_UP);
        BigDecimal minLon = BigDecimal.valueOf(longitude - lonRange).setScale(6, RoundingMode.HALF_UP);
        BigDecimal maxLon = BigDecimal.valueOf(longitude + lonRange).setScale(6, RoundingMode.HALF_UP);

        List<Map<String, Object>> nearbyMarkers = markerMapper.findMarkersWithinRadius(minLat, maxLat, minLon, maxLon);

        for (Map<String, Object> marker : nearbyMarkers) {
            double existingLat = ((Double) marker.get("latitude"));
            double existingLon = ((Double) marker.get("longitude"));
            double distance = calculateDistance(latitude, longitude, existingLat, existingLon);
            if (distance < 500) {
                throw new IllegalArgumentException("500m 범위 이내에 등록된 농구장이 있습니다.");
            }
        }

        Map<String, Object> markerData = new HashMap<>();
        markerData.put("title", title);
        markerData.put("latitude", latitude);
        markerData.put("longitude", longitude);
        markerData.put("approved", false);

        if (images != null && !images.isEmpty()) {
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);

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

                    BufferedImage originalImage = ImageIO.read(image.getInputStream());
                    if (originalImage == null) {
                        System.err.println("🚨 이미지 포맷을 읽을 수 없습니다: " + image.getOriginalFilename());
                        continue; // 이미지가 손상되었거나 지원하지 않는 형식이면 건너뜀
                    }

                    // 800px 이상이면 리사이징, 작으면 원본 유지
                    BufferedImage imageToSave = (originalImage.getWidth() > 800) ? resizeImage(originalImage, 800) : originalImage;

                    ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                    ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                    jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    jpgWriteParam.setCompressionQuality(0.75f); // 압축률 설정

                    try (FileImageOutputStream output = new FileImageOutputStream(filePath.toFile())) {
                        jpgWriter.setOutput(output);
                        jpgWriter.write(null, new IIOImage(imageToSave, null, null), jpgWriteParam);
                        jpgWriter.dispose();
                    }

                    imagePaths.append("/uploads/").append(fileName).append(",");
                    System.out.println("📌 저장된 리사이즈 이미지: " + filePath.toAbsolutePath());

                } catch (IOException e) {
                    System.err.println("🚨 이미지 저장 중 오류 발생: " + e.getMessage());
                }
            }

            markerData.put("image", imagePaths.length() > 0 ? imagePaths.substring(0, imagePaths.length() - 1) : null);
        } else {
            markerData.put("image", null);
        }

        markerMapper.insertPendingMarker(markerData);
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        double ratio = (double) targetWidth / width;
        int newHeight = (int) (height * ratio);

        BufferedImage resized = new BufferedImage(targetWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, newHeight, null);
        g.dispose();
        return resized;
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
                List<String> imagePaths = Arrays.asList(marker.get("image").toString().split(","));
                List<String> fullUrls = imagePaths.stream()
                        .map(img -> "/uploads/" + img.trim())
                        .collect(Collectors.toList());
                marker.put("images", fullUrls);
            }
        }
        return markers;
    }

    // ✅ 특정 마커 상세 조회
    public Map<String, Object> getMarkerById(int id) {
        Map<String, Object> marker = markerMapper.getMarkerById(id);
        if (marker != null && marker.get("image") != null) {
            List<String> imagePaths = Arrays.asList(marker.get("image").toString().split(","));
            List<String> fullUrls = imagePaths.stream()
                    .map(img -> "/uploads/" + img.trim())
                    .collect(Collectors.toList());
            marker.put("images", fullUrls);
        }
        return marker;
    }

    // ✅ 승인 요청 마커 삭제
    public void deleteRequestdMarker(Integer id) {
        markerMapper.deletePendingMarker(id);
    }

    // ✅ AI 서버로 모든 이미지 검증
    public boolean validateImagesWithAI(List<MultipartFile> images) {
        try {
            for (MultipartFile image : images) {
                String url = aiServerUrl + "/detect/";

                // ✅ MultipartFile을 ByteArrayResource로 변환
                Resource fileResource = new ByteArrayResource(image.getBytes()) {
                    @Override
                    public String getFilename() {
                        return image.getOriginalFilename();  // 원본 파일명 유지
                    }
                };

                // ✅ Multipart 요청 바디 생성
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", fileResource);  // FastAPI에서 `file` 필드로 받음

                // ✅ HttpHeaders 설정 (multipart/form-data)
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                // ✅ HttpEntity 생성
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                // ✅ AI 서버에 요청 전송
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    System.out.println("✅ AI 응답: " + response.getBody());

                    if (!"valid".equals(response.getBody().get("result"))) {
                        return false;
                    }
                } else {
                    System.err.println("🚨 AI 서버 응답 실패: " + response.getStatusCode());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("🚨 AI 검증 요청 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

}
