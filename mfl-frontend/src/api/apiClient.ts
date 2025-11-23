// src/api/apiClient.ts
import axios from 'axios';

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 请求拦截器：在每个请求发送前，检查并添加 Token
apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem('jwt_token');

    if (token) {
        config.headers.Authorization = `Token ${token}`;
    }
    return config;
});

export default apiClient;