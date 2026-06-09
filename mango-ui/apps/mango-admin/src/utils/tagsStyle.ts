export const TAGS_STYLE_ALIASES: Record<string, string> = {
  'tags-style-one': 'tags-style-capsule',
  'tags-style-four': 'tags-style-card',
  'tags-style-five': 'tags-style-classic',
};

export const DEFAULT_TAGS_STYLE = 'tags-style-classic';

export function normalizeTagsStyle(style?: string) {
  if (!style) {
    return DEFAULT_TAGS_STYLE;
  }
  return TAGS_STYLE_ALIASES[style] || style;
}
