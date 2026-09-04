# SF Symbol Catalog

A cross-platform **Compose Desktop** app for browsing, searching, and copying **Apple SF Symbols** — the icon set Apple ships with its operating systems. It exposes **7,007 symbols** in both **Dualtone** and **Monochrome** variants, wrapped in a native-feeling macOS-style interface.

![Platforms](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-blue)

## Features

- **7,007 SF Symbols** in Dualtone (default) and Monochrome variants
- **Fuzzy search** across every symbol name
- **Browse by category** with live per-category counts
- **Favorites / pinned symbols** — persisted across sessions
- **Multi-select** to favorite or bulk-manage symbols
- **Copy to clipboard** with one click:
  - Apple name (`heart.fill`)
  - Pascal name (`SFHeartFill`)
  - Code snippet (`SfSymbols.Dualtone.SFHeartFill`)
- **Preview customization**:
  - Light / dark theme
  - Icon tint color, background color, and tint toggle
  - Grid scale and grid / list views
- Native **macOS-glass** aesthetic (Haze backdrop blur, rounded floating panels) on every platform

## Downloads

Installers are built automatically by GitHub Actions and attached to each [GitHub Release](https://github.com/cosmictaserdev-creator/SF_Symbol_Catalog/releases):

| Platform | Installer |
|----------|-----------|
| Windows | `.msi` |
| macOS   | `.dmg` |
| Linux   | `.deb` |

Pick the installer for your OS from the latest release and run it.

## Usage

Launch the app, type in the search bar, or pick a category in the sidebar. Click any symbol to copy its names or code snippet, and use **Settings** to tune colors, theme, and layout.

## Development

### Requirements

- JDK 17+
- A macOS setup is only needed to build the `.dmg`; Windows/Linux builds work on their respective hosts.

### Build & run

```bash
# Run the app
./gradlew run

# Build a platform installer
./gradlew packageMsi   # Windows
./gradlew packageDmg   # macOS
./gradlew packageDeb   # Linux
```

### Project layout

```
app/src/jvmMain/
├── kotlin/com/sfsymbols/
│   ├── data/          # 7,007-strong Dualtone + Monochrome image catalogs
│   ├── mcp/           # MCP server for tooling integration
│   ├── ui/            # macOS-compose screens (catalog, sidebar, detail, settings)
│   ├── util/          # clipboard + URL helpers
│   └── viewmodel/     # plain Compose state / controller
└── resources/         # app icon & reference image
```

## Releases

Tags starting with `v` trigger the [release workflow](.github/workflows/release.yml), which:

1. Builds the `.msi`, `.dmg`, and `.deb` installers on their native CI runners
2. Uploads them as artifacts
3. Attaches them to a GitHub Release with auto-generated notes

To ship a new version:

```bash
git tag v1.1.0
git push origin v1.1.0
```

## License

[MIT](LICENSE)
