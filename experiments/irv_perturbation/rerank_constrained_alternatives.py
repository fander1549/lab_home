#!/usr/bin/env python3
"""Create one deterministically ranked JSONL stream from strict route chunks."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


CHUNK_NAME = re.compile(r"routes_(\d+)_(\d+)\.jsonl$")


def contiguous_chunks(routes_dir: Path) -> list[Path]:
    """Choose the smallest contiguous chunks and ignore interrupted overlaps."""
    by_start: dict[int, list[tuple[int, Path]]] = {}
    for path in routes_dir.glob("routes_*.jsonl"):
        match = CHUNK_NAME.fullmatch(path.name)
        if match is None:
            continue
        start, end = map(int, match.groups())
        if end > start:
            by_start.setdefault(start, []).append((end, path))

    result: list[Path] = []
    current = 0
    while current in by_start:
        end, path = min(by_start[current], key=lambda item: item[0])
        result.append(path)
        current = end
    if not result:
        raise ValueError(f"No contiguous route chunks beginning at zero in {routes_dir}")
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--routes-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--selected-paths", type=int, required=True)
    args = parser.parse_args()

    paths = contiguous_chunks(args.routes_dir)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    rank = 0
    record_count = 0
    with args.output.open("w") as destination:
        for path in paths:
            with path.open() as source:
                for line in source:
                    if not line.strip():
                        continue
                    record = json.loads(line)
                    if record.get("alternative") is not None and rank < args.selected_paths:
                        record["replacement_rank"] = rank
                        rank += 1
                    else:
                        record["replacement_rank"] = None
                        record["alternative"] = None
                    destination.write(json.dumps(record, separators=(",", ":")) + "\n")
                    record_count += 1

    if rank != args.selected_paths:
        raise ValueError(f"Only {rank} strict alternatives available; expected {args.selected_paths}")
    print(f"Wrote {record_count} routes with {rank} deterministically ranked alternatives to {args.output}")


if __name__ == "__main__":
    main()
