import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";
import patchVendor from "./scripts/vite-plugin-patch-vendor.mjs";

export default defineConfig({
  plugins: [uni(), patchVendor()],
});
