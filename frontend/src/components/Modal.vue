<template>
  <div class="modal-mask" @click.self="$emit('close')">
    <div class="modal" :style="{ width: width }">
      <h3>
        <span>{{ title }}</span>
        <a class="muted" @click="$emit('close')">✕</a>
      </h3>
      <slot />
      <div v-if="$slots.footer" style="margin-top:14px; text-align:right; display:flex; gap:10px; justify-content:flex-end">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ title: String, width: { type: String, default: '640px' } })
defineEmits(['close'])
</script>
