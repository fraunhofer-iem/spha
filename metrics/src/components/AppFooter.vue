<script setup lang="ts">
import { footerLinks } from "../lib/config";
</script>

<template>
  <footer class="site-footer">
    <div class="footer__inner">
      <div class="footer__links">
        <a
          v-for="link in footerLinks"
          :key="link.id"
          class="footer-link"
          :href="link.url"
          target="_blank"
          rel="noopener noreferrer"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" :d="link.iconPath" />
          </svg>
          {{ link.label }}
        </a>
      </div>
    </div>
  </footer>
</template>
