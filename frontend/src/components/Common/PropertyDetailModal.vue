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
              <span class="value">{{ communityName }}</span>
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
import { ref, watch, computed } from 'vue'
import type { PropertyDetail, PopularProperty } from '@/types/api.types'
import { useAuthStore } from '@/stores/auth.store'

interface Props {
  visible: boolean
  property: PropertyDetail | PopularProperty | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'favorite': [propertyId: number]
  'purchase': [property: PropertyDetail | PopularProperty]
}>()

const authStore = useAuthStore()
const isFavorited = ref(false)

// 获取小区名称（兼容PropertyDetail和PopularProperty）
const communityName = computed(() => {
  if (!props.property) return '暂无'
  const property = props.property
  if ('communityName' in property) {
    return property.communityName || '暂无'
  }
  if ('community_name' in property) {
    return property.community_name || '暂无'
  }
  return '暂无'
})

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
  // 检查登录状态 - 支持多种方式
  const userInfo = localStorage.getItem('userInfo')
  const hasToken = localStorage.getItem('token')
  const isUserLoggedIn = authStore.isLoggedIn || (hasToken && userInfo)

  if (!isUserLoggedIn) {
    alert('请先登录')
    return
  }

  if (props.property) {
    emit('favorite', props.property.propertyId)
    isFavorited.value = !isFavorited.value
  }
}

const handlePurchase = () => {
  // 检查登录状态 - 支持多种方式
  const userInfo = localStorage.getItem('userInfo')
  const hasToken = localStorage.getItem('token')
  const isUserLoggedIn = authStore.isLoggedIn || (hasToken && userInfo)

  if (!isUserLoggedIn) {
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
  if (!props.property) return ''
  // 优先使用接口返回的cover图片
  const property = props.property
  if ('cover' in property) {
    return property.cover || `https://picsum.photos/seed/property-${property.propertyId}/600/400`
  }
  return `https://picsum.photos/seed/property-${property.propertyId}/600/400`
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95), rgba(22, 33, 62, 0.98));
  border: 2px solid rgba(212, 175, 55, 0.3);
  border-radius: 16px;
  width: 100%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow:
    0 0 60px rgba(212, 175, 55, 0.15),
    0 20px 60px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  animation: modalSlideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 2px solid rgba(212, 175, 55, 0.2);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05), transparent);
}

.modal-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffd700, #d4af37);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(212, 175, 55, 0.3);
  letter-spacing: 0.5px;
}

.close-btn {
  background: rgba(212, 175, 55, 0.1);
  border: 1.5px solid rgba(212, 175, 55, 0.3);
  font-size: 24px;
  cursor: pointer;
  color: #d4af37;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 300;
}

.close-btn:hover {
  background: rgba(212, 175, 55, 0.2);
  border-color: #ffd700;
  color: #ffd700;
  transform: rotate(90deg);
  box-shadow: 0 0 20px rgba(212, 175, 55, 0.4);
}

.modal-body {
  padding: 28px;
}

.property-images {
  margin-bottom: 28px;
}

.main-image {
  width: 100%;
  height: 300px;
  object-fit: cover;
  border-radius: 12px;
  border: 2px solid rgba(212, 175, 55, 0.25);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    0 0 40px rgba(212, 175, 55, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.main-image:hover {
  transform: scale(1.02);
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.5),
    0 0 60px rgba(212, 175, 55, 0.2);
}

.property-basic-info {
  margin-bottom: 24px;
}

.price-section {
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.08), rgba(212, 175, 55, 0.03));
  border: 2px solid rgba(212, 175, 55, 0.25);
  border-radius: 12px;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  backdrop-filter: blur(12px);
}

.total-price {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.price-amount {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700, #d4af37, #ffd700);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 40px rgba(212, 175, 55, 0.4);
  animation: priceShine 3s ease-in-out infinite;
  letter-spacing: 1px;
}

@keyframes priceShine {
  0%, 100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.2);
  }
}

.price-unit {
  font-size: 18px;
  color: rgba(212, 175, 55, 0.8);
  margin-left: 6px;
  font-weight: 600;
}

.unit-price {
  font-size: 15px;
  color: rgba(212, 175, 55, 0.7);
  font-weight: 500;
}

.property-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(212, 175, 55, 0.05);
  border: 1px solid rgba(212, 175, 55, 0.15);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.detail-row:hover {
  background: rgba(212, 175, 55, 0.1);
  border-color: rgba(212, 175, 55, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.15);
}

.label {
  font-weight: 600;
  color: rgba(212, 175, 55, 0.9);
  font-size: 14px;
  letter-spacing: 0.3px;
}

.value {
  color: rgba(212, 175, 55, 0.7);
  font-weight: 500;
  font-size: 14px;
}

.location-info, .stats-info {
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.06), rgba(212, 175, 55, 0.02));
  border: 2px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  backdrop-filter: blur(12px);
}

.location-info h3 {
  margin: 0 0 18px 0;
  font-size: 18px;
  font-weight: 700;
  color: #ffd700;
  text-shadow: 0 0 20px rgba(212, 175, 55, 0.3);
  letter-spacing: 0.5px;
}

.stats-info {
  display: flex;
  justify-content: space-around;
  text-align: center;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 16px;
  background: rgba(212, 175, 55, 0.05);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 10px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: rgba(212, 175, 55, 0.1);
  border-color: rgba(212, 175, 55, 0.4);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.2);
}

.stat-label {
  font-size: 13px;
  color: rgba(212, 175, 55, 0.7);
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffd700, #d4af37);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 20px rgba(212, 175, 55, 0.3);
}

.modal-footer {
  display: flex;
  gap: 16px;
  padding: 24px 28px;
  border-top: 2px solid rgba(212, 175, 55, 0.2);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05), transparent);
}

.btn {
  flex: 1;
  padding: 14px 24px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.5px;
  position: relative;
  overflow: hidden;
}

.btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.btn:hover::before {
  width: 300px;
  height: 300px;
}

.btn-secondary {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(212, 175, 55, 0.05));
  border: 2px solid rgba(212, 175, 55, 0.3);
  color: rgba(212, 175, 55, 0.9);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.btn-secondary:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2), rgba(212, 175, 55, 0.1));
  border-color: rgba(212, 175, 55, 0.5);
  transform: translateY(-2px);
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.3),
    0 0 30px rgba(212, 175, 55, 0.3);
  color: #ffd700;
}

.btn-secondary.active {
  background: linear-gradient(135deg, #ffd700, #d4af37);
  color: #1a1a2e;
  border-color: #ffd700;
  box-shadow:
    0 6px 25px rgba(212, 175, 55, 0.4),
    0 0 40px rgba(212, 175, 55, 0.3);
  font-weight: 700;
}

.btn-secondary.active:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow:
    0 8px 30px rgba(212, 175, 55, 0.5),
    0 0 50px rgba(212, 175, 55, 0.4);
}

.btn-primary {
  background: linear-gradient(135deg, #ffd700, #d4af37, #ffd700);
  color: #1a1a2e;
  border: 2px solid transparent;
  box-shadow:
    0 6px 25px rgba(212, 175, 55, 0.4),
    0 0 40px rgba(212, 175, 55, 0.2);
  font-weight: 700;
  animation: buttonGlow 2s ease-in-out infinite;
}

@keyframes buttonGlow {
  0%, 100% {
    box-shadow:
      0 6px 25px rgba(212, 175, 55, 0.4),
      0 0 40px rgba(212, 175, 55, 0.2);
  }
  50% {
    box-shadow:
      0 6px 30px rgba(212, 175, 55, 0.6),
      0 0 50px rgba(212, 175, 55, 0.3);
  }
}

.btn-primary:hover {
  background: linear-gradient(135deg, #ffd700, #ffed4e, #ffd700);
  transform: translateY(-3px) scale(1.03);
  box-shadow:
    0 8px 35px rgba(212, 175, 55, 0.6),
    0 0 60px rgba(212, 175, 55, 0.4);
}

/* 滚动条样式 */
.modal-content::-webkit-scrollbar {
  width: 8px;
}

.modal-content::-webkit-scrollbar-track {
  background: rgba(15, 20, 25, 0.4);
  border-radius: 10px;
}

.modal-content::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.4), rgba(212, 175, 55, 0.6));
  border-radius: 10px;
  border: 2px solid rgba(15, 20, 25, 0.4);
}

.modal-content::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.6), rgba(212, 175, 55, 0.8));
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 10px;
  }

  .modal-content {
    max-width: 100%;
    border-radius: 12px;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 20px;
  }

  .modal-title {
    font-size: 18px;
  }

  .main-image {
    height: 200px;
  }

  .price-amount {
    font-size: 28px;
  }

  .property-details {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .stats-info {
    flex-direction: column;
    gap: 12px;
  }

  .modal-footer {
    flex-direction: column;
    gap: 12px;
  }

  .btn {
    width: 100%;
  }
}
</style>
