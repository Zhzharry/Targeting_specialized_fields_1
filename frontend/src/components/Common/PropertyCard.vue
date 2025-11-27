<template>
  <div class="property-card" @click="$emit('click')">
    <img :src="property.image" :alt="property.title" class="property-image">
    <div class="property-info">
      <h3 class="property-title">{{ property.title }}</h3>
      <p class="property-location">{{ property.community }}</p>
      <div class="property-meta">
        <span>{{ property.bedrooms }}室{{ property.livingRooms }}厅</span>
        <span>{{ property.area }}㎡</span>
      </div>
      <div class="property-price">
        <span class="price">¥{{ property.price }}</span>
        <span class="unit">/月</span>
      </div>
      <div class="property-tags">
        <span v-for="tag in property.tags" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Property {
  id: number
  title: string
  community: string
  bedrooms: number
  livingRooms: number
  area: number
  price: number
  image: string
  tags: string[]
}

defineProps<{
  property: Property
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.property-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.property-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.property-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.property-info {
  padding: 16px;
}

.property-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #333;
  line-height: 1.4;
}

.property-location {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.property-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  color: #888;
  font-size: 13px;
}

.property-price {
  margin-bottom: 12px;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #ff4757;
}

.unit {
  color: #888;
  font-size: 14px;
  margin-left: 2px;
}

.property-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 8px;
  background: #e3f2fd;
  color: #1976d2;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}
</style>