from __future__ import annotations

import json
import sys
from pathlib import Path


data = json.loads(Path("outputs/busbooking-slide-update/source_extract.json").read_text(encoding="utf-8"))
terms = [t.lower() for t in sys.argv[1:]]
for i, p in enumerate(data["report"]["paragraphs"]):
    text = p["text"]
    low = text.lower()
    if any(term in low for term in terms):
        print(f"[{i}] {p['style']}: {text}")
