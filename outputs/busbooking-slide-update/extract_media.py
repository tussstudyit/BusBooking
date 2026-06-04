from __future__ import annotations

import math
import shutil
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path("outputs/busbooking-slide-update")
DOCX = Path(r"D:\hk2n2\Đồ án\BaoCao_DACS3_2.docx")
PPTX = Path(r"D:\hk2n2\Đồ án\SlideBaoCao_DACS3.pptx")
MEDIA = ROOT / "assets" / "extracted"


def extract_package_media(package: Path, prefix: str, inner_prefix: str) -> list[Path]:
    out_dir = MEDIA / prefix
    out_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []
    with zipfile.ZipFile(package) as zf:
        for idx, name in enumerate([n for n in zf.namelist() if n.startswith(inner_prefix)], start=1):
            suffix = Path(name).suffix.lower()
            if suffix not in {".png", ".jpg", ".jpeg"}:
                continue
            out = out_dir / f"{prefix}-{idx:03d}{suffix}"
            out.write_bytes(zf.read(name))
            paths.append(out)
    return paths


def make_contact_sheet(paths: list[Path], out: Path) -> None:
    thumbs = []
    for path in paths:
        try:
            img = Image.open(path).convert("RGB")
        except Exception:
            continue
        img.thumbnail((220, 140))
        canvas = Image.new("RGB", (240, 178), "white")
        x = (240 - img.width) // 2
        canvas.paste(img, (x, 8))
        draw = ImageDraw.Draw(canvas)
        draw.text((8, 150), path.stem, fill=(30, 30, 30))
        thumbs.append(canvas)
    if not thumbs:
        return
    cols = 4
    rows = math.ceil(len(thumbs) / cols)
    sheet = Image.new("RGB", (cols * 240, rows * 178), (245, 247, 250))
    for i, thumb in enumerate(thumbs):
        sheet.paste(thumb, ((i % cols) * 240, (i // cols) * 178))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)


def main() -> None:
    if MEDIA.exists():
        shutil.rmtree(MEDIA)
    doc_images = extract_package_media(DOCX, "docx", "word/media/")
    ppt_images = extract_package_media(PPTX, "pptx", "ppt/media/")
    all_images = doc_images + ppt_images
    make_contact_sheet(all_images, ROOT / "assets" / "media-contact-sheet.jpg")
    print(f"docx_images={len(doc_images)} pptx_images={len(ppt_images)} total={len(all_images)}")


if __name__ == "__main__":
    main()
