export type ComicSummaryRelations = {
  directoryId: number;
  metadataId: number | null;
};

export type ComicSummaryFilesystem = {
  folderName: string;
};

export type ComicSummaryMetadata = {
  title: string | null;
  externalSync: boolean;
  activeSource: string | null;
};

export type ComicSummaryArtwork = {
  cover: string | null;
  banner: string | null;
};

export type ComicSummaryItem = {
  relations: ComicSummaryRelations;
  filesystem: ComicSummaryFilesystem;
  metadata: ComicSummaryMetadata;
  artwork: ComicSummaryArtwork;
};

export type ComicSummaryPayload = {
  comics: ComicSummaryItem[];
  total: number;
  fetchedAt: string;
};
