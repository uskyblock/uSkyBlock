#!/usr/bin/env python3

import argparse
import re
from html.parser import HTMLParser
from pathlib import Path


class HtmlToBbcodeParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []
        self.href_stack: list[str | None] = []
        self.list_stack: list[str] = []
        self.inline_code_depth = 0
        self.pre_depth = 0
        self.blockquote_depth = 0

    def append(self, text: str) -> None:
        if text:
            self.parts.append(text)

    def ensure_newlines(self, count: int) -> None:
        current = "".join(self.parts)
        stripped = current.rstrip(" \t")
        existing = len(current) - len(stripped)
        if existing:
            self.parts[-1] = self.parts[-1][: len(self.parts[-1]) - existing]
        current = "".join(self.parts)
        newline_count = len(current) - len(current.rstrip("\n"))
        if newline_count < count:
            self.parts.append("\n" * (count - newline_count))

    def heading_just_closed(self) -> bool:
        return "".join(self.parts).endswith("[/B]\n\n")

    def trim_trailing_newlines(self, count: int) -> None:
        current = "".join(self.parts)
        newline_count = len(current) - len(current.rstrip("\n"))
        if newline_count <= count:
            return
        trimmed = current[:- (newline_count - count)]
        self.parts = [trimmed] if trimmed else []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attr_map = dict(attrs)
        if tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            self.ensure_newlines(2)
            self.append("[B]")
        elif tag in {"strong", "b"}:
            self.append("[B]")
        elif tag in {"em", "i"}:
            self.append("[I]")
        elif tag == "a":
            href = attr_map.get("href")
            self.href_stack.append(href)
            if href:
                self.append(f"[URL='{href}']")
        elif tag == "br":
            self.append("\n")
        elif tag == "p":
            self.ensure_newlines(2)
        elif tag == "blockquote":
            self.ensure_newlines(2)
            self.blockquote_depth += 1
            self.append("[INDENT]")
        elif tag == "ul":
            after_heading = self.heading_just_closed()
            if after_heading:
                self.trim_trailing_newlines(1)
            self.ensure_newlines(1 if after_heading else 2)
            self.list_stack.append("ul")
            self.append("[LIST]\n")
        elif tag == "ol":
            after_heading = self.heading_just_closed()
            if after_heading:
                self.trim_trailing_newlines(1)
            self.ensure_newlines(1 if after_heading else 2)
            self.list_stack.append("ol")
            self.append("[LIST=1]\n")
        elif tag == "li":
            self.ensure_newlines(1)
            self.append("[*] ")
        elif tag == "pre":
            self.ensure_newlines(2)
            self.pre_depth += 1
            self.append("[CODE]\n")
        elif tag == "code":
            if self.pre_depth == 0:
                self.inline_code_depth += 1
                self.append("`")
        elif tag == "hr":
            self.ensure_newlines(2)
            self.append("---")
            self.ensure_newlines(2)
        elif tag in {"table", "tr"}:
            self.ensure_newlines(1)
        elif tag in {"th", "td"}:
            if self.parts and not "".join(self.parts).endswith(("\n", "[*] ")):
                self.append(" | ")
        elif tag == "img":
            alt = attr_map.get("alt")
            if alt:
                self.append(alt)

    def handle_endtag(self, tag: str) -> None:
        if tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            self.append("[/B]")
            self.ensure_newlines(2)
        elif tag in {"strong", "b"}:
            self.append("[/B]")
        elif tag in {"em", "i"}:
            self.append("[/I]")
        elif tag == "a":
            href = self.href_stack.pop() if self.href_stack else None
            if href:
                self.append("[/URL]")
        elif tag == "p":
            self.ensure_newlines(2)
        elif tag == "blockquote":
            self.append("[/INDENT]")
            self.ensure_newlines(2)
            if self.blockquote_depth > 0:
                self.blockquote_depth -= 1
        elif tag in {"ul", "ol"}:
            if self.list_stack:
                self.list_stack.pop()
            self.ensure_newlines(1)
            self.append("[/LIST]")
            self.ensure_newlines(2)
        elif tag == "li":
            self.ensure_newlines(1)
        elif tag == "pre":
            self.ensure_newlines(1)
            self.append("[/CODE]")
            self.ensure_newlines(2)
            if self.pre_depth > 0:
                self.pre_depth -= 1
        elif tag == "code":
            if self.pre_depth == 0 and self.inline_code_depth > 0:
                self.append("`")
                self.inline_code_depth -= 1
        elif tag in {"tr", "table"}:
            self.ensure_newlines(1)

    def handle_data(self, data: str) -> None:
        if not data:
            return
        if self.pre_depth > 0:
            self.append(data)
            return

        normalized = re.sub(r"\s+", " ", data)
        if not normalized.strip():
            return

        content = normalized.strip()
        current = "".join(self.parts)
        if normalized[0].isspace() and current and not current.endswith((" ", "\n", "(", "[", "/", "=")):
            self.append(" ")
        self.append(content)
        if normalized[-1].isspace():
            self.append(" ")

    def get_output(self) -> str:
        output = "".join(self.parts)
        output = re.sub(r"[ \t]+\n", "\n", output)
        output = re.sub(r"\n{3,}", "\n\n", output)
        return output.strip() + "\n"


def derive_spigot_title(title: str, version: str) -> str:
    cleaned = title.strip() or version.strip()
    version_prefix = re.compile(rf"^v?{re.escape(version)}(?:\b|\s|$)")
    if version_prefix.match(cleaned):
        return cleaned
    return f"{version} - {cleaned}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--html", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--title-output", required=True)
    parser.add_argument("--notes-output", required=True)
    args = parser.parse_args()

    html = Path(args.html).read_text(encoding="utf-8")
    converter = HtmlToBbcodeParser()
    converter.feed(html)
    converter.close()

    bbcode = converter.get_output()
    title = derive_spigot_title(args.title, args.version)

    Path(args.title_output).write_text(title + "\n", encoding="utf-8")
    Path(args.notes_output).write_text(bbcode, encoding="utf-8")


if __name__ == "__main__":
    main()
