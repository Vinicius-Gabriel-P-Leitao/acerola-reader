/// <reference types="vite/client" />
import type { Preview } from '@storybook/sveltekit';
import './storybook.css';

document.documentElement.setAttribute('data-theme', 'catppuccin-mocha');
document.documentElement.classList.add('dark');

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
