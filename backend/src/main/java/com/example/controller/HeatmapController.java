package com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房价热力图接口控制器。
 * 提供房价热力图所需的地理位置和价格数据。
 */
@RestController
@RequestMapping("/api/heatmap")
@CrossOrigin(origins = "http://localhost:5173")
public class HeatmapController {

  @Autowired
  private DataSource dataSource;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 获取热力图数据。
   * 返回房产的经纬度和价格信息。
   *
   * @param city       城市（可选）
   * @param priceRange 价格范围（可选）
   * @return 热力图数据点列表
   */
  @GetMapping("/data")
  public ResponseEntity<Map<String, Object>> getHeatmapData(
      @RequestParam(value = "city", required = false) String city,
      @RequestParam(value = "priceRange", required = false) String priceRange) {

    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> dataPoints = new ArrayList<>();

    try (Connection connection = dataSource.getConnection()) {
      // 构建SQL查询 - 从communities表获取经纬度
      StringBuilder sql = new StringBuilder(
          "SELECT p.property_id, p.title as property_name, " +
              "JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.district')) as district, " +
              "JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.total_price')) as total_price, " +
              "JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) as unit_price, " +
              "JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude')) as latitude, " +
              "JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude')) as longitude " +
              "FROM properties p " +
              "INNER JOIN communities c ON p.community_id = c.community_id " +
              "WHERE JSON_EXTRACT(c.location_info, '$.latitude') IS NOT NULL " +
              "AND JSON_EXTRACT(c.location_info, '$.longitude') IS NOT NULL ");

      List<Object> params = new ArrayList<>();

      // 根据城市过滤（如果提供）
      if (city != null && !city.isEmpty() && !city.equals("all")) {
        // 这里可以根据实际数据库结构添加城市字段过滤
        // sql.append("AND p.city = ? ");
        // params.add(city);
      }

      // 根据价格范围过滤
      if (priceRange != null && !priceRange.equals("all")) {
        String[] range = priceRange.split("-");
        if (range.length == 2) {
          if (!range[1].isEmpty()) {
            sql.append("AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) AS UNSIGNED) >= ? ");
            sql.append("AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) AS UNSIGNED) < ? ");
            params.add(Integer.parseInt(range[0]));
            params.add(Integer.parseInt(range[1]));
          } else {
            sql.append("AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) AS UNSIGNED) >= ? ");
            params.add(Integer.parseInt(range[0]));
          }
        }
      }

      // 限制返回数量，避免数据过多
      sql.append("LIMIT 2000");

      try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
        // 设置参数
        for (int i = 0; i < params.size(); i++) {
          stmt.setObject(i + 1, params.get(i));
        }

        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            try {
              double longitude = Double.parseDouble(rs.getString("longitude"));
              double latitude = Double.parseDouble(rs.getString("latitude"));
              int unitPrice = Integer.parseInt(rs.getString("unit_price"));

              Map<String, Object> point = new HashMap<>();
              point.put("longitude", longitude);
              point.put("latitude", latitude);
              point.put("price", unitPrice); // 单价
              point.put("propertyName", rs.getString("property_name"));
              point.put("district", rs.getString("district"));

              dataPoints.add(point);
            } catch (Exception e) {
              // 跳过无效数据
              System.err.println("跳过无效数据行: " + e.getMessage());
            }
          }
        }
      }

      System.out.println("热力图数据查询完成，返回 " + dataPoints.size() + " 个数据点");
      if (dataPoints.size() > 0) {
        System.out.println("示例数据点: " + dataPoints.get(0));
      }

      response.put("success", true);
      response.put("data", dataPoints);
      response.put("message", "数据加载成功");
      response.put("total", dataPoints.size());

    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("热力图数据加载失败: " + e.getMessage());
      response.put("success", false);
      response.put("data", new ArrayList<>());
      response.put("message", "加载数据失败: " + e.getMessage());
    }

    return ResponseEntity.ok(response);
  }

  /**
   * 获取热力图统计信息。
   * 返回价格分布统计数据。
   *
   * @param city 城市（可选）
   * @return 统计信息
   */
  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getHeatmapStats(
      @RequestParam(value = "city", required = false) String city) {

    Map<String, Object> response = new HashMap<>();

    try (Connection connection = dataSource.getConnection()) {
      String sql = "SELECT " +
          "COUNT(*) as total_count, " +
          "AVG(unit_price) as avg_price, " +
          "MIN(unit_price) as min_price, " +
          "MAX(unit_price) as max_price " +
          "FROM properties " +
          "WHERE latitude IS NOT NULL AND longitude IS NOT NULL";

      try (PreparedStatement stmt = connection.prepareStatement(sql);
          ResultSet rs = stmt.executeQuery()) {

        if (rs.next()) {
          Map<String, Object> stats = new HashMap<>();
          stats.put("totalCount", rs.getInt("total_count"));
          stats.put("avgPrice", rs.getDouble("avg_price"));
          stats.put("minPrice", rs.getDouble("min_price"));
          stats.put("maxPrice", rs.getDouble("max_price"));

          response.put("success", true);
          response.put("data", stats);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      response.put("success", false);
      response.put("message", "获取统计信息失败: " + e.getMessage());
    }

    return ResponseEntity.ok(response);
  }
}
