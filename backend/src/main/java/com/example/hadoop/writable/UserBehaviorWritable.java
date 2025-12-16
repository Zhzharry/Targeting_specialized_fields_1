package com.example.hadoop.writable;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 用户行为数据Writable类
 * 用于在MapReduce中传递用户行为数据
 * 
 * 位置：backend/src/main/java/com/example/hadoop/writable/UserBehaviorWritable.java
 */
public class UserBehaviorWritable implements Writable {
    
    private long userId;
    private long propertyId;
    private int behaviorType; // 1=浏览, 2=收藏, 3=搜索
    private double weight;    // 行为权重
    private long timestamp;
    
    public UserBehaviorWritable() {
    }
    
    public UserBehaviorWritable(long userId, long propertyId, int behaviorType, double weight, long timestamp) {
        this.userId = userId;
        this.propertyId = propertyId;
        this.behaviorType = behaviorType;
        this.weight = weight;
        this.timestamp = timestamp;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(userId);
        out.writeLong(propertyId);
        out.writeInt(behaviorType);
        out.writeDouble(weight);
        out.writeLong(timestamp);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        userId = in.readLong();
        propertyId = in.readLong();
        behaviorType = in.readInt();
        weight = in.readDouble();
        timestamp = in.readLong();
    }

    // Getters and Setters
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    
    public long getPropertyId() { return propertyId; }
    public void setPropertyId(long propertyId) { this.propertyId = propertyId; }
    
    public int getBehaviorType() { return behaviorType; }
    public void setBehaviorType(int behaviorType) { this.behaviorType = behaviorType; }
    
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return userId + "\t" + propertyId + "\t" + behaviorType + "\t" + weight + "\t" + timestamp;
    }

    /**
     * 从字符串解析
     */
    public static UserBehaviorWritable fromString(String line) {
        String[] parts = line.split("\t");
        if (parts.length >= 5) {
            return new UserBehaviorWritable(
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1]),
                Integer.parseInt(parts[2]),
                Double.parseDouble(parts[3]),
                Long.parseLong(parts[4])
            );
        }
        return null;
    }
}