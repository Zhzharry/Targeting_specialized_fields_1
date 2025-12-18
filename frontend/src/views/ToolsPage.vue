<!-- src/views/ToolsPage.vue -->
<template>
  <div class="tools-page">
    <!-- 奢华背景装饰 -->
    <div class="luxury-bg">
      <div class="floating-particle p-1"></div>
      <div class="floating-particle p-2"></div>
      <div class="floating-particle p-3"></div>
      <div class="radial-glow"></div>
    </div>

    <!-- 顶部导航 -->
    <header class="header">
      <div class="logo" @click="$router.push('/search')">
        <span class="logo-icon">🏛️</span>
        <span class="logo-text">尊贵房产</span>
      </div>
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
        <div class="prediction-flex">
          <!-- 左侧表单 -->
          <div class="prediction-form prediction-form-col">
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
                <input v-model.number="predictionForm.area" type="number" placeholder="请输入面积" min="20" max="500" />
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
                <input v-model.number="predictionForm.age" type="number" placeholder="请输入房龄" min="0" max="50" />
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
                <input v-model.number="predictionForm.distanceToCenter" type="number" placeholder="请输入距离" min="0" max="50" step="0.1" />
              </div>
              <!-- 经度 -->
              <div class="form-group">
                <label>经度 (可选)</label>
                <input v-model.number="predictionForm.longitude" type="number" placeholder="如：116.4014" step="0.0001" />
              </div>
              <!-- 纬度 -->
              <div class="form-group">
                <label>纬度 (可选)</label>
                <input v-model.number="predictionForm.latitude" type="number" placeholder="如：39.9263" step="0.0001" />
              </div>
            </div>
            <div class="form-actions">
              <button @click="predictPrice" class="predict-btn" :disabled="!canPredict || predicting">
                {{ predicting ? '预测中...' : '开始预测' }}
              </button>
            </div>
          </div>
          <!-- 右侧结果区 -->
          <div class="prediction-result-col">
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
            <div v-else-if="predictionError" class="error-message">
              <div class="error-icon">❌</div>
              <div class="error-text">{{ predictionError }}</div>
            </div>
            <div v-else-if="predicting" class="loading-state">
              <div class="loading-spinner"></div>
              <div class="loading-text">正在计算预测结果...</div>
            </div>
            <div v-else class="empty-result-hint">
              <span>请在左侧填写信息后点击“开始预测”</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 贷款计算器 -->
      <div v-if="activeTool === 'mortgage'" class="tool-section">
        <div class="section-header">
          <h2>贷款计算器</h2>
          <p>计算房贷月供和还款计划</p>
        </div>
        <div class="mortgage-flex">
          <!-- 左侧表单 -->
          <div class="mortgage-form mortgage-form-col">
            <div class="form-grid">
              <div class="form-group">
                <label>贷款金额（万元）</label>
                <input v-model.number="mortgageForm.amount" type="number" placeholder="请输入贷款金额" min="1" step="0.1" />
              </div>
              <div class="form-group">
                <label>年利率（%）</label>
                <input v-model.number="mortgageForm.annualRate" type="number" placeholder="请输入年利率" min="0" step="0.01" />
              </div>
              <div class="form-group">
                <label>贷款期限（年）</label>
                <input v-model.number="mortgageForm.years" type="number" placeholder="请输入贷款年限" min="1" max="30" />
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
              <button class="calculate-btn" @click="calculateMortgage" :disabled="!canCalculateMortgage">计算</button>
            </div>
          </div>
          <!-- 右侧结果区 -->
          <div class="mortgage-result-col">
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
                  <div v-for="item in mortgageResult.schedule.slice(0, 12)" :key="item.period" class="table-row">
                    <div class="table-cell">{{ item.period }}</div>
                    <div class="table-cell">{{ formatCurrency(item.payment) }}</div>
                    <div class="table-cell">{{ formatCurrency(item.principal) }}</div>
                    <div class="table-cell">{{ formatCurrency(item.interest) }}</div>
                    <div class="table-cell">{{ formatCurrency(item.remainingPrincipal) }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-result-hint">
              <span>请在左侧填写信息后点击“计算”</span>
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

<style scoped>
.prediction-flex, .mortgage-flex {
  display: flex;
  flex-direction: column;
  gap: 32px;
}
.prediction-form-col, .mortgage-form-col {
  width: 100%;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.7), rgba(22, 33, 62, 0.8));
  padding: 36px;
  border-radius: 20px;
  border: 2px solid rgba(212, 175, 55, 0.25);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  backdrop-filter: blur(15px);
  position: relative;
  overflow: hidden;
}

.prediction-form-col::before, .mortgage-form-col::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #ffd700, transparent);
  animation: formShimmer 3s ease-in-out infinite;
}

@keyframes formShimmer {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}
.prediction-result-col, .mortgage-result-col {
  width: 100%;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  background: linear-gradient(135deg, rgba(22, 33, 62, 0.5), rgba(26, 26, 46, 0.6));
  padding: 36px;
  border-radius: 20px;
  border: 2px solid rgba(212, 175, 55, 0.2);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(212, 175, 55, 0.08);
  backdrop-filter: blur(12px);
  position: relative;
}
.empty-result-hint {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: rgba(212, 175, 55, 0.6);
  font-size: 18px;
  letter-spacing: 0.5px;
  background: transparent;
  border-radius: 0;
  margin-top: 0;
  box-shadow: none;
}
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-preview ul {
    grid-template-columns: 1fr;
  }
  .predict-btn,
  .calculate-btn {
    min-width: 100%;
  }
}
</style>

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
  background: linear-gradient(135deg, #0f1419 0%, #1a1f2e 50%, #0f1419 100%);
  padding-bottom: 70px;
  position: relative;
  overflow-x: hidden;
}

/* 奢华背景装饰 */
.luxury-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.floating-particle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.2), transparent 70%);
  animation: particleFloat 20s ease-in-out infinite;
}

.p-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -80px;
  animation-delay: 0s;
}

.p-2 {
  width: 250px;
  height: 250px;
  bottom: -80px;
  left: -60px;
  animation-delay: 7s;
}

.p-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 30%;
  animation-delay: 14s;
}

@keyframes particleFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.3;
  }
  50% {
    transform: translate(40px, -40px) scale(1.2);
    opacity: 0.5;
  }
}

.radial-glow {
  position: absolute;
  top: 20%;
  right: 10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.1), transparent 60%);
  filter: blur(60px);
  animation: glowPulse 10s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.1); }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.98) 100%);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.2);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(20px);
  gap: 40px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.logo-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 8px rgba(212, 175, 55, 0.5));
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.logo-text {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 50%, #ffd700 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.logo:hover {
  transform: scale(1.05);
}

.top-nav {
  display: flex;
  gap: 12px;
  flex: 1;
  max-width: 500px;
}

.nav-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(212, 175, 55, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.nav-link:hover {
  background: rgba(212, 175, 55, 0.1);
  color: #ffd700;
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.2);
}

.nav-link.active {
  color: #0f1419;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  border-color: #d4af37;
  box-shadow:
    0 4px 16px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.nav-icon {
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 15px;
}

.icon-btn {
  background: rgba(26, 26, 46, 0.8);
  border: 2px solid rgba(212, 175, 55, 0.3);
  font-size: 20px;
  cursor: pointer;
  padding: 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
  color: #d4af37;
}

.icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(212, 175, 55, 0.4);
  border-color: #d4af37;
  background: rgba(212, 175, 55, 0.1);
}

.tools-nav {
  display: flex;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.9) 0%, rgba(22, 33, 62, 0.95) 100%);
  padding: 12px 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  gap: 12px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
}

.nav-tab {
  flex: 1;
  padding: 16px 24px;
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(212, 175, 55, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.nav-tab:hover {
  background: rgba(212, 175, 55, 0.1);
  color: #ffd700;
  border-color: rgba(212, 175, 55, 0.4);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.2);
}

.nav-tab.active {
  color: #0f1419;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  border-color: #d4af37;
  box-shadow:
    0 4px 16px rgba(212, 175, 55, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.tools-content {
  padding: 32px 24px;
  position: relative;
  z-index: 1;
}

.tool-section {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.9) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 24px;
  padding: 40px;
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(212, 175, 55, 0.2),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  animation: slideUp 0.5s ease;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(212, 175, 55, 0.2);
  position: relative;
  overflow: hidden;
}

.tool-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #d4af37, transparent);
  animation: cardShimmer 4s ease-in-out infinite;
}

@keyframes cardShimmer {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
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
  margin-bottom: 40px;
}

.section-header h2 {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 50%, #ffd700 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.section-header p {
  color: rgba(212, 175, 55, 0.8);
  font-size: 15px;
  font-weight: 500;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.form-group {
  display: flex;
  flex-direction: column;
  position: relative;
}

.form-group label {
  font-size: 13px;
  font-weight: 700;
  color: #ffd700;
  margin-bottom: 8px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-group label::before {
  content: '▸';
  color: #d4af37;
  font-size: 12px;
}

.form-group input,
.form-group select {
  padding: 12px 14px;
  border: 2px solid rgba(212, 175, 55, 0.3);
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, rgba(15, 20, 25, 0.9), rgba(20, 25, 30, 0.85));
  color: #ffd700;
  font-weight: 500;
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.3),
    0 1px 2px rgba(212, 175, 55, 0.1);
}

.form-group input:hover,
.form-group select:hover {
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.3),
    0 2px 8px rgba(212, 175, 55, 0.2);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #ffd700;
  background: linear-gradient(135deg, rgba(15, 20, 25, 0.95), rgba(20, 25, 30, 0.9));
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.4),
    0 0 0 3px rgba(212, 175, 55, 0.25),
    0 4px 12px rgba(212, 175, 55, 0.3);
  transform: translateY(-1px);
}

.form-group input::placeholder {
  color: rgba(212, 175, 55, 0.5);
}

.form-actions {
  text-align: center;
  margin-top: 12px;
}

.predict-btn,
.calculate-btn {
  min-width: 280px;
  max-width: 400px;
  padding: 18px 48px;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 50%, #ffd700 100%);
  background-size: 200% 100%;
  color: #0f1419;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 8px 24px rgba(212, 175, 55, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  position: relative;
  overflow: hidden;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  animation: buttonGlow 2s ease-in-out infinite;
}

@keyframes buttonGlow {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
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
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.3), rgba(212, 175, 55, 0.2));
  cursor: not-allowed;
  box-shadow: none;
  opacity: 0.5;
}

.predict-btn:hover:not(:disabled),
.calculate-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 8px 32px rgba(212, 175, 55, 0.7);
}

.predict-btn:active:not(:disabled),
.calculate-btn:active:not(:disabled) {
  transform: translateY(-1px);
}

/* 预测结果样式 */
.prediction-result {
  margin-top: 40px;
}

.result-card {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.18), rgba(255, 215, 0, 0.12));
  backdrop-filter: blur(12px);
  border: 2px solid rgba(212, 175, 55, 0.35);
  color: #ffd700;
  padding: 40px;
  border-radius: 20px;
  margin-bottom: 0;
  box-shadow:
    0 16px 48px rgba(212, 175, 55, 0.4),
    inset 0 2px 0 rgba(212, 175, 55, 0.25),
    0 0 60px rgba(212, 175, 55, 0.15);
  position: relative;
  overflow: hidden;
  animation: resultFadeIn 0.5s ease-out;
}

@keyframes resultFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.15), transparent);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

.result-header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #ffd700;
  letter-spacing: 0.5px;
}

.confidence {
  background: rgba(212, 175, 55, 0.3);
  padding: 10px 18px;
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
  font-size: 56px;
  font-weight: 900;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #ffd700 0%, #fff 50%, #ffd700 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 4px 16px rgba(212, 175, 55, 0.5);
  letter-spacing: -1px;
  animation: priceShine 3s ease-in-out infinite;
}

@keyframes priceShine {
  0%, 100% { filter: brightness(1); }
  50% { filter: brightness(1.2); }
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
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.6), rgba(22, 33, 62, 0.7));
  padding: 20px;
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.trend-chart h4 {
  margin: 0 0 16px 0;
  color: #ffd700;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.chart-container {
  height: 200px;
  margin-bottom: 16px;
}

.chart-placeholder {
  background: rgba(15, 20, 25, 0.5);
  border-radius: 12px;
  padding: 16px;
  height: 100%;
  border: 1px solid rgba(212, 175, 55, 0.2);
}

.chart-bars {
  display: flex;
  align-items: end;
  justify-content: space-around;
  height: 100%;
}

.chart-bar {
  width: 30px;
  background: linear-gradient(to top, #d4af37, #ffd700);
  border-radius: 4px 4px 0 0;
  position: relative;
  transition: height 0.3s;
  box-shadow: 0 2px 8px rgba(212, 175, 55, 0.3);
}

.bar-value {
  position: absolute;
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  white-space: nowrap;
  color: #ffd700;
  font-weight: 600;
}

.bar-label {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: rgba(212, 175, 55, 0.7);
  font-weight: 500;
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
  color: rgba(212, 175, 55, 0.8);
  font-weight: 500;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.legend-color.historical {
  background: #d4af37;
}

.legend-color.predicted {
  background: #ffd700;
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
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(255, 215, 0, 0.1));
  border: 2px solid rgba(212, 175, 55, 0.3);
  color: #ffd700;
  padding: 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.result-cards .result-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(212, 175, 55, 0.4);
  border-color: #d4af37;
}

.result-cards .card-icon {
  font-size: 32px;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 2px 4px rgba(212, 175, 55, 0.3));
}

.result-cards .card-content {
  flex: 1;
}

.result-cards .card-value {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
  text-shadow: 0 1px 2px rgba(212, 175, 55, 0.2);
}

.result-cards .card-label {
  font-size: 13px;
  color: rgba(212, 175, 55, 0.8);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.repayment-plan {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.5), rgba(22, 33, 62, 0.6));
  padding: 20px;
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.repayment-plan h4 {
  margin: 0 0 16px 0;
  color: #ffd700;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.plan-table {
  background: rgba(15, 20, 25, 0.6);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(212, 175, 55, 0.2);
}

.table-header,
.table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
  gap: 1px;
}

.table-header {
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  font-weight: 700;
}

.table-cell {
  padding: 12px 8px;
  font-size: 12px;
  text-align: center;
}

.table-row {
  background: rgba(26, 26, 46, 0.4);
  border-bottom: 1px solid rgba(212, 175, 55, 0.1);
}

.table-row:nth-child(even) {
  background: rgba(22, 33, 62, 0.4);
}

.table-row .table-cell {
  color: rgba(212, 175, 55, 0.9);
}

/* 热力图样式 */
.heatmap-controls {
  display: flex;
  gap: 24px;
  margin-bottom: 28px;
  flex-wrap: wrap;
  align-items: flex-end;
  padding: 24px;
  background: rgba(26, 26, 46, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.control-group label {
  font-size: 14px;
  font-weight: 600;
  color: #d4af37;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.control-group select,
.control-group input[type="range"] {
  padding: 12px 16px;
  border: 2px solid rgba(212, 175, 55, 0.3);
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
  background: rgba(15, 20, 25, 0.8);
  color: #ffd700;
}

.control-group select:focus {
  border-color: #d4af37;
  box-shadow: 0 0 0 3px rgba(212, 175, 55, 0.2);
  background: rgba(15, 20, 25, 0.9);
}

.control-group input[type="range"] {
  width: 160px;
}

.refresh-btn {
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  border: none;
  padding: 12px 24px;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
  height: 44px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.4);
  letter-spacing: 0.5px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.refresh-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.6);
}

/* 高德地图容器 */
.amap-wrapper {
  position: relative;
  background: rgba(26, 26, 46, 0.8);
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
  border: 2px solid rgba(212, 175, 55, 0.3);
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.2);
}

.amap-container {
  width: 100%;
  height: 550px;
  border-radius: 14px;
}

/* 地图图例 */
.amap-legend {
  position: absolute;
  bottom: 24px;
  right: 24px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95), rgba(22, 33, 62, 0.98));
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 8px;
  color: #ffd700;
  border: 1px solid rgba(212, 175, 55, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
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
  width: 60px;
  height: 60px;
  border: 5px solid rgba(212, 175, 55, 0.2);
  border-top-color: #ffd700;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 统计信息 */
.heatmap-stats {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(22, 33, 62, 0.9));
  padding: 32px;
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
}

.heatmap-stats h4 {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
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
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), rgba(255, 215, 0, 0.08));
  border-radius: 12px;
  transition: all 0.3s ease;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
  border-color: rgba(212, 175, 55, 0.4);
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
  text-shadow: 0 1px 2px rgba(212, 175, 55, 0.2);
}

.stat-label {
  font-size: 13px;
  color: rgba(212, 175, 55, 0.8);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.15), rgba(255, 77, 79, 0.1));
  border: 1px solid rgba(255, 107, 107, 0.3);
  border-radius: 12px;
  padding: 16px;
  margin-top: 20px;
  backdrop-filter: blur(10px);
}

.error-icon {
  font-size: 20px;
  color: #ff6b6b;
}

.error-text {
  color: #ff8787;
  font-size: 14px;
  font-weight: 500;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 30px;
  padding: 30px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.6), rgba(22, 33, 62, 0.7));
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(212, 175, 55, 0.2);
  border-top: 3px solid #ffd700;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  box-shadow: 0 2px 8px rgba(212, 175, 55, 0.3);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: rgba(212, 175, 55, 0.8);
  font-size: 14px;
  font-weight: 500;
}

.form-preview {
  padding: 32px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.4), rgba(22, 33, 62, 0.5));
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  color: #d4af37;
  font-size: 16px;
  margin-bottom: 0;
  position: relative;
  overflow: hidden;
}

.form-preview::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.08), transparent 70%);
  pointer-events: none;
}

.form-preview h4 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #ffd700;
  font-weight: 800;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-preview h4::before {
  content: '📋';
  font-size: 24px;
}

.form-preview ul {
  margin: 0 0 24px 0;
  padding: 0 0 0 24px;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.form-preview li {
  margin-bottom: 10px;
  color: rgba(212, 175, 55, 0.9);
  font-size: 15px;
  font-weight: 500;
  position: relative;
  padding-left: 8px;
  transition: all 0.2s ease;
}

.form-preview li::before {
  content: '▸';
  position: absolute;
  left: -16px;
  color: #d4af37;
  font-size: 12px;
}

.form-preview li:hover {
  color: #ffd700;
  transform: translateX(4px);
}
</style>
