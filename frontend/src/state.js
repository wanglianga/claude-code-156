import { reactive } from 'vue'

export const ui = reactive({ tab: 'dashboard', batchId: null })

export function goto(tab, batchId) {
  ui.tab = tab
  if (batchId != null) ui.batchId = batchId
}
