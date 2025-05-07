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

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.mapper.MarkerMapper;

@Service
public class MarkerService {

    private final MarkerMapper markerMapper;

    private static final double EARTH_RADIUS = 6371000;

    public MarkerService(RestTemplate restTemplate, MarkerMapper markerMapper) {
        this.markerMapper = markerMapper;
    }

    private double degreesToRadians(double degrees) {
        return degrees * (Math.PI / 180);
    }

    private double metersToLatitudeDegrees(double meters) {
        return (meters / EARTH_RADIUS) * (180 / Math.PI);
    }

    private double metersToLongitudeDegrees(double meters, double latitude) {
        return (meters / (EARTH_RADIUS * Math.cos(degreesToRadians(latitude)))) * (180 / Math.PI);
    }

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
                String originalFilename = image.getOriginalFilename();
                if (originalFilename == null || originalFilename.trim().isEmpty()) {
                    System.err.println("🚨 파일 이름이 비어 있음");
                    continue;
                }

                try {
                    String fileName = System.currentTimeMillis() + "_" + originalFilename;
                    Path filePath = uploadPath.resolve(fileName);

                    BufferedImage originalImage = ImageIO.read(image.getInputStream());
                    if (originalImage == null) {
                        System.err.println("🚨 이미지 포맷을 읽을 수 없습니다: " + originalFilename);
                        continue;
                    }

                    BufferedImage imageToSave = (originalImage.getWidth() > 800)
                            ? resizeImage(originalImage, 800)
                            : originalImage;

                    ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                    ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                    jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    jpgWriteParam.setCompressionQuality(0.75f);

                    try (FileImageOutputStream output = new FileImageOutputStream(filePath.toFile())) {
                        jpgWriter.setOutput(output);
                        jpgWriter.write(null, new IIOImage(imageToSave, null, null), jpgWriteParam);
                        jpgWriter.dispose();
                    }

                    imagePaths.append("/uploads/").append(fileName).append(",");
                    System.out.println("📌 저장된 이미지: " + filePath.toAbsolutePath());

                } catch (IOException e) {
                    System.err.println("🚨 이미지 저장 중 오류 발생: " + e.getMessage());
                }
            }

            String finalPath = imagePaths.length() > 0 ? imagePaths.substring(0, imagePaths.length() - 1) : null;
            System.out.println("📥 DB에 저장될 이미지 경로: " + finalPath);
            markerData.put("image", finalPath);
        } else {
            markerData.put("image", null);
        }

        markerMapper.insertPendingMarker(markerData);
        System.out.println("✅ 마커 요청 저장 완료: " + markerData);
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

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public List<Map<String, Object>> getPendingMarkers() {
        return markerMapper.getPendingMarkers();
    }

    public void approveMarker(int id) {
        Map<String, Object> marker = markerMapper.getPendingMarkerById(id);
        if (marker != null) {
            markerMapper.insertMarker(marker);
            markerMapper.deletePendingMarker(id);
        }
    }

    public void deleteMarker(int id) {
        markerMapper.deleteMarker(id);
    }

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

    public void deleteRequestdMarker(Integer id) {
        markerMapper.deletePendingMarker(id);
    }
}
