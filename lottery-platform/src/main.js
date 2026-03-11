import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import Vant from 'vant'
import 'vant/lib/index.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/responsive.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) app.component(key, component)
app.use(createPinia()).use(router).use(Vant).use(ElementPlus, { locale: zhCn }).mount('#app')
