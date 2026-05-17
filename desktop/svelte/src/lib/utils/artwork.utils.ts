import { convertFileSrc } from "@tauri-apps/api/core";

/**
 * Resolves a file path to a URL suitable for display in the UI.
 * Handles Windows backslashes and converts them to forward slashes.
 */
export function resolveArtworkPath(path: string | null | undefined): string | null {
  if (!path) return null;
  return convertFileSrc(path.replaceAll("\\", "/"));
}

export interface ArtworkData {
  cover?: string | null;
  banner?: string | null;
}

/**
 * Resolves the cover image for a comic.
 * Fallback priority: cover -> banner -> null
 */
export function resolveCover(artwork: ArtworkData): string | null {
  return resolveArtworkPath(artwork.cover ?? artwork.banner);
}

/**
 * Resolves the banner image for a comic.
 * Fallback priority: banner -> cover -> null
 */
export function resolveBanner(artwork: ArtworkData): string | null {
  return resolveArtworkPath(artwork.banner ?? artwork.cover);
}
