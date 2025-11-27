/**
 * src/config/assets.ts
 * 全局静态资源配置文件
 * 所有硬编码的图片 URL 都应该统一提取到这里
 */

/**
 * 【核心函数】生成安全的 OSS URL
 * 功能：自动拼接域名，并对中文文件名进行 URL 编码
 *
 * 输入: "assets/屏幕截图 2025.png"
 * 输出: "http://localhost:9000/.../assets/%E5%B1%8F%E5%B9%95...2025.png"
 */
export const getOssUrl = (path: string | null | undefined): string => {
    // 1. 如果路径为空，返回默认图
    if (!path) return ASSETS.defaults.avatarD;

    // 2. 如果已经是完整链接 (http开头)，直接返回
    if (path.startsWith('https://')||path.startsWith('http://')) {
        return path;
    }

    // 3. 处理路径中的中文和空格
    // 逻辑：将路径按 '/' 切割，对每一段进行 encodeURIComponent 编码，再用 '/' 连起来
    // 例如：assets/我的 图片.png -> assets/%E6%88%91%E7%9A%84%20%E5%9B%BE%E7%89%87.png
    const safePath = path
        .split('/')
        .map(part => encodeURIComponent(part))
        .join('/');

    // 4. 拼接域名并返回
    // 确保没有双重斜杠 (//)
    const baseUrl = OSS_DOMAIN.replace(/\/$/, ''); // 去掉域名末尾的 /
    const relativePath = safePath.replace(/^\//, ''); // 去掉路径开头的 /

    return `${baseUrl}/${relativePath}`;
};


// 你的云存储域名 (OSS / S3 / CDN)
const OSS_DOMAIN = 'http://localhost:9000/realworld-media';

export const ASSETS = {
    // === 1. 全局通用 ===
    common: {
        // 网站 Logo
        logo: `${OSS_DOMAIN}/assets/logo.png`,
        // 默认的 404 图片
        notFound: `${OSS_DOMAIN}/assets/404.png`,
        // 空状态图片 (比如没有评论、没有稿件时)
        emptyState: `${OSS_DOMAIN}/assets/empty-state.png`,
    },

    // === 2. 默认占位图 (当用户没有上传时显示) ===
    defaults: {
        // 默认用户头像 (游客或未设置头像的用户)
        avatarD: getOssUrl(`${OSS_DOMAIN}/assets/老婆.jpg`),
        // 默认文章封面 (当文章没有封面时)
        articleCoverD: `${OSS_DOMAIN}/assets/logo.png`,
        // 默认个人空间背景图
        profileBannerD: getOssUrl(`${OSS_DOMAIN}/assets/logo.png`),
    },

    // === 3. 页面专属背景 ===
    pages: {
        // 首页顶部的大 Banner 图
        homeBanner: `${OSS_DOMAIN}/assets/列车组.jpg`,
        // 登录/注册页面的背景图
        authBackground: `${OSS_DOMAIN}/assets/logo.png`,
    },

    // === 4. 表情包或图标 (可选) ===
    icons: {
        wink: `${OSS_DOMAIN}/assets/logo.png`,
    }
};

/**
 * 一个辅助函数：用于处理完整的 URL 还是相对路径
 * 如果传入的 url 已经是 http 开头，直接返回；否则拼接 OSS 域名
 */
export const getAssetUrl = (path: string | null | undefined, defaultType: keyof typeof ASSETS.defaults = 'articleCoverD') => {
    if (!path) return ASSETS.defaults[defaultType];
    if (path.startsWith('http')) return path;
    return `${OSS_DOMAIN}/${path}`;
};

