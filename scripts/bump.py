from __future__ import annotations

import re
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CARGO_TOML = REPO_ROOT / "client" / "rust-core" / "Cargo.toml"
ANDROID_KTS = REPO_ROOT / "client" / "android" / "app" / "build.gradle.kts"

VERSION_RE = re.compile(r"^\d+\.\d+\.\d+$")
CARGO_VERSION_RE = re.compile(r'^(version\s*=\s*")([^"]+)(")', re.MULTILINE)
KTS_VERSION_RE = re.compile(
    r'^(val\s+appVersionName\s*=\s*providers\.gradleProperty\("APP_VERSION_NAME"\)\.orElse\(")([^"]+)("\))',
    re.MULTILINE,
)

RESET = "\033[0m"
BOLD = "\033[1m"
ITALIC = "\033[3m"
GREEN = "\033[32m"
CYAN = "\033[36m"
YELLOW = "\033[33m"
MAGENTA = "\033[35m"
RED = "\033[31m"


def style(text: str, *codes: str) -> str:
    return "".join(codes) + text + RESET


def replace_once(path: Path, pattern: re.Pattern[str], new_value: str, label: str) -> None:
    original = path.read_text(encoding="utf-8")
    updated, count = pattern.subn(rf"\g<1>{new_value}\g<3>", original, count=1)
    if count != 1:
        raise SystemExit(style(f"❌ Failed to update {label} in {path}", BOLD, RED))
    path.write_text(updated, encoding="utf-8", newline="\n")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(style("📎 Usage: python ./scripts/bump.py x.y.z", BOLD, YELLOW))

    version = sys.argv[1].strip()
    if not VERSION_RE.fullmatch(version):
        raise SystemExit(style("🚫 Invalid version. Expected format: x.y.z", BOLD, RED))

    print(style("🚀 Bumping project version...", BOLD, MAGENTA))
    print(f"{style('   Target', CYAN)} {style(version, BOLD, GREEN)}")

    replace_once(CARGO_TOML, CARGO_VERSION_RE, version, "Cargo.toml version")
    replace_once(ANDROID_KTS, KTS_VERSION_RE, version, "Android default version")

    print(style("\n✅ Version bump completed", BOLD, GREEN))
    print(f"{style('🦀 Rust', BOLD, YELLOW)}   {style(str(CARGO_TOML.relative_to(REPO_ROOT)), ITALIC)}")
    print(f"{style('🤖 Android', BOLD, YELLOW)} {style(str(ANDROID_KTS.relative_to(REPO_ROOT)), ITALIC)}")
    print(f"{style('🏷️  New Version', CYAN)} {style(version, BOLD, GREEN)}")


if __name__ == "__main__":
    main()
