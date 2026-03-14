"""MkDocs hook that generates the config reference page from the bundled config.yml.

Splits the config into sections (one per top-level key, or per options.* child)
and renders each as a headed code block for sidebar TOC navigation.
"""

import logging
import re
from pathlib import Path

log = logging.getLogger("mkdocs.hooks.gen_config_reference")

_REPO_ROOT = Path(__file__).resolve().parent.parent.parent
_CONFIG_SOURCE = _REPO_ROOT / "uSkyBlock-Core" / "src" / "main" / "resources" / "config.yml"
_OUTPUT_PAGE = _REPO_ROOT / "docs" / "src" / "admin" / "config-reference.md"
_HEADER_SOURCE = _REPO_ROOT / "docs" / "src" / "admin" / ".config-reference-header.md"

# Display titles for sections.
_TITLES = {
    'language': 'Language',
    'general': 'General',
    'island': 'Island',
    'extras': 'Extras',
    'protection': 'Protection',
    'spawning': 'Spawning',
    'party': 'Party',
    'advanced': 'Advanced',
    'restart': 'Restart',
    'donor-perks': 'Donor Perks',
    'island-schemes': 'Island Schemes',
    'confirmation': 'Confirmation',
    'asyncworldedit': 'AsyncWorldEdit',
    'worldguard': 'WorldGuard',
    'signs': 'Signs',
    'nether': 'Nether',
    'tool-menu': 'Tool Menu',
    'plugin-updates': 'Plugin Updates',
    'placeholder': 'Placeholders',
}

# Sections to merge: key is absorbed into the section that follows it.
_MERGE_INTO_NEXT = {'island-schemes-enabled'}


def on_pre_build(config, **kwargs):
    """Generate the config reference Markdown file before MkDocs reads pages."""
    if not _CONFIG_SOURCE.is_file():
        log.warning("Config source not found at %s — skipping generation.", _CONFIG_SOURCE)
        return

    raw = _CONFIG_SOURCE.read_text(encoding="utf-8")
    lines = raw.splitlines()

    # Strip the internal version footer.
    filtered = []
    for line in lines:
        if line.startswith("# DO NOT TOUCH"):
            break
        filtered.append(line)
    while filtered and filtered[-1].strip() == "":
        filtered.pop()

    header = _HEADER_SOURCE.read_text(encoding="utf-8") if _HEADER_SOURCE.is_file() else ""

    sections = _split_sections(filtered)
    page = _build_page(header, sections)

    _OUTPUT_PAGE.parent.mkdir(parents=True, exist_ok=True)
    # Only write when content changed to avoid triggering a live-reload loop.
    if _OUTPUT_PAGE.is_file() and _OUTPUT_PAGE.read_text(encoding="utf-8") == page:
        log.info("Config reference unchanged — skipping write.")
        return
    _OUTPUT_PAGE.write_text(page, encoding="utf-8")
    log.info("Generated config reference at %s", _OUTPUT_PAGE)


def _split_sections(lines):
    """Split config lines into named sections.

    Returns list of (key, is_options_child, content_lines) tuples.
    Each section's content_lines are the raw lines from the file (preceding
    comments through all content), excluding the section key line itself and
    the ``options:`` wrapper.
    """
    # Phase 1: find all section boundaries.
    # Each boundary is (line_index_of_key, key_name, is_options_child).
    boundaries = []
    options_line = None
    in_options = False

    for i, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith('#'):
            continue

        m_top = re.match(r'^([\w][\w-]*)\s*:', line)
        if m_top:
            key = m_top.group(1)
            if key == 'options':
                in_options = True
                options_line = i
                continue
            in_options = False
            boundaries.append((i, key, False))
            continue

        if in_options:
            m_child = re.match(r'^  ([\w][\w-]*)\s*:', line)
            if m_child:
                boundaries.append((i, m_child.group(1), True))

    if not boundaries:
        return []

    # Phase 2: determine line ranges for each section.
    # Strategy: each section owns from its preceding comments through
    # the line before the next section's preceding comments.
    # We exclude: the section key line itself, `options:` lines, and
    # blank lines immediately after the key line.
    sections_raw = []

    for idx, (key_line, key, is_child) in enumerate(boundaries):
        # Find start of preceding comments.
        comment_start = key_line
        while comment_start > 0:
            prev = lines[comment_start - 1].strip()
            if prev.startswith('#') or prev == '':
                comment_start -= 1
            else:
                break
        # Skip leading blank lines.
        while comment_start < key_line and lines[comment_start].strip() == '':
            comment_start += 1

        # Don't overlap with previous section or grab the `options:` line.
        if sections_raw:
            comment_start = max(comment_start, sections_raw[-1]['raw_end'])
        if options_line is not None:
            if comment_start <= options_line < key_line:
                comment_start = options_line + 1
                # Skip blank lines after options:
                while comment_start < key_line and lines[comment_start].strip() == '':
                    comment_start += 1

        # End of section: start of next section's comment block.
        if idx + 1 < len(boundaries):
            next_key_line = boundaries[idx + 1][0]
            # Walk backward to find where next section's comments start.
            raw_end = next_key_line
            while raw_end > key_line + 1 and (lines[raw_end - 1].strip() == '' or lines[raw_end - 1].strip().startswith('#')):
                raw_end -= 1
        else:
            raw_end = len(lines)

        sections_raw.append({
            'key': key,
            'is_child': is_child,
            'comment_start': comment_start,
            'key_line': key_line,
            'raw_end': raw_end,
        })

    # Phase 3: build section content.
    # For options children: skip the key line (the heading replaces it) and dedent.
    # For top-level sections: include the key line in the code block.
    result = []
    for s in sections_raw:
        pre_comments = lines[s['comment_start']:s['key_line']]
        key_line_text = lines[s['key_line']]

        body_start = s['key_line'] + 1
        while body_start < s['raw_end'] and lines[body_start].strip() == '':
            body_start += 1
        body = lines[body_start:s['raw_end']]

        # Filter out the bare `options:` wrapper line if it leaked into a section body.
        body = [l for l in body if l.strip() != 'options:']

        if s['is_child']:
            # Options children: key line is redundant with heading.
            result.append((s['key'], True, pre_comments, key_line_text, body))
        else:
            # Top-level: include the key line as part of the body.
            result.append((s['key'], False, pre_comments, key_line_text, [key_line_text] + body))

    # Phase 4: merge sections (e.g. island-schemes-enabled → island-schemes).
    merged = []
    pending = None
    for key, is_child, pre, _key_line_text, body in result:
        if key in _MERGE_INTO_NEXT:
            pending = (key, is_child, pre, body)
            continue
        if pending:
            # Prepend the pending section's full content.
            pre = pending[2] + pending[3] + [''] + pre
            pending = None
        merged.append((key, is_child, pre, body))
    if pending:
        merged.append((pending[0], pending[1], pending[2], pending[3]))

    return merged


def _build_page(header, sections):
    """Build the Markdown page from parsed sections."""
    parts = [header.rstrip() + "\n\n"]

    for key, is_child, pre_comments, body in sections:
        title = _TITLES.get(key, key.replace('-', ' ').title())
        path = f"options.{key}" if is_child else key

        parts.append(f"## {title} {{ #{key} }}\n\n")
        parts.append(f"Config path: `{path}`\n\n")

        # Determine dedent: use the indent of the first non-blank content line.
        dedent = _detect_indent(body) if body else 0

        # Combine pre-comments and body.
        block = pre_comments + body

        code = _dedent_lines(block, dedent)

        # Trim leading/trailing blank lines in the code block.
        while code and code[0].strip() == '':
            code.pop(0)
        while code and code[-1].strip() == '':
            code.pop()

        if code:
            parts.append("```yaml\n")
            parts.append("\n".join(code))
            parts.append("\n```\n\n")

    return "".join(parts)


def _detect_indent(lines):
    """Return the minimum indentation (in spaces) of non-blank lines."""
    min_indent = None
    for line in lines:
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip())
        if min_indent is None or indent < min_indent:
            min_indent = indent
    return min_indent or 0


def _dedent_lines(lines, amount):
    """Remove up to ``amount`` leading spaces from each line. Returns list."""
    if amount == 0:
        return list(lines)
    result = []
    for line in lines:
        if not line.strip():
            result.append('')
        elif line.startswith(' ' * amount):
            result.append(line[amount:])
        else:
            # Less indented than expected (e.g. a comment at section level).
            # Dedent as much as possible.
            stripped = line.lstrip()
            result.append(stripped)
    return result
