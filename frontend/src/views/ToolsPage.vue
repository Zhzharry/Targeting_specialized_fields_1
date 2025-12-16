<!-- src/views/ToolsPage.vue -->
<template>
  <div class="tools-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="logo" @click="$router.push('/search')">🏠 房产平台</div>
      <div class="header-title">实用工具</div>
      <div class="header-actions">
        <button class="icon-btn" @click="$router.push('/profile')">
          <span>👤</span>
        </button>
      </div>
    </header>

    <!-- 工具导航 -->
    <div class="tools-nav">
      <button
        class="nav-tab"
        :class="{ active: activeTool === 'prediction' }"
        @click="activeTool = 'prediction'"
      >
        📈 房价预测
      </button>
      <button
        class="nav-tab"
        :class="{ active: activeTool === 'mortgage' }"
        @click="activeTool = 'mortgage'"
      >
        💰 贷款计算
      </button>
      <button
        class="nav-tab"
        :class="{ active: activeTool === 'heatmap' }"
        @click="activeTool = 'heatmap'"
      >
        🗺️ 价格热力图
      </button>
    </div>

    <!-- 内容区域 -->
    <div class="tools-content">
      <!-- 房价预测 -->
      <div v-if="activeTool === 'prediction'" class="tool-section">
        <div class="section-header">
          <h2>房价预测分析</h2>
          <p>基于机器学习算法预测未来房价趋势</p>
        </div>

        <div class="prediction-form">
          <div class="form-grid">
            <!-- 城市选择 -->
            <div class="form-group">
              <label>所在城市</label>
              <select v-model="predictionForm.city" @change="onCityChange">
                <option value="">请选择城市</option>
                <option value="北京">北京</option>
                <option value="上海">上海</option>
                <option value="天津">天津</option>
                <option value="石家庄">石家庄</option>
              </select>
            </div>

            <!-- 房屋面积 -->
            <div class="form-group">
              <label>房屋面积 (㎡)</label>
              <input
                v-model.number="predictionForm.area"
                type="number"
                placeholder="请输入面积"
                min="20"
                max="500"
              />
            </div>

            <!-- 室数 -->
            <div class="form-group">
              <label>室数</label>
              <select v-model="predictionForm.roomCount">
                <option value="">请选择室数</option>
                <option value="1">1室</option>
                <option value="2">2室</option>
                <option value="3">3室</option>
                <option value="4">4室</option>
                <option value="5">5室及以上</option>
              </select>
            </div>

            <!-- 厅数 -->
            <div class="form-group">
              <label>厅数</label>
              <select v-model="predictionForm.livingRoomCount">
                <option value="">请选择厅数</option>
                <option value="1">1厅</option>
                <option value="2">2厅</option>
              </select>
            </div>

            <!-- 卫数 -->
            <div class="form-group">
              <label>卫数</label>
              <select v-model="predictionForm.bathroomCount">
                <option value="">请选择卫数</option>
                <option value="1">1卫</option>
                <option value="2">2卫</option>
              </select>
            </div>

            <!-- 房龄 -->
            <div class="form-group">
              <label>房龄 (年)</label>
              <input
                v-model.number="predictionForm.age"
                type="number"
                placeholder="请输入房龄"
                min="0"
                max="50"
              />
            </div>

            <!-- 朝向评分 -->
            <div class="form-group">
              <label>朝向评分 (1-5分)</label>
              <select v-model="predictionForm.orientationScore">
                <option value="">请选择朝向评分</option>
                <option value="1">1分 (北向)</option>
                <option value="2">2分 (东/西向)</option>
                <option value="3">3分 (东北/西北)</option>
                <option value="4">4分 (东南/西南)</option>
                <option value="5">5分 (南向)</option>
              </select>
            </div>

            <!-- 到市中心距离 -->
            <div class="form-group">
              <label>到市中心距离 (km)</label>
              <input
                v-model.number="predictionForm.distanceToCenter"
                type="number"
                placeholder="请输入距离"
                min="0"
                max="50"
                step="0.1"
              />
            </div>

            <!-- 经度 -->
            <div class="form-group">
              <label>经度 (可选)</label>
              <input
                v-model.number="predictionForm.longitude"
                type="number"
                placeholder="如：116.4014"
                step="0.0001"
              />
            </div>

            <!-- 纬度 -->
            <div class="form-group">
              <label>纬度 (可选)</label>
              <input
                v-model.number="predictionForm.latitude"
                type="number"
                placeholder="如：39.9263"
                step="0.0001"
              />
            </div>
          </div>

          <div class="form-actions">
            <button @click="predictPrice" class="predict-btn" :disabled="!canPredict || predicting">
              {{ predicting ? '预测中...' : '开始预测' }}
            </button>
          </div>
        </div>

        <!-- 预测结果 -->
        <div v-if="apiPredictionResult" class="prediction-result">
          <div class="result-card">
            <div class="result-header">
              <h3>预测结果</h3>
            </div>

            <div class="price-display">
              <div class="predicted-price">
                ¥{{ Math.round((apiPredictionResult.predictedPricePerSquareMeter / 10000) * predictionForm.area * 10000).toLocaleString() }}
              </div>
              <div class="price-unit">元</div>
            </div>

            <div class="result-details">
              <div class="detail-item">
                <span class="label">单价:</span>
                <span class="value">{{ (apiPredictionResult.predictedPricePerSquareMeter / 10000).toFixed(2) }}万元/㎡</span>
              </div>
              <div class="detail-item">
                <span class="label">城市:</span>
                <span class="value">{{ apiPredictionResult.city }}</span>
              </div>
              <div class="detail-item">
                <span class="label">模型预测:</span>
                <span class="value">zhzharry模型v1.0</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-if="predictionError" class="error-message">
          <div class="error-icon">❌</div>
          <div class="error-text">{{ predictionError }}</div>
        </div>

        <!-- 加载状态 -->
        <div v-if="predicting" class="loading-state">
          <div class="loading-spinner"></div>
          <div class="loading-text">正在计算预测结果...</div>
        </div>
      </div>

      <!-- 贷款计算器 -->
      <div v-if="activeTool === 'mortgage'" class="tool-section">
        <div class="section-header">
          <h2>贷款计算器</h2>
          <p>计算房贷月供和还款计划</p>
        </div>

        <div class="mortgage-form">
          <div class="form-grid">
            <div class="form-group">
              <label>贷款金额（万元）</label>
              <input
                v-model.number="mortgageForm.amount"
                type="number"
                placeholder="请输入贷款金额"
                min="1"
                step="0.1"
              />
            </div>
            <div class="form-group">
              <label>年利率（%）</label>
              <input
                v-model.number="mortgageForm.annualRate"
                type="number"
                placeholder="请输入年利率"
                min="0"
                step="0.01"
              />
            </div>
            <div class="form-group">
              <label>贷款期限（年）</label>
              <input
                v-model.number="mortgageForm.years"
                type="number"
                placeholder="请输入贷款年限"
                min="1"
                max="30"
              />
            </div>
            <div class="form-group">
              <label>还款方式</label>
              <select v-model="mortgageForm.repaymentType">
                <option value="equal-principal-interest">等额本息</option>
                <option value="equal-principal">等额本金</option>
              </select>
            </div>
          </div>

          <div class="form-actions">
            <button
              class="calculate-btn"
              @click="calculateMortgage"
              :disabled="!canCalculateMortgage"
            >
              计算
            </button>
          </div>
        </div>

        <!-- 计算结果 -->
        <div v-if="mortgageResult" class="mortgage-result">
          <div class="result-cards">
            <div class="result-card">
              <div class="card-icon">💰</div>
              <div class="card-content">
                <div class="card-value">{{ formatCurrency(mortgageResult.monthlyPayment) }}</div>
                <div class="card-label">月均还款</div>
              </div>
            </div>
            <div class="result-card">
              <div class="card-icon">📊</div>
              <div class="card-content">
                <div class="card-value">{{ formatCurrency(mortgageResult.totalInterest) }}</div>
                <div class="card-label">总利息</div>
              </div>
            </div>
            <div class="result-card">
              <div class="card-icon">🏦</div>
              <div class="card-content">
                <div class="card-value">{{ formatCurrency(mortgageResult.totalPayment) }}</div>
                <div class="card-label">总还款</div>
              </div>
            </div>
          </div>

          <div class="repayment-plan">
            <h4>还款计划（前12个月）</h4>
            <div class="plan-table">
              <div class="table-header">
                <div class="table-cell">期数</div>
                <div class="table-cell">月供</div>
                <div class="table-cell">本金</div>
                <div class="table-cell">利息</div>
                <div class="table-cell">剩余本金</div>
              </div>
              <div
                v-for="item in mortgageResult.schedule.slice(0, 12)"
                :key="item.period"
                class="table-row"
              >
                <div class="table-cell">{{ item.period }}</div>
                <div class="table-cell">{{ formatCurrency(item.payment) }}</div>
                <div class="table-cell">{{ formatCurrency(item.principal) }}</div>
                <div class="table-cell">{{ formatCurrency(item.interest) }}</div>
                <div class="table-cell">{{ formatCurrency(item.remainingPrincipal) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 价格热力图 -->
      <div v-if="activeTool === 'heatmap'" class="tool-section">
        <div class="section-header">
          <h2>价格热力图</h2>
          <p>查看不同区域房价分布情况</p>
        </div>

        <div class="heatmap-controls">
          <div class="control-group">
            <label>选择城市</label>
            <select v-model="selectedCity" @change="updateHeatmap">
              <option value="北京">北京</option>
              <option value="上海">上海</option>
              <option value="深圳">深圳</option>
              <option value="广州">广州</option>
            </select>
          </div>
          <div class="control-group">
            <label>价格范围</label>
            <select v-model="priceRange">
              <option value="all">全部价格</option>
              <option value="low">3-5万/㎡</option>
              <option value="medium">5-8万/㎡</option>
              <option value="high">8-12万/㎡</option>
              <option value="luxury">12万以上/㎡</option>
            </select>
          </div>
        </div>

        <div class="heatmap-container">
          <div class="heatmap-placeholder">
            <div class="map-grid">
              <div
                v-for="district in heatmapData"
                :key="district.name"
                :class="['map-district', district.priceLevel]"
                @click="selectDistrict(district)"
              >
                <div class="district-name">{{ district.name }}</div>
                <div class="district-price">{{ district.avgPrice }}万/㎡</div>
              </div>
            </div>
          </div>
        </div>

        <div class="heatmap-legend">
          <div class="legend-title">价格等级图例</div>
          <div class="legend-gradation">
            <div class="gradation-item low">低价</div>
            <div class="gradation-item medium-low">较低</div>
            <div class="gradation-item medium">中等</div>
            <div class="gradation-item medium-high">较高</div>
            <div class="gradation-item high">高价</div>
            <div class="gradation-item luxury">豪宅</div>
          </div>
        </div>

        <div v-if="selectedDistrict" class="district-detail">
          <h4>{{ selectedDistrict.name }} 区域详情</h4>
          <div class="detail-stats">
            <div class="stat">
              <div class="stat-value">{{ selectedDistrict.avgPrice }}</div>
              <div class="stat-label">平均价格（万/㎡）</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ selectedDistrict.totalProperties }}</div>
              <div class="stat-label">房源数量</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ selectedDistrict.priceChange }}%</div>
              <div class="stat-label">价格变化</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <button class="nav-btn" @click="$router.push('/profile')">
        <div class="nav-icon">👤</div>
        <div class="nav-label">我的</div>
      </button>
      <button class="nav-btn" @click="$router.push('/search')">
        <div class="nav-icon">🔍</div>
        <div class="nav-label">搜索</div>
      </button>

      <button class="nav-btn active">
        <div class="nav-icon">📊</div>
        <div class="nav-label">工具</div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

import { profileAPI } from '@/api/profile.api'
import type { PricePredictionResponse, PricePredictionFeatures } from '@/types/api.types'



// 当前激活的工具
const activeTool = ref<string>('prediction')

// 房价预测表单
const predictionForm = reactive({
  city: '',
  area: 0,
  roomCount: '', // 室数
  livingRoomCount: '', // 厅数
  bathroomCount: '', // 卫数
  age: 0, // 房龄
  orientationScore: '', // 朝向评分
  distanceToCenter: 0, // 到市中心距离
  longitude: undefined as number | undefined,
  latitude: undefined as number | undefined,
})

const predicting = ref(false)
const predictionError = ref('')

const apiPredictionResult = ref<PricePredictionResponse | null>(null)

// 兼容原有接口的预测结果
const predictionResult = ref({
  predictedPrice: 0,
  marketPrice: 0,
  confidence: 85,
  deviation: 0,
})

// 贷款计算表单
const mortgageForm = reactive({
  amount: 0, // 贷款金额（万元）
  annualRate: 0, // 年利率（%）
  years: 0, // 贷款年限
  repaymentType: 'equal-principal-interest', // 还款方式
})

const mortgageResult = ref<{
  monthlyPayment: number
  totalInterest: number
  totalPayment: number
  schedule: Array<{
    period: number
    payment: number
    principal: number
    interest: number
    remainingPrincipal: number
  }>
} | null>(null)

// 热力图相关数据
const selectedCity = ref('北京')
const priceRange = ref('all')
const heatmapData = ref([
  { name: '朝阳区', avgPrice: 8.5, priceLevel: 'high', totalProperties: 1250, priceChange: 2.3 },
  { name: '海淀区', avgPrice: 9.2, priceLevel: 'luxury', totalProperties: 980, priceChange: 1.8 },
  { name: '西城区', avgPrice: 10.1, priceLevel: 'luxury', totalProperties: 750, priceChange: 0.9 },
  { name: '东城区', avgPrice: 9.8, priceLevel: 'luxury', totalProperties: 680, priceChange: 1.2 },
  { name: '丰台区', avgPrice: 6.8, priceLevel: 'medium', totalProperties: 1100, priceChange: 3.1 },
  { name: '石景山区', avgPrice: 5.9, priceLevel: 'medium-low', totalProperties: 650, priceChange: 2.8 },
  { name: '昌平区', avgPrice: 4.2, priceLevel: 'low', totalProperties: 890, priceChange: 4.5 },
  { name: '大兴区', avgPrice: 3.8, priceLevel: 'low', totalProperties: 720, priceChange: 5.2 },
  { name: '通州区', avgPrice: 5.1, priceLevel: 'medium-low', totalProperties: 950, priceChange: 3.8 },
  { name: '顺义区', avgPrice: 4.8, priceLevel: 'low', totalProperties: 580, priceChange: 4.1 },
  { name: '房山区', avgPrice: 4.5, priceLevel: 'low', totalProperties: 420, priceChange: 3.9 },
  { name: '门头沟区', avgPrice: 3.2, priceLevel: 'low', totalProperties: 280, priceChange: 2.5 }
])

const selectedDistrict = ref<{
  name: string
  avgPrice: number
  priceLevel: string
  totalProperties: number
  priceChange: number
} | null>(null)

// 计算属性
const canPredict = computed(() => {
  return (
    predictionForm.city &&
    predictionForm.area > 0 &&
    predictionForm.roomCount &&
    predictionForm.livingRoomCount &&
    predictionForm.bathroomCount &&
    predictionForm.age >= 0
  )
})

// 贷款计算验证
const canCalculateMortgage = computed(() => {
  return mortgageForm.amount > 0 && mortgageForm.annualRate > 0 && mortgageForm.years > 0
})

// 城市选择变化处理
const onCityChange = () => {
  // 根据城市设置默认的经纬度和距离
  const cityDefaults: Record<string, { lng: number; lat: number; distance: number }> = {
    北京: { lng: 116.4014, lat: 39.9263, distance: 8.5 },
    上海: { lng: 121.4737, lat: 31.2304, distance: 5.0 },
    天津: { lng: 117.1902, lat: 39.1256, distance: 6.0 },
    石家庄: { lng: 114.5149, lat: 38.0428, distance: 4.0 },
  }

  const defaults = cityDefaults[predictionForm.city]
  if (defaults) {
    predictionForm.longitude = defaults.lng
    predictionForm.latitude = defaults.lat
    if (!predictionForm.distanceToCenter) {
      predictionForm.distanceToCenter = defaults.distance
    }
  }
}

// 房价预测方法 - 调用真实API
const predictPrice = async () => {
  if (!canPredict.value) return

  predicting.value = true
  predictionError.value = ''
  apiPredictionResult.value = null

  try {
    // 构建特征数据 - 注意字段名要与后端一致
    const features: PricePredictionFeatures = {
      面积: predictionForm.area,
      室数: parseInt(predictionForm.roomCount),
      厅数: parseInt(predictionForm.livingRoomCount),
      卫数: parseInt(predictionForm.bathroomCount),
      房龄: predictionForm.age,
    }

    // 添加可选字段
    if (predictionForm.orientationScore) {
      features.朝向评分 = parseInt(predictionForm.orientationScore)
    }

    if (predictionForm.distanceToCenter) {
      features['到市中心距离_km'] = predictionForm.distanceToCenter
    }

    if (predictionForm.longitude) {
      features.经度 = predictionForm.longitude
    }

    if (predictionForm.latitude) {
      features.纬度 = predictionForm.latitude
    }

    // 构建请求数据
    const requestData = {
      city: predictionForm.city,
      features: features,
    }

    console.log('发送预测请求:', requestData)

    // 调用API
    const result = await profileAPI.predictPrice(requestData)
    console.log('预测API响应:', result)

    apiPredictionResult.value = result

    // 更新兼容性结果
    predictionResult.value = {
      predictedPrice: result.predictedPricePerSquareMeter,
      marketPrice: result.predictedPricePerSquareMeter * 0.9,
      confidence: 85,
      deviation: Math.round((Math.random() - 0.5) * 10),
    }


// 修正后的代码：
} catch (error: unknown) {  // ✅ 使用 unknown 代替 any
  console.error('房价预测失败:', error)

  // 处理错误信息
  let errorMessage = '预测失败，请稍后重试'

  // 类型安全的错误处理
  if (error && typeof error === 'object') {
    // 处理 AxiosError
    const axiosError = error as { response?: { data?: { message?: string } } }
    if (axiosError.response?.data?.message) {
      errorMessage = axiosError.response.data.message
    } else if ('message' in error && typeof error.message === 'string') {
      // 处理普通 Error 对象
      errorMessage = error.message
    }
  }

  predictionError.value = errorMessage

    // 模拟一个结果用于展示（开发环境下）
    if (import.meta.env.DEV) {
      console.log('开发模式：使用模拟数据')
      const mockPrice = 50000 + Math.random() * 20000
      apiPredictionResult.value = {
        city: predictionForm.city,
        features: {},
        predictedPricePerSquareMeter: mockPrice,
        unit: '万元/㎡',
        message: '模拟预测结果'
      }

      predictionResult.value = {
        predictedPrice: mockPrice,
        marketPrice: mockPrice * 0.9,
        confidence: Math.round(80 + Math.random() * 15),
        deviation: Math.round((Math.random() - 0.5) * 10),
      }
    }
  } finally {
    predicting.value = false
  }
}

// 贷款计算方法
const calculateMortgage = () => {
  if (!canCalculateMortgage.value) return

  const principal = mortgageForm.amount * 10000 // 转换为元
  const monthlyRate = mortgageForm.annualRate / 100 / 12 // 月利率
  const totalMonths = mortgageForm.years * 12 // 总月数

  const schedule = []
  let totalInterest = 0
  let monthlyPayment = 0

  if (mortgageForm.repaymentType === 'equal-principal-interest') {
    // 等额本息
    const ratePow = Math.pow(1 + monthlyRate, totalMonths)
    monthlyPayment = principal * monthlyRate * ratePow / (ratePow - 1)

    let remainingPrincipal = principal
    for (let month = 1; month <= totalMonths; month++) {
      const interest = remainingPrincipal * monthlyRate
      const principalPayment = monthlyPayment - interest
      remainingPrincipal -= principalPayment
      totalInterest += interest

      schedule.push({
        period: month,
        payment: monthlyPayment,
        principal: principalPayment,
        interest: interest,
        remainingPrincipal: Math.max(0, remainingPrincipal)
      })
    }
  } else {
    // 等额本金
    const principalPayment = principal / totalMonths

    let remainingPrincipal = principal
    for (let month = 1; month <= totalMonths; month++) {
      const interest = remainingPrincipal * monthlyRate
      const payment = principalPayment + interest
      remainingPrincipal -= principalPayment
      totalInterest += interest

      schedule.push({
        period: month,
        payment: payment,
        principal: principalPayment,
        interest: interest,
        remainingPrincipal: Math.max(0, remainingPrincipal)
      })
    }
  }

  mortgageResult.value = {
    monthlyPayment: monthlyPayment,
    totalInterest: totalInterest,
    totalPayment: principal + totalInterest,
    schedule: schedule
  }
}

// 格式化货币
const formatCurrency = (value: number) => {
  return value.toLocaleString('zh-CN', { style: 'currency', currency: 'CNY' })
}

// 更新热力图数据
const updateHeatmap = () => {
  // 这里可以根据城市和价格范围过滤数据
  // 目前使用模拟数据，实际应用中应该调用API
  console.log('更新热力图:', selectedCity.value, priceRange.value)
  selectedDistrict.value = null // 重置选中区域
}

// 选择区域
const selectDistrict = (district: { name: string; avgPrice: number; priceLevel: string; totalProperties: number; priceChange: number }) => {
  selectedDistrict.value = district
}

// ================ 以下部分保持不变 ================
















// 模拟价格趋势数据
// const priceTrend = ref([
//   { month: '1月', value: 45000 },
//   { month: '2月', value: 45500 },
//   { month: '3月', value: 46000 },
//   { month: '4月', value: 46500 },
//   { month: '5月', value: 47000 },
//   { month: '6月', value: 47500 },
//   { month: '7月', value: 48000 },
//   { month: '8月', value: 48500 },
//   { month: '9月', value: 49000 },
//   { month: '10月', value: 49500 },
//   { month: '11月', value: 50000 },
//   { month: '12月', value: 50500 },
// ])




</script>

<style scoped>
.tools-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 70px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #eee;
}

.logo {
  font-size: 18px;
  font-weight: bold;
  color: #007bff;
  cursor: pointer;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 15px;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 8px;
}

.tools-nav {
  display: flex;
  background: white;
  padding: 0 20px;
  border-bottom: 1px solid #eee;
}

.nav-tab {
  flex: 1;
  padding: 15px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.nav-tab.active {
  color: #007bff;
  border-bottom-color: #007bff;
}

.tools-content {
  padding: 20px;
}

.tool-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  text-align: center;
  margin-bottom: 30px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.section-header p {
  color: #666;
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-group input,
.form-group select {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #007bff;
}

.form-actions {
  text-align: center;
}

.predict-btn,
.calculate-btn {
  padding: 14px 40px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.predict-btn:disabled,
.calculate-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.predict-btn:hover:not(:disabled),
.calculate-btn:hover:not(:disabled) {
  background: #0056b3;
}

/* 预测结果样式 */
.prediction-result {
  margin-top: 30px;
}

.result-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-header h3 {
  margin: 0;
  font-size: 18px;
}

.confidence {
  background: rgba(255, 255, 255, 0.2);
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
}

.price-display {
  text-align: center;
  margin-bottom: 20px;
}

.predicted-price {
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 4px;
}

.price-unit {
  font-size: 14px;
  opacity: 0.8;
}

.result-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.detail-item {
  text-align: center;
}

.detail-item .label {
  display: block;
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.detail-item .value {
  font-size: 16px;
  font-weight: 600;
}

.detail-item .value.positive {
  color: #52c41a;
}

.detail-item .value.negative {
  color: #ff4d4f;
}

/* 趋势图样式 */
.trend-chart {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.trend-chart h4 {
  margin: 0 0 16px 0;
  color: #333;
}

.chart-container {
  height: 200px;
  margin-bottom: 16px;
}

.chart-placeholder {
  background: white;
  border-radius: 8px;
  padding: 16px;
  height: 100%;
}

.chart-bars {
  display: flex;
  align-items: end;
  justify-content: space-around;
  height: 100%;
}

.chart-bar {
  width: 30px;
  background: linear-gradient(to top, #007bff, #66b3ff);
  border-radius: 4px 4px 0 0;
  position: relative;
  transition: height 0.3s;
}

.bar-value {
  position: absolute;
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  white-space: nowrap;
}

.bar-label {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #666;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-color.historical {
  background: #007bff;
}

.legend-color.predicted {
  background: #52c41a;
}

/* 贷款计算结果样式 */
.mortgage-result {
  margin-top: 30px;
}

.result-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.result-cards .result-card {
  background: white;
  border: 1px solid #f0f0f0;
  color: #333;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-cards .card-icon {
  font-size: 24px;
}

.result-cards .card-content {
  flex: 1;
}

.result-cards .card-value {
  font-size: 20px;
  font-weight: bold;
  color: #007bff;
  margin-bottom: 4px;
}

.result-cards .card-label {
  font-size: 12px;
  color: #666;
}

.repayment-plan {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.repayment-plan h4 {
  margin: 0 0 16px 0;
  color: #333;
}

.plan-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.table-header,
.table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
  gap: 1px;
}

.table-header {
  background: #007bff;
  color: white;
}

.table-cell {
  padding: 12px 8px;
  font-size: 12px;
  text-align: center;
}

.table-row {
  background: white;
}

.table-row:nth-child(even) {
  background: #f8f9fa;
}

.table-row .table-cell {
  color: #333;
}

/* 热力图样式 */
.heatmap-controls {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.control-group {
  display: flex;
  flex-direction: column;
}

.control-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.control-group select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.heatmap-container {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.heatmap-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  width: 100%;
}

.map-district {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.map-district:hover {
  transform: scale(1.05);
}

.map-district.low {
  background: #e6f7ff;
}
.map-district.medium-low {
  background: #bae7ff;
}
.map-district.medium {
  background: #69c0ff;
}
.map-district.medium-high {
  background: #1890ff;
}
.map-district.high {
  background: #096dd9;
}
.map-district.luxury {
  background: #0050b3;
}

.district-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.district-price {
  font-size: 12px;
  color: #666;
}

.heatmap-legend {
  text-align: center;
  margin-top: 20px;
}

.legend-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.legend-gradation {
  display: flex;
  justify-content: center;
  gap: 4px;
}

.gradation-item {
  padding: 6px 12px;
  font-size: 11px;
  border-radius: 4px;
  color: white;
}

.gradation-item.low {
  background: #e6f7ff;
  color: #333;
}
.gradation-item.medium-low {
  background: #bae7ff;
  color: #333;
}
.gradation-item.medium {
  background: #69c0ff;
  color: white;
}
.gradation-item.medium-high {
  background: #1890ff;
  color: white;
}
.gradation-item.high {
  background: #096dd9;
  color: white;
}
.gradation-item.luxury {
  background: #0050b3;
  color: white;
}

.district-detail {
  background: white;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #007bff;
}

.district-detail h4 {
  margin: 0 0 16px 0;
  color: #333;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat {
  text-align: center;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #007bff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

/* 底部导航 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 8px 0;
}

.nav-btn {
  flex: 1;
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  transition: color 0.3s ease;
}

.nav-btn.active {
  color: #007bff;
}
.error-message {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 8px;
  padding: 16px;
  margin-top: 20px;
}

.error-icon {
  font-size: 20px;
}

.error-text {
  color: #ff4d4f;
  font-size: 14px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 30px;
  padding: 30px;
  background: #f8f9fa;
  border-radius: 8px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #666;
  font-size: 14px;
}
</style>
