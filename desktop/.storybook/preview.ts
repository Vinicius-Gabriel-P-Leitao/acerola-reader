/// <reference types="vite/client" />
import type { Preview } from '@storybook/sveltekit';
import '$theme/layout.css';
import '$theme/colors/catppuccin.css';
import '$theme/colors/nord.css';
import '$theme/colors/dracula.css';

document.documentElement.setAttribute('data-theme', 'catppuccin-mocha');

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    a11y: {
      test: 'todo',
    },
  },
};

export default preview;
