export type ArchiveTemplateSortType = 'Chapter' | 'Volume';

export type ArchiveTemplate = {
	id: number;
	label: string;
	pattern: string;
	sort_type: ArchiveTemplateSortType;
	is_default: boolean;
	priority: number;
};
