/**
 * vite-plugin-patch-vendor — Vite plugin
 * In watch mode (dev), fires on each rebuild via closeBundle hook.
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default function patchVendorPlugin() {
  return {
    name: 'patch-vendor',
    closeBundle() {
      const vendorPath = path.resolve(
        __dirname, '..', 'dist', 'dev', 'mp-weixin', 'common', 'vendor.js'
      )
      if (!fs.existsSync(vendorPath)) return

      let content = fs.readFileSync(vendorPath, 'utf8')
      if (content.includes('exports.reactive = reactive;')) return // already patched

      const patched = content.replace(
        'exports.ref = ref;',
        'exports.ref = ref;\nexports.reactive = reactive;'
      )
      if (patched === content) {
        console.warn('[patch-vendor] ⚠️  patch anchor "exports.ref = ref;" not found')
        return
      }
      fs.writeFileSync(vendorPath, patched, 'utf8')
      console.log('[patch-vendor] ✅ reactive patch applied')
    }
  }
}
