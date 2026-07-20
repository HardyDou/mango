const MANAGED_SECTION_HEADING = 'Risk / Verification';
const DEFAULT_INSERT_BEFORE_HEADING = 'Validation';

export function inspectProjectPullRequestTemplate(
  currentContent,
  canonicalContent,
  { targetExists = true } = {},
) {
  const canonical = requireSingleSection(canonicalContent, MANAGED_SECTION_HEADING, 'canonical PMO template');
  if (!targetExists) {
    return { errors: ['project PR template is missing'] };
  }
  const currentSections = findSections(currentContent, MANAGED_SECTION_HEADING);
  if (currentSections.length !== 1) {
    return {
      errors: [
        `project PR template must contain exactly one ## ${MANAGED_SECTION_HEADING} section; found ${currentSections.length}`,
      ],
    };
  }
  const current = sectionContent(currentContent, currentSections[0]);
  if (normalizeLineEndings(current) !== normalizeLineEndings(canonical)) {
    return { errors: [`project PR template ## ${MANAGED_SECTION_HEADING} section differs from the locked PMO contract`] };
  }
  return { errors: [] };
}

export function synchronizeProjectPullRequestTemplate(
  currentContent,
  canonicalContent,
  { targetExists = true } = {},
) {
  let canonical;
  try {
    canonical = requireSingleSection(canonicalContent, MANAGED_SECTION_HEADING, 'canonical PMO template');
  } catch (error) {
    return { action: 'error', reason: error.message };
  }
  if (!targetExists) {
    return { action: 'add', content: ensureFinalNewline(canonicalContent) };
  }
  const lineEnding = preferredLineEnding(currentContent);
  const canonicalForTarget = normalizeLineEndings(canonical, lineEnding);
  const currentSections = findSections(currentContent, MANAGED_SECTION_HEADING);
  if (currentSections.length > 1) {
    return {
      action: 'error',
      reason: `project PR template must contain exactly one ## ${MANAGED_SECTION_HEADING} section before synchronization; found ${currentSections.length}`,
    };
  }
  if (currentSections.length === 0) {
    return {
      action: 'update',
      content: insertSection(currentContent, canonicalForTarget, DEFAULT_INSERT_BEFORE_HEADING, lineEnding),
    };
  }
  const currentSection = sectionContent(currentContent, currentSections[0]);
  if (normalizeLineEndings(currentSection) === normalizeLineEndings(canonical)) {
    return { action: 'skip', content: currentContent };
  }
  const section = currentSections[0];
  const suffix = currentContent.slice(section.end);
  const separator = suffix.length > 0 && !/^\r?\n/u.test(suffix) ? lineEnding.repeat(2) : '';
  return {
    action: 'update',
    content: `${currentContent.slice(0, section.start)}${canonicalForTarget}${separator}${suffix}`,
  };
}

function requireSingleSection(markdown, heading, sourceLabel) {
  const sections = findSections(markdown, heading);
  if (sections.length !== 1) {
    throw new Error(`${sourceLabel} must contain exactly one ## ${heading} section; found ${sections.length}`);
  }
  return sectionContent(markdown, sections[0]);
}

function findSections(markdown, heading) {
  const escaped = escapeRegExp(heading);
  const headingPattern = new RegExp(`^##[ \\t]+${escaped}[ \\t]*\\r?$`, 'gm');
  const matches = [...markdown.matchAll(headingPattern)];
  return matches.map(match => {
    const restStart = match.index + match[0].length;
    const nextHeading = markdown.slice(restStart).search(/^##[ \t]+/m);
    return {
      start: match.index,
      end: nextHeading >= 0 ? restStart + nextHeading : markdown.length,
    };
  });
}

function sectionContent(markdown, section) {
  return markdown.slice(section.start, section.end).trimEnd();
}

function insertSection(markdown, section, beforeHeading, lineEnding) {
  const escaped = escapeRegExp(beforeHeading);
  const before = new RegExp(`^##[ \\t]+${escaped}[ \\t]*\\r?$`, 'm').exec(markdown);
  if (!before) {
    return `${markdown}${appendSeparator(markdown, lineEnding)}${section}${lineEnding}`;
  }
  const prefix = markdown.slice(0, before.index);
  return `${prefix}${appendSeparator(prefix, lineEnding)}${section}${lineEnding.repeat(2)}${markdown.slice(before.index)}`;
}

function appendSeparator(value, lineEnding) {
  if (value.length === 0) return '';
  if (/\r?\n\r?\n$/u.test(value)) return '';
  if (/\r?\n$/u.test(value)) return lineEnding;
  return lineEnding.repeat(2);
}

function ensureFinalNewline(value) {
  return /\r?\n$/u.test(value) ? value : `${value}\n`;
}

function preferredLineEnding(value) {
  return value.includes('\r\n') ? '\r\n' : '\n';
}

function normalizeLineEndings(value, lineEnding = '\n') {
  return value.replace(/\r\n|\r|\n/gu, lineEnding);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
