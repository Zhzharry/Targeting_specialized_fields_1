package com.example.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "properties")
@Data
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer propertyId;
    
    @Column(name = "community_id")
    private Integer communityId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "JSON")
    private String basicInfo;
    
    @Column(columnDefinition = "JSON")
    private String priceInfo;
    
    @Column(columnDefinition = "JSON")
    private String layoutInfo;
    
    @Column(nullable = false)
    private String status = "for_sale";
    
    @Transient
    public JSONObject getBasicInfoJson() {
        return JSONObject.parseObject(basicInfo);
    }
    
    @Transient
    public JSONObject getPriceInfoJson() {
        return JSONObject.parseObject(priceInfo);
    }
    
    @Transient
    public JSONObject getLayoutInfoJson() {
        return JSONObject.parseObject(layoutInfo);
    }
}