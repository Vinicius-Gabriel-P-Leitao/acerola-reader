/// <reference types="vite/client" />
import type { Preview } from "@storybook/sveltekit";
import "./storybook.css";

const DARK_THEMES = ["catppuccin-mocha", "nord-dark", "dracula"];

export const globalTypes = {
  theme: {
    name: "Theme",
    defaultValue: "catppuccin-mocha",
    toolbar: {
      icon: "paintbrush",
      items: [
        { value: "catppuccin-mocha", title: "Catppuccin Mocha (Dark)" },
        { value: "catppuccin-latte", title: "Catppuccin Latte (Light)" },
        { value: "nord-dark", title: "Nord Dark" },
        { value: "nord-light", title: "Nord Light" },
        { value: "dracula", title: "Dracula (Dark)" },
        { value: "alucard", title: "Alucard (Light)" },
      ],
      dynamicTitle: true,
    },
  },
};

const preview: Preview = {
  decorators: [
    (Story, context) => {
      const theme = (context.globals.theme as string) ?? "catppuccin-mocha";
      const isDark = DARK_THEMES.includes(theme);

      document.documentElement.setAttribute("data-theme", theme);
      document.documentElement.classList.toggle("dark", isDark);

      return Story();
    },
  ],
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    a11y: {
      test: "todo",
    },
  },
};

export default preview;
