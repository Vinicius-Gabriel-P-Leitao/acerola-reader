import type { Meta, StoryObj } from '@storybook/svelte';
import AcerolaBookmarkRibbon from './acerola-bookmark-ribbon.svelte';

const meta = {
	title: 'Components/AcerolaBookmarkRibbon',
	component: AcerolaBookmarkRibbon,
	tags: ['autodocs'],
	argTypes: {
		color: { control: 'number' },
		class: { control: 'text' }
	}
} satisfies Meta<typeof AcerolaBookmarkRibbon>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
	args: {
		color: 0xFFF44336 // Red color
	}
};

export const BlueRibbon: Story = {
	args: {
		color: 0xFF2196F3 // Blue color
	}
};

export const GreenRibbon: Story = {
	args: {
		color: 0xFF4CAF50 // Green color
	}
};
