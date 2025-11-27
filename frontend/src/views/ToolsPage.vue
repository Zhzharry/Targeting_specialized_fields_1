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
            <div class="form-group">
              <label>所在城市</label>
              <select v-model="predictionForm.city">
                <option value="">请选择城市</option>
                <option value="beijing">北京</option>
                <option value="shanghai">上海</option>
                <option value="guangzhou">广州</option>
                <option value="shenzhen">深圳</option>
                <option value="hangzhou">杭州</option>
              </select>
            </div>

            <div class="form-group">
              <label>区域位置</label>
              <select v-model="predictionForm.district">
                <option value="">请选择区域</option>
                <option value="chaoyang">朝阳区</option>
                <option value="haidian">海淀区</option>
                <option value="xuhui">徐汇区</option>
                <option value="pudong">浦东新区</option>
                <option value="tianhe">天河区</option>
              </select>
            </div>

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

            <div class="form-group">
              <label>户型</label>
              <select v-model="predictionForm.layout">
                <option value="">请选择户型</option>
                <option value="1">一居室</option>
                <option value="2">两居室</option>
                <option value="3">三居室</option>
                <option value="4">四居室</option>
                <option value="loft">LOFT</option>
              </select>
            </div>

            <div class="form-group">
              <label>楼层</label>
              <select v-model="predictionForm.floor">
                <option value="">请选择楼层</option>
                <option value="low">低楼层</option>
                <option value="middle">中楼层</option>
                <option value="high">高楼层</option>
              </select>
            </div>

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
          </div>

          <div class="form-actions">
            <button @click="predictPrice" class="predict-btn" :disabled="!canPredict">
              {{ predicting ? '预测中...' : '开始预测' }}
            </button>
          </div>
        </div>

        <!-- 预测结果 -->
        <div v-if="predictionResult" class="prediction-result">
          <div class="result-card">
            <div class="result-header">
              <h3>预测结果</h3>
              <div class="confidence">置信度: {{ predictionResult.confidence }}%</div>
            </div>

            <div class="price-display">
              <div class="predicted-price">
                ¥{{ predictionResult.predictedPrice.toLocaleString() }}
              </div>
              <div class="price-unit">元/㎡</div>
            </div>

            <div class="result-details">
              <div class="detail-item">
                <span class="label">总价估算:</span>
                <span class="value"
                  >¥{{
                    (predictionResult.predictedPrice * predictionForm.area).toLocaleString()
                  }}</span
                >
              </div>
              <div class="detail-item">
                <span class="label">市场参考价:</span>
                <span class="value">¥{{ predictionResult.marketPrice.toLocaleString() }}/㎡</span>
              </div>
              <div class="detail-item">
                <span class="label">预测偏差:</span>
                <span
                  class="value"
                  :class="{
                    positive: predictionResult.deviation > 0,
                    negative: predictionResult.deviation < 0,
                  }"
                >
                  {{ predictionResult.deviation > 0 ? '+' : '' }}{{ predictionResult.deviation }}%
                </span>
              </div>
            </div>
          </div>

          <!-- 价格趋势图 -->
          <div class="trend-chart">
            <h4>价格趋势预测</h4>
            <div class="chart-container">
              <div class="chart-placeholder">
                <div class="chart-bars">
                  <div
                    v-for="(month, index) in priceTrend"
                    :key="index"
                    class="chart-bar"
                    :style="{ height: `${(month.value / 1500) * 100}%` }"
                  >
                    <div class="bar-value">¥{{ month.value }}</div>
                    <div class="bar-label">{{ month.month }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="chart-legend">
              <div class="legend-item">
                <div class="legend-color historical"></div>
                <span>历史价格</span>
              </div>
              <div class="legend-item">
                <div class="legend-color predicted"></div>
                <span>预测价格</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 贷款计算器 -->
      <div v-if="activeTool === 'mortgage'" class="tool-section">
        <div class="section-header">
          <h2>房贷计算器</h2>
          <p>计算您的月供和还款计划</p>
        </div>

        <div class="mortgage-form">
          <div class="form-grid">
            <div class="form-group">
              <label>贷款总额 (万元)</label>
              <input
                v-model.number="mortgageForm.loanAmount"
                type="number"
                placeholder="请输入贷款金额"
                min="10"
                max="1000"
              />
            </div>

            <div class="form-group">
              <label>贷款年限</label>
              <select v-model="mortgageForm.loanTerm">
                <option value="5">5年</option>
                <option value="10">10年</option>
                <option value="15">15年</option>
                <option value="20">20年</option>
                <option value="25">25年</option>
                <option value="30">30年</option>
              </select>
            </div>

            <div class="form-group">
              <label>贷款利率 (%)</label>
              <input
                v-model.number="mortgageForm.interestRate"
                type="number"
                placeholder="请输入利率"
                step="0.01"
                min="1"
                max="10"
              />
            </div>

            <div class="form-group">
              <label>还款方式</label>
              <select v-model="mortgageForm.repaymentType">
                <option value="equalPrincipal">等额本金</option>
                <option value="equalInstallment">等额本息</option>
              </select>
            </div>
          </div>

          <div class="form-actions">
            <button @click="calculateMortgage" class="calculate-btn" :disabled="!canCalculate">
              {{ calculating ? '计算中...' : '开始计算' }}
            </button>
          </div>
        </div>

        <!-- 计算结果 -->
        <div v-if="mortgageResult" class="mortgage-result">
          <div class="result-cards">
            <div class="result-card">
              <div class="card-icon">💰</div>
              <div class="card-content">
                <div class="card-value">¥{{ mortgageResult.monthlyPayment.toLocaleString() }}</div>
                <div class="card-label">月供金额</div>
              </div>
            </div>

            <div class="result-card">
              <div class="card-icon">📅</div>
              <div class="card-content">
                <div class="card-value">{{ mortgageResult.totalMonths }}</div>
                <div class="card-label">还款月数</div>
              </div>
            </div>

            <div class="result-card">
              <div class="card-icon">💵</div>
              <div class="card-content">
                <div class="card-value">¥{{ mortgageResult.totalInterest.toLocaleString() }}</div>
                <div class="card-label">总利息</div>
              </div>
            </div>

            <div class="result-card">
              <div class="card-icon">🏦</div>
              <div class="card-content">
                <div class="card-value">¥{{ mortgageResult.totalPayment.toLocaleString() }}</div>
                <div class="card-label">还款总额</div>
              </div>
            </div>
          </div>

          <!-- 还款计划表 -->
          <div class="repayment-plan">
            <h4>还款计划 (前12个月)</h4>
            <div class="plan-table">
              <div class="table-header">
                <div class="table-cell">期数</div>
                <div class="table-cell">月供</div>
                <div class="table-cell">本金</div>
                <div class="table-cell">利息</div>
                <div class="table-cell">剩余本金</div>
              </div>
              <div
                v-for="plan in mortgageResult.repaymentPlan.slice(0, 12)"
                :key="plan.period"
                class="table-row"
              >
                <div class="table-cell">第{{ plan.period }}期</div>
                <div class="table-cell">¥{{ plan.monthlyPayment.toLocaleString() }}</div>
                <div class="table-cell">¥{{ plan.principal.toLocaleString() }}</div>
                <div class="table-cell">¥{{ plan.interest.toLocaleString() }}</div>
                <div class="table-cell">¥{{ plan.remainingPrincipal.toLocaleString() }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 价格热力图 -->
      <div v-if="activeTool === 'heatmap'" class="tool-section">
        <div class="section-header">
          <h2>价格热力图</h2>
          <p>可视化展示各区域房价分布</p>
        </div>

        <div class="heatmap-controls">
          <div class="control-group">
            <label>城市选择</label>
            <select v-model="heatmapCity">
              <option value="beijing">北京</option>
              <option value="shanghai">上海</option>
              <option value="guangzhou">广州</option>
              <option value="shenzhen">深圳</option>
            </select>
          </div>

          <div class="control-group">
            <label>价格区间</label>
            <select v-model="priceRange">
              <option value="all">全部</option>
              <option value="low">3万以下/㎡</option>
              <option value="medium">3-6万/㎡</option>
              <option value="high">6-10万/㎡</option>
              <option value="luxury">10万以上/㎡</option>
            </select>
          </div>
        </div>

        <div class="heatmap-container">
          <div class="heatmap-placeholder">
            <div class="map-grid">
              <div
                v-for="district in heatmapData"
                :key="district.name"
                class="map-district"
                :class="getDistrictClass(district)"
                @click="selectDistrict(district)"
              >
                <div class="district-name">{{ district.name }}</div>
                <div class="district-price">¥{{ district.price }}/㎡</div>
              </div>
            </div>
          </div>

          <div class="heatmap-legend">
            <div class="legend-title">价格区间</div>
            <div class="legend-gradation">
              <div class="gradation-item low">3万以下</div>
              <div class="gradation-item medium-low">3-4万</div>
              <div class="gradation-item medium">4-6万</div>
              <div class="gradation-item medium-high">6-8万</div>
              <div class="gradation-item high">8-10万</div>
              <div class="gradation-item luxury">10万以上</div>
            </div>
          </div>
        </div>

        <!-- 选中区域详情 -->
        <div v-if="selectedDistrict" class="district-detail">
          <h4>{{ selectedDistrict.name }}区房价分析</h4>
          <div class="detail-stats">
            <div class="stat">
              <div class="stat-value">¥{{ selectedDistrict.price }}/㎡</div>
              <div class="stat-label">平均单价</div>
            </div>
            <div class="stat">
              <div class="stat-value">
                {{ selectedDistrict.trend > 0 ? '+' : '' }}{{ selectedDistrict.trend }}%
              </div>
              <div class="stat-label">月环比</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ selectedDistrict.transactions }}</div>
              <div class="stat-label">月成交量</div>
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

// 当前激活的工具
const activeTool = ref('prediction')

// 房价预测相关
const predictionForm = reactive({
  city: '',
  district: '',
  area: 0,
  layout: '',
  floor: '',
  age: 0,
})

const predicting = ref(false)

// 定义预测结果接口
interface PredictionResult {
  predictedPrice: number
  marketPrice: number
  confidence: number
  deviation: number
}

const predictionResult = ref<PredictionResult | null>(null)

// 贷款计算相关
const mortgageForm = reactive({
  loanAmount: 0,
  loanTerm: 20,
  interestRate: 4.5,
  repaymentType: 'equalInstallment',
})

const calculating = ref(false)

// 定义还款计划项接口
interface RepaymentPlanItem {
  period: number
  monthlyPayment: number
  principal: number
  interest: number
  remainingPrincipal: number
}

// 定义贷款计算结果接口
interface MortgageResult {
  monthlyPayment: number
  totalMonths: number
  totalInterest: number
  totalPayment: number
  repaymentPlan: RepaymentPlanItem[]
}

const mortgageResult = ref<MortgageResult | null>(null)

// 热力图相关
const heatmapCity = ref('beijing')
const priceRange = ref('all')

// 定义区域数据接口
interface DistrictData {
  name: string
  price: number
  trend: number
  transactions: number
}

const selectedDistrict = ref<DistrictData | null>(null)

// 计算属性
const canPredict = computed(() => {
  return predictionForm.city && predictionForm.district && predictionForm.area > 0
})

const canCalculate = computed(() => {
  return mortgageForm.loanAmount > 0 && mortgageForm.interestRate > 0
})

// 模拟价格趋势数据
const priceTrend = ref([
  { month: '1月', value: 45000 },
  { month: '2月', value: 45500 },
  { month: '3月', value: 46000 },
  { month: '4月', value: 46500 },
  { month: '5月', value: 47000 },
  { month: '6月', value: 47500 },
  { month: '7月', value: 48000 },
  { month: '8月', value: 48500 },
  { month: '9月', value: 49000 },
  { month: '10月', value: 49500 },
  { month: '11月', value: 50000 },
  { month: '12月', value: 50500 },
])

// 模拟热力图数据
const heatmapData = ref<DistrictData[]>([
  { name: '朝阳', price: 68000, trend: 1.2, transactions: 245 },
  { name: '海淀', price: 85000, trend: 2.1, transactions: 189 },
  { name: '西城', price: 120000, trend: 0.8, transactions: 76 },
  { name: '东城', price: 95000, trend: 1.5, transactions: 92 },
  { name: '丰台', price: 52000, trend: 1.8, transactions: 156 },
  { name: '石景山', price: 48000, trend: 1.1, transactions: 98 },
  { name: '通州', price: 42000, trend: 2.3, transactions: 203 },
  { name: '大兴', price: 38000, trend: 1.9, transactions: 187 },
])

// 方法
const predictPrice = async () => {
  predicting.value = true

  // 模拟API调用
  setTimeout(() => {
    const basePrice = 50000
    const areaFactor = predictionForm.area / 100
    const ageFactor = Math.max(0, 1 - (predictionForm.age || 0) * 0.01)
    const predictedPrice = basePrice * areaFactor * ageFactor * (1 + Math.random() * 0.2)

    predictionResult.value = {
      predictedPrice: Math.round(predictedPrice),
      marketPrice: Math.round(basePrice * areaFactor),
      confidence: Math.round(85 + Math.random() * 10),
      deviation: Math.round((Math.random() - 0.5) * 10),
    }

    predicting.value = false
  }, 1500)
}

const calculateMortgage = async () => {
  calculating.value = true

  // 模拟计算
  setTimeout(() => {
    const loanAmount = mortgageForm.loanAmount * 10000
    const monthlyRate = mortgageForm.interestRate / 100 / 12
    const totalMonths = mortgageForm.loanTerm * 12

    let monthlyPayment = 0
    let totalInterest = 0
    let totalPayment = 0
    const repaymentPlan: RepaymentPlanItem[] = []

    if (mortgageForm.repaymentType === 'equalInstallment') {
      // 等额本息计算
      monthlyPayment =
        (loanAmount * monthlyRate * Math.pow(1 + monthlyRate, totalMonths)) /
        (Math.pow(1 + monthlyRate, totalMonths) - 1)
      totalPayment = monthlyPayment * totalMonths
      totalInterest = totalPayment - loanAmount

      // 生成还款计划
      let remainingPrincipal = loanAmount
      for (let i = 1; i <= totalMonths; i++) {
        const interest = remainingPrincipal * monthlyRate
        const principal = monthlyPayment - interest
        remainingPrincipal -= principal

        repaymentPlan.push({
          period: i,
          monthlyPayment: Math.round(monthlyPayment),
          principal: Math.round(principal),
          interest: Math.round(interest),
          remainingPrincipal: Math.round(Math.max(0, remainingPrincipal)),
        })
      }
    } else {
      // 等额本金计算
      const principalPerMonth = loanAmount / totalMonths
      totalInterest = 0

      for (let i = 1; i <= totalMonths; i++) {
        const remainingPrincipal = loanAmount - (i - 1) * principalPerMonth
        const interest = remainingPrincipal * monthlyRate
        monthlyPayment = principalPerMonth + interest
        totalInterest += interest

        repaymentPlan.push({
          period: i,
          monthlyPayment: Math.round(monthlyPayment),
          principal: Math.round(principalPerMonth),
          interest: Math.round(interest),
          remainingPrincipal: Math.round(Math.max(0, remainingPrincipal - principalPerMonth)),
        })
      }

      totalPayment = loanAmount + totalInterest
    }

    mortgageResult.value = {
      monthlyPayment: Math.round(monthlyPayment),
      totalMonths,
      totalInterest: Math.round(totalInterest),
      totalPayment: Math.round(totalPayment),
      repaymentPlan,
    }

    calculating.value = false
  }, 1000)
}

const getDistrictClass = (district: DistrictData) => {
  if (district.price < 30000) return 'low'
  if (district.price < 40000) return 'medium-low'
  if (district.price < 60000) return 'medium'
  if (district.price < 80000) return 'medium-high'
  if (district.price < 100000) return 'high'
  return 'luxury'
}

const selectDistrict = (district: DistrictData) => {
  selectedDistrict.value = district
}
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
</style>
