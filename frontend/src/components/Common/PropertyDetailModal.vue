<template>
  <div v-if="visible && property" class="modal-overlay" @click="closeModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">{{ property.title }}</h2>
        <button class="close-btn" @click="closeModal">×</button>
      </div>

      <div class="modal-body">
        <!-- 房源图片 -->
        <div class="property-images">
          <img :src="getPropertyImage()" :alt="property.title" class="main-image">
        </div>

        <!-- 基本信息 -->
        <div class="property-basic-info">
          <div class="price-section">
            <div class="total-price">
              <span class="price-amount">¥{{ property.priceInfo?.total_price || '暂无' }}</span>
              <span class="price-unit">万</span>
            </div>
            <div class="unit-price">
              单价: ¥{{ property.priceInfo?.unit_price || '暂无' }}/㎡
            </div>
          </div>

          <div class="property-details">
            <div class="detail-row">
              <span class="label">户型:</span>
              <span class="value">
                {{ property.layoutInfo?.bedroom_count || 0 }}室
                {{ property.layoutInfo?.living_room_count || 0 }}厅
                {{ property.layoutInfo?.bathroom_count || 0 }}卫
              </span>
            </div>
            <div class="detail-row">
              <span class="label">面积:</span>
              <span class="value">{{ property.layoutInfo?.area || '暂无' }}㎡</span>
            </div>
            <div class="detail-row">
              <span class="label">楼层:</span>
              <span class="value">暂无信息</span>
            </div>
            <div class="detail-row">
              <span class="label">朝向:</span>
              <span class="value">暂无信息</span>
            </div>
            <div class="detail-row">
              <span class="label">装修:</span>
              <span class="value">暂无信息</span>
            </div>
            <div class="detail-row">
              <span class="label">年代:</span>
              <span class="value">{{ property.basicInfo?.build_year || '暂无' }}年</span>
            </div>
          </div>
        </div>

        <!-- 位置信息 -->
        <div class="location-info">
          <h3>位置信息</h3>
          <div class="location-details">
            <div class="detail-row">
              <span class="label">小区:</span>
              <span class="value">{{ property.communityName || '暂无' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">地址:</span>
              <span class="value">
                {{ property.locationInfo?.province || '' }}
                {{ property.locationInfo?.city || '' }}
                {{ property.locationInfo?.district || '' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="stats-info">
          <div class="stat-item">
            <span class="stat-label">浏览次数</span>
            <span class="stat-value">{{ property.viewCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">收藏次数</span>
            <span class="stat-value">{{ property.favoriteCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">更新时间</span>
            <span class="stat-value">{{ formatDate(property.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="modal-footer">
        <button
          class="btn btn-secondary"
          @click="toggleFavorite"
          :class="{ active: isFavorited }"
        >
          {{ isFavorited ? '已收藏' : '收藏' }}
        </button>
        <button class="btn btn-primary" @click="handlePurchase">
          立即购买
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { PropertyDetail } from '@/types/api.types'
import { useAuthStore } from '@/stores/auth.store'

interface Props {
  visible: boolean
  property: PropertyDetail | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'favorite': [propertyId: number]
  'purchase': [property: PropertyDetail]
}>()

const authStore = useAuthStore()
const isFavorited = ref(false)

// 监听属性变化，检查是否已收藏
watch(() => props.property, (newProperty) => {
  if (newProperty && authStore.isLoggedIn) {
    // 这里可以调用API检查是否已收藏
    // 暂时用简单的逻辑
    isFavorited.value = false
  }
})

const closeModal = () => {
  emit('update:visible', false)
}

const toggleFavorite = () => {
  if (!authStore.isLoggedIn) {
    alert('请先登录')
    return
  }

  if (props.property) {
    emit('favorite', props.property.propertyId)
    isFavorited.value = !isFavorited.value
  }
}

const handlePurchase = () => {
  if (!authStore.isLoggedIn) {
    alert('请先登录')
    return
  }

  if (props.property) {
    emit('purchase', props.property)
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '暂无'
  try {
    return new Date(dateString).toLocaleDateString('zh-CN')
  } catch {
    return dateString
  }
}

const getPropertyImage = () => {
  // 可以使用picsum.photos生成基于propertyId的图片，或者使用占位符
  return `https://picsum.photos/seed/property-${props.property?.propertyId || 'default'}/600/400`
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #666;
}

.modal-body {
  padding: 20px;
}

.property-images {
  margin-bottom: 20px;
}

.main-image {
  width: 100%;
  height: 250px;
  object-fit: cover;
  border-radius: 8px;
}

.property-basic-info {
  margin-bottom: 20px;
}

.price-section {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.total-price {
  display: flex;
  align-items: baseline;
  margin-bottom: 5px;
}

.price-amount {
  font-size: 28px;
  font-weight: bold;
  color: #e74c3c;
}

.price-unit {
  font-size: 16px;
  color: #666;
  margin-left: 4px;
}

.unit-price {
  font-size: 14px;
  color: #666;
}

.property-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-row:last-child {
  border-bottom: none;
}

.label {
  font-weight: 500;
  color: #666;
}

.value {
  color: #333;
}

.location-info, .stats-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.location-info h3, .stats-info h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #333;
}

.stats-info {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.modal-footer {
  display: flex;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
  background: #f8f9fa;
}

.btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: white;
  border: 1px solid #ddd;
  color: #666;
}

.btn-secondary:hover {
  background: #f5f5f5;
}

.btn-secondary.active {
  background: #e74c3c;
  color: white;
  border-color: #e74c3c;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 10px;
  }

  .property-details {
    grid-template-columns: 1fr;
  }

  .stats-info {
    flex-direction: column;
    gap: 15px;
  }

  .modal-footer {
    flex-direction: column;
  }
}
</style>
