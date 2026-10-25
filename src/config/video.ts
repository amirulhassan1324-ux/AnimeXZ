export interface EpisodeSource {
  url: string;
  type: 'mp4' | 'm3u8';
  quality: string;
}

const BACKEND_STREAM_BASE = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample';
const HLS_STREAM_URL = 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8';

const SAMPLE_SOURCES = [
  `${BACKEND_STREAM_BASE}/BigBuckBunny.mp4`,
  `${BACKEND_STREAM_BASE}/ElephantsDream.mp4`,
  `${BACKEND_STREAM_BASE}/TearsOfSteel.mp4`,
  `${BACKEND_STREAM_BASE}/Sintel.mp4`,
  `${BACKEND_STREAM_BASE}/SubaruOutbackSeeTheWorld.mp4`
];

/**
 * Returns an .mp4 or .m3u8 streaming URL for a specific anime and episode.
 * Connect your licensed video backend URL here.
 *
 * @param animeId - AniList anime ID
 * @param episode - Episode number (1-based)
 */
export function getEpisodeSource(animeId: number, episode: number): EpisodeSource {
  const isHls = (animeId + episode) % 5 === 0;

  if (isHls) {
    return {
      url: HLS_STREAM_URL,
      type: 'm3u8',
      quality: 'Auto (HLS adaptive)',
    };
  }

  const index = Math.abs(animeId * 31 + episode) % SAMPLE_SOURCES.length;
  return {
    url: SAMPLE_SOURCES[index],
    type: 'mp4',
    quality: '1080p',
  };
}
