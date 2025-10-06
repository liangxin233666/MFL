// src/stores/auth.ts
import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAuthStore = defineStore('auth', () => {
    const isAuthenticated = ref(false);
    const user = ref(null);
    function login(userData: any) {
        isAuthenticated.value = true;
        user.value = userData;

    }

    function logout() {
        isAuthenticated.value = false;
        user.value = null;

    }

    return { isAuthenticated, user, login, logout };
});