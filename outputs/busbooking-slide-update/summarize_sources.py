from __future__ import annotations

import json
from pathlib import Path


data = json.loads(Path("outputs/busbooking-slide-update/source_extract.json").read_text(encoding="utf-8"))
print("paragraphs", len(data["report"]["paragraphs"]))
print("tables", len(data["report"]["tables"]))
print("slides", len(data["existing_deck"]))
print("\nHEADINGS")
for p in data["report"]["paragraphs"]:
    if p["style"].lower().startswith("heading"):
        print(p["text"])
print("\nSLIDE TEXT")
for s in data["existing_deck"]:
    print(f"{s['slide']}: {s['text'][:260]}")
