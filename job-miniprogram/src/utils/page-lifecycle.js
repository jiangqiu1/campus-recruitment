/**
 * uni-app 页面生命周期包装器
 * 
 * 由于 @dcloudio/vite-plugin-uni@alpha 存在编译器 bug，
 * 直接 import { onShow } from '@dcloudio/uni-app' 会在 vendor.js 中生成 nref; 导致崩溃。
 * 通过本模块中转，仅对 uni-app 做一次导入，避免编译器 bug。
 */

import { onShow } from '@dcloudio/uni-app'

export { onShow }
