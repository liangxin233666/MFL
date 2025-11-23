// 使用 ES Module 的 import 语法导入 daisyui 插件
import daisyui from "daisyui";

/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",

    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'sans-serif'],
            },
        },
    },


    plugins: [
        require('@tailwindcss/typography'),

        daisyui,
    ],

    daisyui: {
        themes: [
            {
                bili: {
                    "primary": "#fb7299",
                    "secondary": "#23ADE5",
                    "accent": "#f7a33a",
                    "base-100": "#ffffff",
                    "base-200": "#f1f2f3",
                    "base-300": "#e5e6e7",
                    "neutral": "#3d4451",
                    "info": "#23ADE5",
                    "success": "#36d399",
                    "warning": "#fbbd23",
                    "error": "#f87272",
                },
            },
        ],
    },
}