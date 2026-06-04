from __future__ import annotations

from pathlib import Path


slides_dir = Path("outputs/busbooking-slide-update/slides")
for i in range(1, 21):
    name = f"slide-{i:02d}.mjs"
    fn = f"slide{i:02d}"
    (slides_dir / name).write_text(
        f"""import {{ renderSlide }} from './deck.mjs';

export async function {fn}(presentation, ctx) {{
  return renderSlide(presentation, ctx, {i});
}}
""",
        encoding="utf-8",
    )
