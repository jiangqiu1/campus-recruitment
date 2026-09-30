// ECharts 按需引入：只注册项目实际用到的图表与组件，显著减小打包体积
// 用法与全量引入一致：import echarts from '@/utils/echarts'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, BarChart, PieChart, RadarChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

export default echarts
