# Edit Docs

You do not need a local dev setup to edit docs.

## Quick edit flow (GitHub web)

1. Open any docs page in the repository under `docs/`.
2. Click the pencil icon ("Edit this file").
3. Make your changes in Markdown.
4. Submit a pull request.

## Writing guidelines

- Use plain language and short sections.
- Prefer concrete steps over long explanations.
- Include exact command names and file paths.
- Keep instructions testable by another admin.

## Local preview (optional)

If you want to preview changes before opening a pull request:

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
pip install -r requirements-docs.txt
mkdocs serve
```
