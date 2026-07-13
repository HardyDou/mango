function trimOuterPipe(line) {
  const trimmed = line.trim();
  if (!trimmed.startsWith('|')) return null;
  let start = 1;
  let end = trimmed.length;
  if (trimmed.endsWith('|') && !trimmed.endsWith('\\|')) end -= 1;
  return trimmed.slice(start, end);
}

export function splitTableRow(line) {
  const body = trimOuterPipe(line);
  if (body === null) return null;

  const cells = [];
  let cell = '';
  let escaped = false;
  let inCode = false;

  for (const char of body) {
    if (escaped) {
      cell += char;
      escaped = false;
      continue;
    }
    if (char === '\\') {
      cell += char;
      escaped = true;
      continue;
    }
    if (char === '`') {
      inCode = !inCode;
      cell += char;
      continue;
    }
    if (char === '|' && !inCode) {
      cells.push(cell.trim());
      cell = '';
      continue;
    }
    cell += char;
  }
  cells.push(cell.trim());
  return cells;
}

function isSeparatorCell(cell) {
  const value = cell.trim();
  let index = 0;
  if (value[index] === ':') index += 1;
  let dashes = 0;
  while (value[index] === '-') {
    dashes += 1;
    index += 1;
  }
  if (value[index] === ':') index += 1;
  return dashes >= 3 && index === value.length;
}

function isSeparatorRow(line) {
  const cells = splitTableRow(line);
  return Array.isArray(cells) && cells.length > 0 && cells.every(isSeparatorCell);
}

function parseHeading(line) {
  let level = 0;
  while (line[level] === '#') level += 1;
  if (level === 0 || level > 6 || line[level] !== ' ') return null;
  const title = line.slice(level + 1).trim();
  return title ? { level, title } : null;
}

export function logicalHeadingTitle(title) {
  let index = 0;
  while (index < title.length && title[index] >= '0' && title[index] <= '9') index += 1;
  if (index === 0 || title[index] !== '.') return title.trim();
  index += 1;
  while (title[index] === ' ') index += 1;
  return title.slice(index).trim();
}

function parseFrontmatter(lines, errors) {
  const values = {};
  const duplicates = [];
  if (lines[0]?.trim() !== '---') return { values, duplicates, endLine: -1 };

  let endLine = -1;
  for (let index = 1; index < lines.length; index += 1) {
    if (lines[index].trim() === '---') {
      endLine = index;
      break;
    }
    const line = lines[index];
    if (!line.trim() || line.trimStart().startsWith('#')) continue;
    const separator = line.indexOf(':');
    if (separator <= 0) {
      errors.push({ line: index + 1, message: 'frontmatter 必须使用 key: value 平面格式' });
      continue;
    }
    const key = line.slice(0, separator).trim();
    let value = line.slice(separator + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (Object.hasOwn(values, key)) duplicates.push(key);
    values[key] = value;
  }
  if (endLine < 0) errors.push({ line: 1, message: 'frontmatter 缺少结束分隔符 ---' });
  return { values, duplicates, endLine };
}

function makeSection(heading) {
  return {
    type: 'section',
    level: heading.level,
    title: heading.title,
    logicalTitle: logicalHeadingTitle(heading.title),
    line: heading.line,
    nodes: [],
    tables: []
  };
}

export function parseMarkdown(source) {
  const lines = source.replaceAll('\r\n', '\n').split('\n');
  const errors = [];
  const frontmatter = parseFrontmatter(lines, errors);
  const root = {
    type: 'root',
    source,
    lines,
    frontmatter,
    headings: [],
    sections: [],
    nodes: [],
    codeBlocks: [],
    errors
  };

  let activeSection = null;
  let index = frontmatter.endLine >= 0 ? frontmatter.endLine + 1 : 0;
  while (index < lines.length) {
    const line = lines[index];
    const trimmed = line.trim();

    if (trimmed.startsWith('```') || trimmed.startsWith('~~~')) {
      const marker = trimmed.slice(0, 3);
      const startLine = index + 1;
      const content = [];
      index += 1;
      while (index < lines.length && !lines[index].trim().startsWith(marker)) {
        content.push(lines[index]);
        index += 1;
      }
      if (index >= lines.length) errors.push({ line: startLine, message: '代码块未闭合' });
      const node = { type: 'code', marker, content: content.join('\n'), line: startLine };
      root.codeBlocks.push(node);
      root.nodes.push(node);
      if (activeSection) activeSection.nodes.push(node);
      index += 1;
      continue;
    }

    const headingValue = parseHeading(line);
    if (headingValue) {
      const heading = { type: 'heading', ...headingValue, line: index + 1 };
      root.headings.push(heading);
      root.nodes.push(heading);
      if (heading.level === 2) {
        activeSection = makeSection(heading);
        root.sections.push(activeSection);
      } else if (activeSection) {
        activeSection.nodes.push(heading);
      }
      index += 1;
      continue;
    }

    const headerCells = splitTableRow(line);
    if (headerCells && isSeparatorRow(lines[index + 1] ?? '')) {
      const table = {
        type: 'table',
        headers: headerCells,
        rows: [],
        malformedRows: [],
        line: index + 1
      };
      index += 2;
      while (index < lines.length) {
        const rowCells = splitTableRow(lines[index]);
        if (!rowCells) break;
        if (rowCells.length !== table.headers.length) {
          table.malformedRows.push({ line: index + 1, cells: rowCells });
        } else if (!rowCells.every(isSeparatorCell)) {
          const values = {};
          table.headers.forEach((header, cellIndex) => {
            values[header] = rowCells[cellIndex];
          });
          table.rows.push({ values, cells: rowCells, line: index + 1 });
        }
        index += 1;
      }
      root.nodes.push(table);
      if (activeSection) {
        activeSection.nodes.push(table);
        activeSection.tables.push(table);
      }
      continue;
    }

    if (trimmed) {
      const node = { type: 'text', value: line, line: index + 1 };
      root.nodes.push(node);
      if (activeSection) activeSection.nodes.push(node);
    }
    index += 1;
  }

  return root;
}

export function tableKey(headers) {
  return headers.join('\u001f');
}

export function collectBodyText(ast) {
  const pieces = [];
  for (const node of ast.nodes) {
    if (node.type === 'text' || node.type === 'heading') pieces.push(node.value ?? node.title);
    if (node.type === 'table') {
      pieces.push(...node.headers);
      for (const row of node.rows) pieces.push(...row.cells);
    }
  }
  return pieces.join('\n');
}

export function findLiteralLine(ast, literal) {
  const needle = literal.toLocaleLowerCase('en-US');
  for (let index = 0; index < ast.lines.length; index += 1) {
    if (ast.lines[index].toLocaleLowerCase('en-US').includes(needle)) return index + 1;
  }
  return null;
}
