import { createApp } from 'vue';
import './assets/styles.css';
import App from './App.vue';
import { router } from './router';
import { pinia } from './stores';

createApp(App).use(pinia).use(router).mount('#app');
