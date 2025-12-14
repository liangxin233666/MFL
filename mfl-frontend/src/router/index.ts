import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import HomePage from '../views/HomePage.vue';
import LoginPage from '../views/LoginPage.vue';
import RegisterPage from '../views/RegisterPage.vue';
import SettingsPage from '../views/SettingsPage.vue';
import ProfilePage from '../views/ProfilePage.vue';         // 必须导入
import ArticleDetailPage from '../views/ArticleDetailPage.vue';
import EditorPage from '../views/EditorPage.vue';
import FeedPage from "../views/FeedPage.vue";           // 必须导入
import ContentManagePage from '../views/ContentManagePage.vue';
import {useAuthStore} from "../stores/auth.ts";
import NotificationsPage from '../views/NotificationsPage.vue';
import HistoryPage from "../views/HistoryPage.vue";
import SearchPage from "../views/SearchPage.vue";
import PluginMarketPage from "../views/PluginMarketPage.vue";

const routes: Array<RouteRecordRaw> = [
    { path: '/', name: 'Home', component: HomePage },
    { path: '/login', name: 'Login', component: LoginPage },
    { path: '/register', name: 'Register', component: RegisterPage },
    { path: '/settings', name: 'Settings', component: SettingsPage ,

    },
    {
        path: '/market',
        name: 'PluginMarket',
        component: PluginMarketPage
    },
    {
        path: '/history',
        name: 'History',
        component: HistoryPage,

    },
    {
        path: '/search',
        name: 'Search',
        component: SearchPage,
    },
    {
        path: '/notifications',
        name: 'Notifications',
        component: NotificationsPage,

    },
    {
        path: '/profile/:username',
        name: 'Profile',
        component: ProfilePage
    },
    { path: '/article/:slug', name: 'ArticleDetail', component: ArticleDetailPage },

    {
        path: '/editor',
        name: 'ArticleCreate',
        component: EditorPage,

    },
    {
        path: '/editor/:slug',
        name: 'ArticleEdit',
        component: EditorPage
    },
    {
        path: '/feed',
        name: 'Feed',
        component: FeedPage,
    },
    {
        path: '/creator/content',
        name: 'ContentManage',
        component: ContentManagePage,

    },

];

const router = createRouter({
    history: createWebHistory(),
    routes,
});


export default router;