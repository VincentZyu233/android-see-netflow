from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RUST_CORE_DIR = REPO_ROOT / "client" / "rust-core"
ANDROID_APP_DIR = REPO_ROOT / "client" / "android" / "app"
JNI_OUTPUT_DIR = ANDROID_APP_DIR / "build" / "generated" / "rustJniLibs" / "jniLibs"
LIB_BASENAME = "libandroid_see_netflow_core.so"

TARGETS = {
    "arm64-v8a": "aarch64-linux-android",
    "armeabi-v7a": "armv7-linux-androideabi",
    "x86_64": "x86_64-linux-android",
}


def run(cmd: list[str], cwd: Path | None = None, extra_env: dict[str, str] | None = None) -> None:
    env = os.environ.copy()
    if extra_env:
        env.update(extra_env)
    print(" ".join(cmd))
    subprocess.run(cmd, cwd=cwd, env=env, check=True)


def main() -> None:
    if shutil.which("cargo") is None:
        raise SystemExit("cargo not found in PATH")

    if shutil.which("cargo-ndk") is None and shutil.which("cargo") is not None:
        print("cargo-ndk not found as standalone command, trying `cargo ndk` subcommand")

    android_ndk_home = (
        os.environ.get("ANDROID_NDK_HOME")
        or os.environ.get("ANDROID_NDK_ROOT")
        or os.environ.get("ANDROID_NDK_LATEST_HOME")
    )
    if not android_ndk_home:
        raise SystemExit("ANDROID_NDK_HOME or ANDROID_NDK_ROOT is required")

    if JNI_OUTPUT_DIR.exists():
        shutil.rmtree(JNI_OUTPUT_DIR)
    JNI_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    for abi, rust_target in TARGETS.items():
        abi_dir = JNI_OUTPUT_DIR / abi
        abi_dir.mkdir(parents=True, exist_ok=True)
        run(
            [
                "cargo",
                "ndk",
                "-t",
                abi,
                "-o",
                str(JNI_OUTPUT_DIR),
                "build",
                "--release",
            ],
            cwd=RUST_CORE_DIR,
            extra_env={"ANDROID_NDK_HOME": android_ndk_home},
        )
        built_lib = abi_dir / LIB_BASENAME
        if not built_lib.exists():
            raise SystemExit(f"Expected Rust library not found: {built_lib}")

    print(f"Built Rust Android libraries into {JNI_OUTPUT_DIR}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode) from exc
