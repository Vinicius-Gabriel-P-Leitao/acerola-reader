import type { Preview } from "@storybook/sveltekit";
import "../src/theme/layout.css";

// Mock Tauri APIs globalmente no Storybook
// Componentes que usam useTheme/LazyStore não vão quebrar
const mockIPC = (window as any).__TAURI_IPC__;
if (!mockIPC) {
  (window as any).__TAURI__ = { ipc: () => {} };
}

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /date$/i,
      },
    },
  },
};

export default preview;
