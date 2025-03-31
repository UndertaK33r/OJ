<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">Footer</a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basicLayout {
}

#basicLayout .header {
  box-shadow: #eee 1px 1px;
}

#basicLayout .content {
  background: linear-gradient(to top, #aaa, #fff);
  padding: 20px;
}

#basicLayout .footer {
  background: #aaa;
  padding: 16px;
  position: sticky;
  bottom: 0;
  left: 0;
  width: 100%;
  text-align: center;
}
</style>
<script setup lang="ts">
import GlobalHeader from "@/components/GlobalHeader.vue";
</script>
