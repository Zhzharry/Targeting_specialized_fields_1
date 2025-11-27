<template>
  <div class="preference-settings">
    <!-- 兴趣关键词 -->
    <div class="preference-section">
      <h4>兴趣关键词</h4>
      <div class="tags-grid">
        <button
          v-for="tag in interestTags"
          :key="tag"
          class="tag-btn"
          :class="{ active: selectedInterests.includes(tag) }"
          @click="toggleInterest(tag)"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <!-- 房源类型 -->
    <div class="preference-section">
      <h4>偏好房型</h4>
      <div class="property-types">
        <button
          v-for="type in propertyTypes"
          :key="type.value"
          class="type-btn"
          :class="{ active: selectedTypes.includes(type.value) }"
          @click="togglePropertyType(type.value)"
        >
          <span class="type-icon">{{ type.icon }}</span>
          <span class="type-name">{{ type.name }}</span>
        </button>
      </div>
    </div>

    <!-- 价格区间 -->
    <div class="preference-section">
      <h4>价格区间 (元/月)</h4>
      <div class="price-range">
        <div class="price-display">
          <span>{{ priceRange[0] }}</span>
          <span>-</span>
          <span>{{ priceRange[1] }}</span>
        </div>
        <div class="slider-container">
          <input
            type="range"
            min="0"
            max="20000"
            step="100"
            :value="priceRange[0]"
            class="slider"
            @input="handleMinPriceChange"
          />
          <input
            type="range"
            min="0"
            max="20000"
            step="100"
            :value="priceRange[1]"
            class="slider"
            @input="handleMaxPriceChange"
          />
        </div>
      </div>
    </div>

    <!-- 所在城市 -->
    <div class="preference-section">
      <h4>所在城市</h4>
      <select v-model="selectedCity" class="city-select">
        <option value="">请选择城市</option>
        <option v-for="city in cities" :key="city.code" :value="city.code">
          {{ city.name }}
        </option>
      </select>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button @click="savePreferences" class="save-btn">保存偏好</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

// 定义接口
interface PreferenceData {
  interests: string[]
  propertyTypes: string[]
  priceRange: [number, number]
  city: string
}

// 定义emit
const emit = defineEmits<{
  update: [preferences: PreferenceData]
}>()

// 兴趣标签
const interestTags = ref([
  '近地铁',
  '学区房',
  '精装修',
  '拎包入住',
  '朝南',
  '电梯房',
  '停车方便',
  '商圈附近',
  '安静宜居',
  '绿化好',
])

// 房源类型
const propertyTypes = ref([
  { value: '1', name: '一居室', icon: '🏠' },
  { value: '2', name: '两室一厅', icon: '🏡' },
  { value: '3', name: '三室一厅', icon: '🏘️' },
  { value: 'loft', name: 'LOFT', icon: '🕌' },
])

// 城市列表
const cities = ref([
  { code: 'bj', name: '北京' },
  { code: 'sh', name: '上海' },
  { code: 'gz', name: '广州' },
  { code: 'sz', name: '深圳' },
  { code: 'hz', name: '杭州' },
  { code: 'cd', name: '成都' },
])

// 选择状态
const selectedInterests = ref<string[]>([])
const selectedTypes = ref<string[]>([])
const priceRange = ref<[number, number]>([2000, 8000])
const selectedCity = ref('')

// 方法
const toggleInterest = (tag: string) => {
  const index = selectedInterests.value.indexOf(tag)
  if (index > -1) {
    selectedInterests.value.splice(index, 1)
  } else {
    selectedInterests.value.push(tag)
  }
}

const togglePropertyType = (type: string) => {
  const index = selectedTypes.value.indexOf(type)
  if (index > -1) {
    selectedTypes.value.splice(index, 1)
  } else {
    selectedTypes.value.push(type)
  }
}

const handleMinPriceChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = parseInt(target.value)
  if (value < priceRange.value[1]) {
    priceRange.value[0] = value
  }
}

const handleMaxPriceChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = parseInt(target.value)
  if (value > priceRange.value[0]) {
    priceRange.value[1] = value
  }
}

const savePreferences = () => {
  const preferences: PreferenceData = {
    interests: selectedInterests.value,
    propertyTypes: selectedTypes.value,
    priceRange: priceRange.value,
    city: selectedCity.value,
  }
  emit('update', preferences)
}

// 监听变化自动保存（可选）
watch(
  [selectedInterests, selectedTypes, priceRange, selectedCity],
  () => {
    savePreferences()
  },
  { deep: true },
)

// 定义props（如果需要从父组件接收初始值）
defineProps<{
  preferences?: PreferenceData
}>()
</script>

<style scoped>
.preference-settings {
  padding: 20px 0;
}

.preference-section {
  margin-bottom: 25px;
}

.preference-section h4 {
  margin-bottom: 12px;
  color: #333;
  font-size: 16px;
}

.tags-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.tag-btn {
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.tag-btn.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.property-types {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.type-btn {
  padding: 12px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.type-btn.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.type-icon {
  font-size: 20px;
}

.type-name {
  font-size: 12px;
}

.price-range {
  padding: 10px 0;
}

.price-display {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #007bff;
  margin-bottom: 15px;
}

.slider-container {
  position: relative;
  height: 30px;
}

.slider {
  position: absolute;
  width: 100%;
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  background: #007bff;
  border-radius: 50%;
  cursor: pointer;
}

.slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  background: #007bff;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.city-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: white;
}

.action-buttons {
  margin-top: 30px;
  text-align: center;
}

.save-btn {
  padding: 12px 30px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
}

.save-btn:hover {
  background: #0056b3;
}
</style>
