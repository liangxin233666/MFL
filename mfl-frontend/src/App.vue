<script setup lang="ts">
// 这个 HelloWorld 组件我们暂时不用修改，所以保留导入
import HelloWorld from './components/HelloWorld.vue'
</script>

<template>
  <!--
    我们创建一个主容器，并使用 Tailwind 的 Flexbox 和颜色功能类
    - min-h-screen: 最小高度为整个屏幕高度
    - bg-slate-800: 背景颜色为深灰色
    - text-white: 默认文字颜色为白色
    - flex flex-col: 设置为 flex 容器，子元素垂直排列
    - justify-center: 垂直居中
    - items-center: 水平居中
    - p-8: 添加内边距
  -->
  <main class="min-h-screen bg-slate-800 text-white flex flex-col justify-center items-center p-8">

    <!-- 这是我们的主要测试标题 -->
    <!--
      - text-5xl: 非常大的字体
      - font-extrabold: 字体加粗
      - text-cyan-400: 青色文字
      - animate-pulse: 一个跳动的动画效果
    -->
    <h1 class="text-5xl font-extrabold text-cyan-400 animate-pulse">
      Tailwind CSS 配置成功!
    </h1>
    <p class="mt-4 text-lg text-slate-300">
      如果您能看到这个跳动的青色大标题，说明一切正常！
    </p>

    <!-- 这是 Vite 和 Vue 的 Logo -->
    <div class="my-10 flex gap-10">
      <a href="https://vite.dev" target="_blank">
        <img src="/vite.svg" class="logo" alt="Vite logo" />
      </a>
      <a href="https://vuejs.org/" target="_blank">
        <img src="./assets/vue.svg" class="logo vue" alt="Vue logo" />
      </a>
    </div>

    <!-- 这是原来的 HelloWorld 组件，我们也给它加点样式 -->
    <!--
      - bg-slate-700: 组件背景色
      - rounded-xl: 圆角
      - shadow-lg: 添加阴影
    -->
    <div class="p-8 bg-slate-700 rounded-xl shadow-lg">
      <HelloWorld msg="Vite + Vue" />
    </div>

  </main>
</template>

<!--
  这部分是原有的 Scoped CSS，我们保留它
  它专门用于设置 Vite 和 Vue 的 Logo 动画效果
  这说明了 Tailwind 和传统 CSS 可以很好地共存
-->
<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>