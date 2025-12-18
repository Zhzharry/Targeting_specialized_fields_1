// SimilarityEventListener.java
package com.example.listener;

import com.example.service.UserSimilarityPropagationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 相似度计算事件监听器
 * 
 * 监听用户行为事件，自动触发相似度增量计算
 * 
 * 使用方式：
 * 在需要触发计算的地方发布事件：
 *   applicationEventPublisher.publishEvent(new UserPreferenceChangedEvent(userId));
 */
@Component
public class SimilarityEventListener {
    
    @Autowired
    private UserSimilarityPropagationService userSimilarityService;
    
    /**
     * 监听用户偏好变更事件
     */
    @EventListener
    public void handleUserPreferenceChanged(UserPreferenceChangedEvent event) {
        System.out.println("[事件监听] 用户 " + event.getUserId() + " 偏好已变更，触发增量计算");
        userSimilarityService.onUserPreferenceChanged(event.getUserId());
    }
    
    /**
     * 监听用户退出登录事件
     */
    @EventListener
    public void handleUserLogout(UserLogoutEvent event) {
        System.out.println("[事件监听] 用户 " + event.getUserId() + " 已退出，检查是否需要更新");
        userSimilarityService.onUserLogout(event.getUserId());
    }
    
    /**
     * 监听用户浏览事件
     */
    @EventListener
    public void handleUserBrowsing(UserBrowsingEvent event) {
        userSimilarityService.recordUserBrowsing(event.getUserId(), event.getPropertyId());
    }
    
    // ==================== 事件类定义 ====================
    
    /**
     * 用户偏好变更事件
     */
    public static class UserPreferenceChangedEvent {
        private final int userId;
        
        public UserPreferenceChangedEvent(int userId) {
            this.userId = userId;
        }
        
        public int getUserId() {
            return userId;
        }
    }
    
    /**
     * 用户退出登录事件
     */
    public static class UserLogoutEvent {
        private final int userId;
        
        public UserLogoutEvent(int userId) {
            this.userId = userId;
        }
        
        public int getUserId() {
            return userId;
        }
    }
    
    /**
     * 用户浏览事件
     */
    public static class UserBrowsingEvent {
        private final int userId;
        private final int propertyId;
        
        public UserBrowsingEvent(int userId, int propertyId) {
            this.userId = userId;
            this.propertyId = propertyId;
        }
        
        public int getUserId() {
            return userId;
        }
        
        public int getPropertyId() {
            return propertyId;
        }
    }
}