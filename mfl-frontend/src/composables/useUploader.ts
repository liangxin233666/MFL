// src/composables/useUploader.ts
import { ref } from 'vue';
import apiClient from '../api/apiClient';
import axios from 'axios';

export function useUploader() {
    const isUploading = ref(false);
    const uploadError = ref<string | null>(null);

    /**
     * 遵循"预签名URL"方案的核心上传函数
     * @param file 用户选择的 File 对象
     * @returns 成功时返回可用于预览和提交的 tempUrl，失败时返回 null
     */
    const uploadFile = async (file: File): Promise<string | null> => {
        isUploading.value = true;
        uploadError.value = null;

        try {
            // Step 1: 向我们的后端请求“上传许可证”
            const presignResponse = await apiClient.post<{ uploadUrl: string }>('/uploads/presigned-url', {
                fileName: file.name,
                contentType: file.type,
            });

            const { uploadUrl } = presignResponse.data;

            // Step 2: 使用“许可证”直接上传文件到云存储
            await axios.put(uploadUrl, file, {
                headers: {
                    'Content-Type': file.type,
                },
            });

            // Step 3: 获取并返回 tempUrl (去掉查询参数的部分)
            const tempUrl = uploadUrl.split('?')[0];
            return tempUrl;

        } catch (error: any) {
            console.error("上传失败:", error);
            uploadError.value = error.response?.data?.message || '上传过程中发生错误';
            return null;
        } finally {
            isUploading.value = false;
        }
    };

    return {
        isUploading,
        uploadError,
        uploadFile,
    };
}