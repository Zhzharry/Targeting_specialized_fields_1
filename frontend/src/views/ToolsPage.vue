<!-- src/views/ToolsPage.vue -->
<template>
  <div class="tools-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="logo" @click="$router.push('/search')">房产平台</div>
      <nav class="top-nav">
        <button class="nav-link" @click="$router.push('/search')">
          <span class="nav-icon">🔍</span>
          <span>搜索</span>
        </button>
        <button class="nav-link active" @click="$router.push('/tools')">
          <span class="nav-icon">📊</span>
          <span>工具</span>
        </button>
        <button class="nav-link" @click="$router.push('/profile')">
          <span class="nav-icon">👤</span>
          <span>我的</span>
        </button>
      </nav>
      <div class="header-actions">
        <button class="icon-btn" @click="$router.push('/login')">
          <span>🔑</span>
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
          <p>基于高德地图实时查看不同区域房价分布情况</p>
        </div>

        <div class="heatmap-controls">
          <div class="control-group">
            <label>选择城市</label>
            <select v-model="selectedCity" @change="onHeatmapCityChange">
              <option value="beijing">北京</option>
              <option value="shanghai">上海</option>
              <option value="guangzhou">广州</option>
              <option value="shenzhen">深圳</option>
            </select>
          </div>
          <div class="control-group">
            <label>价格范围</label>
            <select v-model="priceRange" @change="loadHeatmapData">
              <option value="all">全部价格</option>
              <option value="0-50000">0-5万/㎡</option>
              <option value="50000-100000">5-10万/㎡</option>
              <option value="100000-150000">10-15万/㎡</option>
              <option value="150000-">15万/㎡以上</option>
            </select>
          </div>
          <div class="control-group">
            <label>热力图强度</label>
            <input
              type="range"
              v-model="heatmapOpacity"
              min="0"
              max="1"
              step="0.1"
              @input="updateHeatmapOpacity"
            />
            <span>{{ (heatmapOpacity * 100).toFixed(0) }}%</span>
          </div>
          <div class="control-group">
            <label>缩放模式</label>
            <select v-model="heatmapScaleMode" @change="loadHeatmapData">
              <option value="linear">线性</option>
              <option value="log">对数（默认）</option>
              <option value="gamma">Gamma 校正</option>
            </select>
          </div>
          <div class="control-group">
            <label>Gamma 值</label>
            <input type="number" v-model.number="heatmapGamma" min="0.1" step="0.1" @change="loadHeatmapData" />
          </div>
          <button class="refresh-btn" @click="loadHeatmapData">
            <span>🔄</span> 刷新数据
          </button>
        </div>

        <div class="amap-wrapper">
          <!-- 高德地图容器 -->
          <div id="amap-heatmap-container" class="amap-container"></div>

          <!-- 图例（动态根据配置生成） -->
          <div class="amap-legend">
            <div class="legend-title">房价区间（万元/㎡）</div>
            <div class="legend-items">
              <div v-for="(item, idx) in legendItems" :key="idx" class="legend-item">
                <div class="color-box" :style="{ background: item.color }"></div>
                <div class="legend-label">{{ item.label }}</div>
              </div>
            </div>

            <div class="legend-controls">
                <label>最大展示值：</label>
                <input type="checkbox" v-model="autoHeatmapMax" id="autoHeatmap" /> <label for="autoHeatmap">自动</label>
                <input :disabled="autoHeatmapMax" type="number" v-model.number="heatmapMax" min="1" step="1" @change="loadHeatmapData" />
                <small>（单位：万/㎡）</small>
            </div>
          </div>

          <!-- 加载提示 -->
          <div v-if="heatmapLoading" class="loading-overlay">
            <div class="loading-spinner"></div>
            <p>正在加载房价数据...</p>
          </div>
        </div>

        <div v-if="heatmapStats" class="heatmap-stats">
          <h4>数据统计</h4>
          <div class="stats-grid">
            <div class="stat">
              <div class="stat-value">{{ heatmapStats.totalCount }}</div>
              <div class="stat-label">房源总数</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ (heatmapStats.avgPrice / 10000).toFixed(2) }}万</div>
              <div class="stat-label">平均单价</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ (heatmapStats.minPrice / 10000).toFixed(2) }}万</div>
              <div class="stat-label">最低单价</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ (heatmapStats.maxPrice / 10000).toFixed(2) }}万</div>
              <div class="stat-label">最高单价</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'

import { profileAPI } from '@/api/profile.api'
import { heatmapAPI } from '@/api/heatmap.api'
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
const selectedCity = ref('beijing')
const priceRange = ref('all')
const heatmapOpacity = ref(0.8)
const heatmapLoading = ref(false)
const heatmapStats = ref<{
  totalCount: number
  avgPrice: number
  minPrice: number
  maxPrice: number
} | null>(null)

// 热力图配置（可调整）
const heatmapMax = ref(18) // 单位：万元/㎡，展示上限
// 渐变映射（保持与heatmapLayer一致）
const heatmapGradientStops = reactive<Record<number, string>>({
  0.0: 'rgba(0, 0, 255, 0)',
  0.25: 'blue',
  0.45: 'cyan',
  0.62: 'lime',
  0.75: 'yellow',
  0.85: 'orange',
  0.92: 'orangered',
  1.0: 'red'
})

// 缩放模式：linear | log | gamma
const heatmapScaleMode = ref<'linear' | 'log' | 'gamma'>('log')
const heatmapGamma = ref(0.6)

// 自动 heatmapMax 开关：若开启，则基于数据计算 95th 百分位并乘以 1.1
const autoHeatmapMax = ref<boolean>(true)

// 是否使用连续渐变图例（保留为未来扩展开关）
const quantileLegend = ref<boolean>(true)

// 根据 gradientStops, heatmapMax 以及可选的 quantile 分位数生成图例项
const legendItems = computed(() => {
  // 如果没有数据或不开启 quantileLegend，则回退到基于 gradientStops 的静态图例
  try {
    const stops = Object.keys(heatmapGradientStops)
      .map(k => parseFloat(k))
      .filter(n => !isNaN(n))
      .sort((a, b) => a - b)

    const items: Array<{ color: string; label: string }> = []

    // 当 quantileLegend 打开并且 heatmapStats 可用时，使用五等分（四个分位点）来生成更能反映数据分布的图例
    if (quantileLegend.value && heatmapStats.value) {
      // 生成5等分（四个边界）: 0%,25%,50%,75%,100% -> 我们标注为区间
      // 更精确的分位数已在 loadHeatmapData 中计算并写入 heatmapQuantiles
      const quantiles = heatmapQuantiles.value ?? []
      if (quantiles.length > 0) {
        // 预计算梯度 keys（安全的数值索引）
        const gradientKeys = Object.keys(heatmapGradientStops)
          .map(k => parseFloat(k))
          .filter(n => !isNaN(n))
          .sort((a, b) => a - b)

        // quantiles 包含 [p0,p25,p50,p75,p100]
        for (let i = 1; i < quantiles.length; i++) {
          const prevVal = quantiles[i - 1]
          const curVal = quantiles[i]
          if (prevVal === undefined || curVal === undefined) continue
          const low = +(prevVal / 10000).toFixed(2)
          const high = +(curVal / 10000).toFixed(2)
          const colorIndex = Math.min(gradientKeys.length - 1, i)
          const colorKey = gradientKeys[colorIndex] ?? gradientKeys[gradientKeys.length - 1] ?? 1
          const color = heatmapGradientStops[colorKey] ?? 'red'
          let label = ''
          if (i === 1) label = `≤${high}万/㎡`
          else if (i === quantiles.length - 1) label = `>${low}万/㎡`
          else label = `${low}-${high}万/㎡`
          items.push({ color, label })
        }
        return items
      }
    }

    // 退回到基于 gradientStops 的静态图例（按 heatmapMax 分段）
    for (let i = 1; i < stops.length; i++) {
      const prev = stops[i - 1]
      const cur = stops[i]
      if (prev === undefined || cur === undefined) continue
      const low = +(prev * heatmapMax.value).toFixed(2)
      const high = +(cur * heatmapMax.value).toFixed(2)

      let label = ''
      if (i === 1) {
        label = `≤${high}万/㎡`
      } else if (i === stops.length - 1) {
        label = `>${low}万/㎡`
      } else {
        label = `${low}-${high}万/㎡`
      }

      items.push({ color: heatmapGradientStops[cur] ?? 'red', label })
    }

    return items
  } catch {
    return []
  }
})

// 存储在 loadHeatmapData 中计算的分位数（以价格元为单位）: [p0,p25,p50,p75,p100]
const heatmapQuantiles = ref<number[] | null>(null)

// 高德地图相关变量
let amapInstance: unknown = null
type HeatmapLayerType = {
  setDataSet?: (opts: { data: Array<{ lng: number; lat: number; count: number }>; max?: number }) => void
  setOptions?: (opts: Record<string, unknown>) => void
  show?: () => void
  getOptions?: () => unknown
}
let heatmapLayer: HeatmapLayerType | null = null

// 高德地图类型声明
declare global {
  interface Window {
    AMap: unknown
    AMapLoader: unknown
  }
}

// AMap minimal typing to avoid blanket `any` casts
type AMapMapInstance = {
  plugin: (plugs: string[], cb: () => void) => void
  on: (event: string, cb: () => void) => void
  getZoom?: () => number
  setCenter?: (center: [number, number]) => void
  destroy?: () => void
}

type AMapType = {
  Map: new (id: string, opts?: Record<string, unknown>) => AMapMapInstance
  HeatMap: new (map: AMapMapInstance, opts?: Record<string, unknown>) => HeatmapLayerType
}

// 城市中心点坐标
const cityCenters: Record<string, [number, number]> = {
  beijing: [116.397428, 39.90923],
  shanghai: [121.473701, 31.230416],
  guangzhou: [113.264385, 23.129112],
  shenzhen: [114.057868, 22.543099]
}

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

// ========== 热力图相关方法 ==========

// 初始化高德地图
const initAMap = async () => {
  try {
    console.log('开始初始化高德地图...')

    // 检查是否已经加载了 AMapLoader
    if (!window.AMapLoader) {
      console.log('加载 AMapLoader 脚本...')
      await loadAMapScript()
    }

    // 使用 AMapLoader 加载高德地图
    const AMapLoader = (window as { AMapLoader: { load: (config: Record<string, unknown>) => Promise<unknown> } }).AMapLoader

    console.log('加载高德地图 API...')
    const AMap = await AMapLoader.load({
      key: 'cc670ed2985b867a48128c2fdd4f1ced', // 高德地图Key
      version: '2.0',
      plugins: ['AMap.HeatMap'],
      securityJsCode: '7e0958da238286d2a0b24757fb1292c6', // 如果在高德平台配置了安全密钥，取消注释并填入
    })

    console.log('高德地图 API 加载成功')
    window.AMap = AMap

    // 创建地图实例
    console.log('创建地图实例...')
    // 使用更精确的类型替代 any
    const AMapLib = (AMap as unknown) as AMapType
    amapInstance = new AMapLib.Map('amap-heatmap-container', {
      zoom: 11,
      center: cityCenters[selectedCity.value],
      mapStyle: 'amap://styles/light'
    })

    console.log('地图实例创建成功，加载热力图插件...');

        // 创建热力图图层
    (amapInstance as AMapMapInstance).plugin(['AMap.HeatMap'], () => {
      console.log('热力图插件加载成功，创建热力图层...')

      try {
        heatmapLayer = new AMapLib.HeatMap(amapInstance as AMapMapInstance, {
          radius: 45, // 热力半径（像素），增大覆盖范围
          opacity: [0, 0.85], // 透明度范围
          gradient: heatmapGradientStops,
          blur: 0.75, // 清晰模式，减少颜色混合
          visible: true, // 确保可见
          zooms: [3, 20], // 显示的缩放级别范围
          '3Dlayer': false,
          rejectMapMask: true // 不受地图蒙版影响
        })

        console.log('热力图层创建成功，层对象:', heatmapLayer);
        console.log('开始加载数据...');

        // 监听地图缩放事件，动态调整热力图半径
        // 原因：radius是屏幕像素值，放大地图时需要增大像素才能保持视觉上的地理覆盖范围
        (amapInstance as AMapMapInstance).on('zoomchange', () => {
          const zoom = (amapInstance as AMapMapInstance).getZoom ? (amapInstance as AMapMapInstance).getZoom!() : 11
          // 根据缩放级别计算合适的半径
          // zoom 11: 45px, zoom 15: 95px (指数增长更自然)
          const baseRadius = 45;
          const zoomFactor = Math.pow(1.15, zoom - 11); // 每级放大1.15倍
          const dynamicRadius = Math.round(baseRadius * zoomFactor);
          const clampedRadius = Math.max(30, Math.min(180, dynamicRadius)); // 限制在30-180之间

          if (heatmapLayer) {
            if (heatmapLayer && typeof heatmapLayer.setOptions === 'function') {
              heatmapLayer.setOptions?.({ radius: clampedRadius })
            }
            console.log(`地图缩放级别: ${zoom.toFixed(1)}, 热力图半径: ${clampedRadius}px`);
          }
        });

        // 加载热力图数据
        loadHeatmapData()
      } catch (error) {
        console.error('创建热力图层失败:', error)
      }
    })
  } catch (error) {
    console.error('❌ 地图初始化失败:', error)
    alert('地图初始化失败，请检查高德地图Key是否正确')
  }
}

// 动态加载高德地图脚本
const loadAMapScript = (): Promise<unknown> => {
  return new Promise((resolve, reject) => {
    if (window.AMapLoader) {
      resolve(window.AMapLoader)
      return
    }

    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/loader.js'
    script.onload = () => {
      resolve(window.AMapLoader)
    }
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 加载热力图数据
const loadHeatmapData = async () => {
  if (!heatmapLayer) {
    console.warn('热力图层未初始化')
    return
  }

  heatmapLoading.value = true
  try {
    console.log('开始加载热力图数据:', { city: selectedCity.value, priceRange: priceRange.value })

    const response = await heatmapAPI.getHeatmapData({
      city: selectedCity.value,
      priceRange: priceRange.value
    })

    console.log('API响应:', response)

    if (response.success && response.data && response.data.length > 0) {
      // 转换数据格式为高德地图热力图需要的格式
      const formattedData: Array<{ lng: number; lat: number; count: number }> = []
      for (const item of response.data) {
        if (item.longitude && item.latitude && item.price) {
          formattedData.push({
            lng: item.longitude,
            lat: item.latitude,
            count: item.price / 10000 // 将价格转换为权重，单位万元
          })
        }
      }

      console.log('格式化后的数据点数量:', formattedData.length)
      console.log('示例数据:', formattedData.slice(0, 3))

        if (formattedData.length > 0) {
        console.log('准备设置热力图数据，数据集:', {
          count: formattedData.length,
          sample: formattedData.slice(0, 3),
            max: heatmapMax.value
        });

        // 计算分位数（基于后端返回的原始价格，单位：元）并保存到 heatmapQuantiles
        const respData = response.data as Array<{ price: number }>
        const pricesAll: number[] = respData.map(it => it.price).sort((a, b) => a - b)
        const percentile = (arr: number[], p: number): number => {
          if (arr.length === 0) return 0
          const idx = (arr.length - 1) * p
          const lo = Math.floor(idx)
          const hi = Math.ceil(idx)
          const safeLo = Math.max(0, Math.min(arr.length - 1, lo))
          const safeHi = Math.max(0, Math.min(arr.length - 1, hi))
          const valLo = arr[safeLo] ?? 0
          const valHi = arr[safeHi] ?? 0
          if (safeLo === safeHi) return valLo
          const weight = idx - lo
          return valLo * (1 - weight) + valHi * weight
        }

        const p0: number = pricesAll.length > 0 ? (pricesAll[0] ?? 0) : 0
        const p25: number = percentile(pricesAll, 0.25)
        const p50: number = percentile(pricesAll, 0.5)
        const p75: number = percentile(pricesAll, 0.75)
        const p100: number = pricesAll.length > 0 ? (pricesAll[pricesAll.length - 1] ?? 0) : 0
        heatmapQuantiles.value = [p0, p25, p50, p75, p100]

        // 如果自动计算 heatmapMax，则用 95th 百分位（单位：元）转换为 万元/㎡ 并乘以 1.1
        if (autoHeatmapMax.value) {
          const p95: number = percentile(pricesAll, 0.95)
          const candidateWan = Math.max(1, Math.round((p95 / 10000) * 1.1))
          heatmapMax.value = candidateWan
          console.log('autoHeatmapMax 启用：95th=', p95, '设置 heatmapMax(万/㎡)=', heatmapMax.value)
        }

        // 使用正确的高德地图热力图API
        // 缩放函数：支持线性 / 对数 / gamma
        const applyScaling = (v: number) => {
            const eps = 1e-6
            const clipped = Math.max(eps, Math.min(v, heatmapMax.value))
            if (heatmapScaleMode.value === 'linear') {
              return clipped
            }
            if (heatmapScaleMode.value === 'log') {
              const log = Math.log10(clipped + 1)
              const maxLog = Math.log10(heatmapMax.value + 1)
              return (log / maxLog) * heatmapMax.value
            }
            // gamma
            const gamma = Math.max(0.1, heatmapGamma.value)
            const normalized = Math.pow(clipped / heatmapMax.value, gamma)
            return normalized * heatmapMax.value
          }

          const scaled = formattedData.map(p => ({
            lng: Number(p.lng ?? 0),
            lat: Number(p.lat ?? 0),
            count: applyScaling(Number(p.count ?? 0))
          }))

          // 使用计算/手动后的展示上限
          const displayMax = heatmapMax.value

          if (heatmapLayer && typeof heatmapLayer.setDataSet === 'function') {
            heatmapLayer.setDataSet?.({ data: scaled, max: displayMax })
          }

        // 显示热力图层
        if (heatmapLayer && typeof heatmapLayer.show === 'function') {
          heatmapLayer.show?.()
        }

        console.log('✅ 热力图数据已设置并显示')
        console.log('热力图层状态:', {
          visible: heatmapLayer && typeof heatmapLayer.getOptions === 'function' ? heatmapLayer.getOptions?.() : 'unknown'
        })
      } else {
        console.warn('⚠️ 没有有效的数据点')
      }

      // 更新统计信息
      if (response.data.length > 0) {
        const prices = response.data.map((item: { price: number }) => item.price)
        heatmapStats.value = {
          totalCount: response.data.length,
          avgPrice: prices.reduce((a: number, b: number) => a + b, 0) / prices.length,
          minPrice: Math.min(...prices),
          maxPrice: Math.max(...prices)
        }
      }
    } else {
      console.warn('⚠️ API返回数据为空或失败:', response)
    }
  } catch (error) {
    console.error('❌ 加载热力图数据失败:', error)
  } finally {
    heatmapLoading.value = false
  }
}

// 城市切换
const onHeatmapCityChange = () => {
    if (amapInstance) {
      const center = cityCenters[selectedCity.value]
      if (center) {
        (amapInstance as AMapMapInstance).setCenter?.(center)
      }
      loadHeatmapData()
    }
}

// 更新热力图透明度
const updateHeatmapOpacity = () => {
  if (heatmapLayer) {
    if (heatmapLayer && typeof heatmapLayer.setOptions === 'function') {
      heatmapLayer.setOptions?.({ opacity: [0, heatmapOpacity.value] })
    }
  }
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

// 生命周期钩子
onMounted(() => {
  // 不在页面加载时初始化地图，等用户切换到热力图标签时再初始化
})

onUnmounted(() => {
  if (amapInstance) {
    (amapInstance as AMapMapInstance).destroy?.()
  }
})

// 监听activeTool变化，当切换到热力图时初始化地图
watch(activeTool, (newValue) => {
  if (newValue === 'heatmap' && !amapInstance) {
    // 延迟初始化，确保DOM已渲染
    setTimeout(() => {
      initAMap()
    }, 100)
  }
})

</script>

<style scoped>
.tools-page {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f7fafc 0%, #edf2f7 100%);
  padding-bottom: 70px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  gap: 40px;
}

.logo {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
  transition: transform 0.2s ease;
  white-space: nowrap;
}

.logo:hover {
  transform: scale(1.05);
}

.top-nav {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 400px;
}

.nav-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #718096;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.nav-link:hover {
  background: #f7fafc;
  color: #2d3748;
}

.nav-link.active {
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.nav-icon {
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 15px;
}

.icon-btn {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border: 2px solid #e2e8f0;
  font-size: 20px;
  cursor: pointer;
  padding: 10px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

.tools-nav {
  display: flex;
  background: white;
  padding: 8px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  gap: 8px;
}

.nav-tab {
  flex: 1;
  padding: 14px 20px;
  background: transparent;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #718096;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.nav-tab:hover {
  background: #f7fafc;
  color: #2d3748;
}

.nav-tab.active {
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.tools-content {
  padding: 24px;
}

.tool-section {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.section-header {
  text-align: center;
  margin-bottom: 36px;
}

.section-header h2 {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.section-header p {
  color: #718096;
  font-size: 15px;
  font-weight: 500;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 10px;
  letter-spacing: 0.3px;
}

.form-group input,
.form-group select {
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: #f7fafc;
  color: #2d3748;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.form-group input::placeholder {
  color: #a0aec0;
}

.form-actions {
  text-align: center;
}

.predict-btn,
.calculate-btn {
  padding: 16px 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.predict-btn::before,
.calculate-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s ease;
}

.predict-btn:hover::before,
.calculate-btn:hover::before {
  left: 100%;
}

.predict-btn:disabled,
.calculate-btn:disabled {
  background: linear-gradient(135deg, #a0aec0 0%, #718096 100%);
  cursor: not-allowed;
  box-shadow: none;
}

.predict-btn:hover:not(:disabled),
.calculate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.predict-btn:active:not(:disabled),
.calculate-btn:active:not(:disabled) {
  transform: translateY(0);
}

/* 预测结果样式 */
.prediction-result {
  margin-top: 36px;
}

.result-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 32px;
  border-radius: 20px;
  margin-bottom: 28px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.result-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1), transparent);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.result-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.confidence {
  background: rgba(255, 255, 255, 0.25);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.price-display {
  text-align: center;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

.predicted-price {
  font-size: 48px;
  font-weight: 900;
  margin-bottom: 8px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.price-unit {
  font-size: 16px;
  opacity: 0.9;
  font-weight: 500;
}

.result-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  position: relative;
  z-index: 1;
}

.detail-item {
  text-align: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.detail-item .label {
  display: block;
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
  font-weight: 500;
}

.detail-item .value {
  font-size: 18px;
  font-weight: 700;
}

.detail-item .value.positive {
  color: #48bb78;
}

.detail-item .value.negative {
  color: #f56565;
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
  gap: 20px;
  margin-bottom: 28px;
}

.result-cards .result-card {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border: 2px solid #e2e8f0;
  color: #2d3748;
  padding: 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.result-cards .result-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.15);
  border-color: #667eea;
}

.result-cards .card-icon {
  font-size: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.result-cards .card-content {
  flex: 1;
}

.result-cards .card-value {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.result-cards .card-label {
  font-size: 13px;
  color: #718096;
  font-weight: 600;
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
  margin-bottom: 24px;
  flex-wrap: wrap;
  align-items: flex-end;
  padding: 20px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.control-group label {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.control-group select,
.control-group input[type="range"] {
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
  background: white;
}

.control-group select:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.control-group input[type="range"] {
  width: 160px;
}

.refresh-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
  height: 42px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.refresh-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

/* 高德地图容器 */
.amap-wrapper {
  position: relative;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.amap-container {
  width: 100%;
  height: 500px;
}

/* 地图图例 */
.amap-legend {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: white;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  color: #333;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.color-box {
  width: 20px;
  height: 20px;
  border-radius: 2px;
}

/* 加载覆盖层 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 20;
  color: white;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 统计信息 */
.heatmap-stats {
  background: white;
  padding: 28px;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
}

.heatmap-stats h4 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .amap-container {
    height: 400px;
  }

  .heatmap-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .control-group input[type="range"] {
    width: 100%;
  }

  .refresh-btn {
    width: 100%;
    justify-content: center;
  }
}

.stat {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: #718096;
  font-weight: 600;
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
