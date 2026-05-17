export enum VolumeViewType {
  VOLUME = "VOLUME",
  CHAPTER = "CHAPTER",
  COVER_VOLUME = "COVER_VOLUME",
}

export type ChapterFileDto = {
  id: string;
  name: string;
  path: string;
  chapterSort: string;
  volumeId: string | null;
  volumeName: string | null;
  isSpecial: boolean;
  lastModified: number;
};

export type VolumeArchiveDto = {
  id: string;
  name: string;
  volumeSort: string;
  isSpecial: boolean;
  coverUri: string | null;
  bannerUri: string | null;
  lastModified: number;
  chapterCount: number;
};

export type VolumeChapterGroupDto = {
  volume: VolumeArchiveDto;
  items: ChapterFileDto[];
  totalChapters: number;
  loadedCount: number;
  hasMore: boolean;
  currentPage: number;
  totalPages: number;
};

export type ChapterPageDto = {
  items: ChapterFileDto[];
  volumes: VolumeArchiveDto[];
  pageSize: number;
  page: number;
  total: number;
  volumeSections: VolumeChapterGroupDto[];
};

export type ChapterDto = {
  archive: ChapterPageDto;
  showVolumeHeaders: boolean;
  hasVolumeStructure: boolean;
  effectiveViewMode: VolumeViewType;
};
