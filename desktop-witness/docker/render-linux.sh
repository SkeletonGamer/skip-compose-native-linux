#!/usr/bin/env bash
# Build the Linux image and render the witness offscreen inside it, then copy the
# produced PNGs to ../docs/linux-*.png. Run from the desktop-witness/ project root.
set -euo pipefail

here="$(cd "$(dirname "$0")/.." && pwd)"   # desktop-witness/
cd "$here"

img="poc-skip-linux"
out="docker/out"
mkdir -p "$out"

echo ">> building image $img"
docker build -f docker/Dockerfile -t "$img" .

echo ">> rendering inside the container"
docker run --rm -v "$here/$out:/app/docs" "$img"

echo ">> copying results to ../docs/linux-*"
cp "$out/desktop-witness.png"             "../docs/linux-witness.png"
cp "$out/desktop-witness-after-click.png" "../docs/linux-witness-after-click.png"
echo ">> done: docs/linux-witness.png, docs/linux-witness-after-click.png"
