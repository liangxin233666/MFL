CREATE TABLE plugins (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL, -- 插件名，如 "Dark Mode Plus"
    slug VARCHAR(255) UNIQUE NOT NULL, -- 唯一标识，如 "dark-mode-plus"
    description TEXT,
    version VARCHAR(50) NOT NULL, -- 版本号，如 "1.0.0"

    file_url TEXT NOT NULL, -- [关键] 插件文件的永久MinIO地址 (.js/.zip)
    icon_url TEXT,          -- 插件图标的地址

    author_name VARCHAR(100),
    type VARCHAR(50), -- 类型：'THEME', 'EXTENSION'

    downloads INT DEFAULT 0, -- 下载量统计
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);