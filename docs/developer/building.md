# Building From Source

uSkyBlock uses Gradle and Java 21.

## Build

```bash
./gradlew build
```

Build output:

- Plugin JAR: `uSkyBlock-Plugin/build/libs/uSkyBlock.jar`

## Update translations

```bash
./gradlew translation
```

## Local docs preview

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
pip install -r requirements-docs.txt
mkdocs serve
```

The docs are then available at `http://127.0.0.1:8000`.
