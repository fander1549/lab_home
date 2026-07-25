#!/usr/bin/env python3
"""Extract a single Chicago calendar day from the 2019 trip-record file."""
from __future__ import annotations

import argparse
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--date", required=True, help="MM/DD/YYYY")
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with args.input.open() as source, args.output.open("w") as target:
        for line in source:
            fields = line.split(",", 4)
            if len(fields) >= 4 and fields[3].startswith(args.date + " "):
                target.write(line)
                count += 1
    print(f"wrote {count} records for {args.date} to {args.output}")


if __name__ == "__main__":
    main()
