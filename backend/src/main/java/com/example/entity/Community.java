package com.example.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "communities")
@Data
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer communityId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "JSON")
    private String basicInfo;
    
    @Column(columnDefinition = "JSON")
    private String locationInfo;
    
    @Column(columnDefinition = "JSON")
    private String facilityInfo;
    
    @Transient
    public JSONObject getBasicInfoJson() {
        return JSONObject.parseObject(basicInfo);
    }
    
    @Transient
    public JSONObject getLocationInfoJson() {
        return JSONObject.parseObject(locationInfo);
    }
    
    @Transient
    public JSONObject getFacilityInfoJson() {
        return JSONObject.parseObject(facilityInfo);
    }
}