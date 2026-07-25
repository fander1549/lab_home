#!/usr/bin/env python3
"""Render full-day CH-to-alternative IRV changes.

NYC mapping is a direct port of ``Tools.GetRegionNumber`` in the supplied
``data_processing_c#`` project: Web-Mercator row conversion, 3x3-pixel region
lookup, and the four directional fallbacks for boundary points.  If a path
segment maps to several regions, its uniformly allocated duration is split
equally, exactly as the reference C# processor does.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
from collections.abc import Iterator
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

import geopandas as gpd
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.colors import LinearSegmentedColormap, TwoSlopeNorm
from shapely import STRtree, points as make_points

ROOT = Path(__file__).resolve().parents[2]
RATES = (0.05, 0.10, 0.15, 0.20)
IRV_UNIT_SECONDS = 30 * 60
NYC_FOCUS_EXTENT = (-74.045, -73.860, 40.655, 40.865)


def softened_diverging_colormap() -> LinearSegmentedColormap:
    """Keep the signed red-blue cue while reducing visual saturation."""
    colors = plt.get_cmap("RdBu_r")(np.linspace(0.0, 1.0, 256))
    colors[:, :3] = 0.68 * colors[:, :3] + 0.32
    return LinearSegmentedColormap.from_list("softened_RdBu_r", colors)


SOFTENED_RDBU = softened_diverging_colormap()


def haversine_meters(points: np.ndarray) -> np.ndarray:
    lat1, lon1 = np.radians(points[:-1, 0]), np.radians(points[:-1, 1])
    lat2, lon2 = np.radians(points[1:, 0]), np.radians(points[1:, 1])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    a = np.sin(dlat / 2) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2) ** 2
    return 6_371_008.8 * 2 * np.arctan2(np.sqrt(a), np.sqrt(1 - a))


@dataclass
class CityMapper:
    name: str
    region_count: int
    region_lists: Callable[[np.ndarray], list[np.ndarray]]
    draw: Callable[[plt.Axes, np.ndarray, TwoSlopeNorm], object]
    focus_extent: tuple[float, float, float, float] | None = None


def csharp_nyc_mapper(grid_path: Path, edge_buffer_pixels: int) -> CityMapper:
    """Port the C# mapper with a small, configurable boundary buffer.

    The reference C# implementation queries a 3×3 pixel window.  One extra
    pixel (the default) creates a modest 5×5-pixel overlap only where a point
    is close to a region edge, so edge segments can contribute to both sides.
    """
    if edge_buffer_pixels < 0:
        raise ValueError("edge_buffer_pixels must be non-negative")
    grid = np.loadtxt(grid_path, dtype=np.int16)
    height, width = grid.shape
    north, west, south, east = 40.918, -74.259, 40.486, -73.7
    query_radius = 1 + edge_buffer_pixels

    def mercator_y(latitude: float) -> float:
        sine = math.sin(math.radians(latitude))
        return math.log((sine + 1) / (1 - sine)) / 2

    north_y, south_y = mercator_y(north), mercator_y(south)

    def coordinate(latitude: float, longitude: float) -> tuple[int, int] | None:
        # The strict bounds and positive-pixel test intentionally match LatLngBox
        # and Tools.GetCoordinate in the user's C# source.
        if not (south < latitude < north and west < longitude < east):
            return None
        row = math.floor(height * (north_y - mercator_y(latitude)) / (north_y - south_y))
        col = math.floor(width * (longitude - west) / (east - west))
        if row <= 0 or col <= 0 or row >= height or col >= width:
            return None
        return row, col

    def nearby_regions(latitude: float, longitude: float) -> np.ndarray:
        pixel = coordinate(latitude, longitude)
        if pixel is None:
            return np.empty(0, dtype=np.int16)
        row, col = pixel
        cells = grid[
            max(row - query_radius, 0): min(row + query_radius + 1, height),
            max(col - query_radius, 0): min(col + query_radius + 1, width),
        ]
        values = np.unique(cells[(cells != 0) & (cells != -1)])
        return values.astype(np.int16) - 1

    def region_lists(points: np.ndarray) -> list[np.ndarray]:
        output: list[np.ndarray] = []
        for latitude, longitude in points:
            regions = nearby_regions(float(latitude), float(longitude))
            if not len(regions):
                # C# boundary fallback: latitude ±0.00045, longitude ±0.00062.
                probes = (
                    nearby_regions(float(latitude + 0.00045), float(longitude)),
                    nearby_regions(float(latitude - 0.00045), float(longitude)),
                    nearby_regions(float(latitude), float(longitude + 0.00062)),
                    nearby_regions(float(latitude), float(longitude - 0.00062)),
                )
                regions = np.unique(np.concatenate(probes)) if any(len(item) for item in probes) else regions
            output.append(regions)
        return output

    def draw(axis: plt.Axes, values: np.ndarray, norm: TwoSlopeNorm):
        image = values[np.maximum(grid, 1) - 1].astype(float)
        image[grid <= 0] = np.nan
        return axis.imshow(
            image,
            extent=(west, east, south, north),
            origin="upper",
            interpolation="nearest",
            cmap=SOFTENED_RDBU,
            norm=norm,
        )

    return CityMapper("NYC", 862, region_lists, draw, NYC_FOCUS_EXTENT)


def chicago_mapper(boundary_path: Path) -> CityMapper:
    boundaries = gpd.read_file(boundary_path).to_crs("EPSG:4326")
    boundaries["region"] = boundaries["area_num_1"].astype(int) - 1
    boundaries = boundaries.sort_values("region").reset_index(drop=True)
    tree = STRtree(boundaries.geometry.to_numpy())
    boundary_regions = boundaries["region"].to_numpy(dtype=int)

    def region_lists(points: np.ndarray) -> list[np.ndarray]:
        result = [[] for _ in range(len(points))]
        pairs = tree.query(make_points(points[:, 1], points[:, 0]), predicate="within")
        for point_index, boundary_index in pairs.T:
            result[int(point_index)].append(boundary_regions[int(boundary_index)])
        return [np.unique(item) if item else np.empty(0, dtype=int) for item in result]

    def draw(axis: plt.Axes, values: np.ndarray, norm: TwoSlopeNorm):
        frame = boundaries.copy()
        frame["delta"] = values[frame["region"].to_numpy()]
        return frame.plot(
            ax=axis,
            column="delta",
            cmap=SOFTENED_RDBU,
            norm=norm,
            edgecolor="#555555",
            linewidth=0.25,
            missing_kwds={"color": "#eeeeee"},
        ).collections[0]

    return CityMapper("Chicago", 77, region_lists, draw)


def path_irv(points: list[list[float]], duration_seconds: float, mapper: CityMapper) -> np.ndarray:
    """Uniformly allocate a trip's duration by segment length, then map it."""
    values = np.zeros(mapper.region_count, dtype=float)
    path = np.asarray(points, dtype=float)
    if len(path) < 2 or duration_seconds <= 0:
        return values
    lengths = haversine_meters(path)
    positive = lengths > 0
    if not positive.any():
        return values
    # Edge midpoints represent the road segments.  C# receives one point per
    # segment; this is the equivalent geometry for GraphHopper's point list.
    midpoints = (path[:-1] + path[1:]) / 2
    region_lists = mapper.region_lists(midpoints)
    total_length = lengths[positive].sum()
    for regions, length in zip(region_lists, lengths):
        if not len(regions) or length <= 0:
            continue
        contribution = duration_seconds * length / total_length / IRV_UNIT_SECONDS / len(regions)
        values[regions] += contribution
    return values


def records(paths: list[Path]) -> Iterator[dict]:
    for path in paths:
        with path.open() as handle:
            for line in handle:
                if line.strip():
                    yield json.loads(line)


def count_records(paths: list[Path]) -> int:
    return sum(sum(1 for line in path.open() if line.strip()) for path in paths)


def aggregate(paths: list[Path], mapper: CityMapper) -> tuple[np.ndarray, dict[float, np.ndarray], dict[float, int], int]:
    route_count = count_records(paths)
    targets = {rate: int(round(route_count * rate)) for rate in RATES}
    baseline = np.zeros(mapper.region_count, dtype=float)
    deltas = {rate: np.zeros(mapper.region_count, dtype=float) for rate in RATES}
    selected = {rate: 0 for rate in RATES}
    observed_dates: set[str] = set()

    for record in records(paths):
        observed_dates.add(record["date"])
        shortest = path_irv(record["shortest"], float(record["duration_s"]), mapper)
        baseline += shortest
        rank = record.get("replacement_rank")
        alternative = record.get("alternative")
        if rank is None or alternative is None:
            continue
        difference = path_irv(alternative, float(record["duration_s"]), mapper) - shortest
        for rate, target in targets.items():
            if rank < target:
                deltas[rate] += difference
                selected[rate] += 1
    if len(observed_dates) != 1:
        raise ValueError(f"route chunks contain multiple dates: {sorted(observed_dates)}")
    return baseline, deltas, selected, route_count


def write_regions(path: Path, baseline: np.ndarray, delta: np.ndarray, rate: float, selected: int) -> None:
    with path.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["region_id", "baseline_irv", "perturbed_irv", "delta_irv", "replacement_rate", "selected_paths"])
        for region_id, (base, difference) in enumerate(zip(baseline, delta), start=1):
            writer.writerow([region_id, f"{base:.10f}", f"{base + difference:.10f}", f"{difference:.10f}", rate, selected])


def render(city_key: str, mapper: CityMapper, baseline: np.ndarray, deltas: dict[float, np.ndarray], selected: dict[float, int], output_dir: Path) -> list[dict]:
    maximum = max(float(np.abs(delta).max()) for delta in deltas.values())
    norm = TwoSlopeNorm(vmin=-maximum, vcenter=0.0, vmax=maximum) if maximum else TwoSlopeNorm(vmin=-1, vcenter=0, vmax=1)
    figure, axes = plt.subplots(2, 2, figsize=(12, 10), constrained_layout=True)
    image = None
    rows = []
    for axis, rate in zip(axes.flat, RATES):
        difference = deltas[rate]
        image = mapper.draw(axis, difference, norm)
        if mapper.focus_extent is not None:
            west, east, south, north = mapper.focus_extent
            axis.set_xlim(west, east)
            axis.set_ylim(south, north)
        axis.set_title(f"{int(rate * 100)}% replacement ({selected[rate]:,} paths)")
        axis.set_xlabel("Longitude")
        axis.set_ylabel("Latitude")
        axis.set_aspect("equal")
        write_regions(output_dir / f"{city_key}_{int(rate * 100)}pct_regions.csv", baseline, difference, rate, selected[rate])
        rows.append({
            "city": city_key,
            "replacement_rate": rate,
            "selected_paths": selected[rate],
            "baseline_irv": baseline.sum(),
            "net_delta_irv": difference.sum(),
            "absolute_delta_irv": np.abs(difference).sum(),
            "changed_regions": int(np.count_nonzero(np.abs(difference) > 1e-12)),
        })
    figure.colorbar(image, ax=axes.ravel().tolist(), shrink=0.82, label="ΔIRV (alternative − CH shortest), 30-min units")
    figure.suptitle(f"{mapper.name}: full-day IRV change from CH path replacement", fontsize=14)
    figure.savefig(output_dir / f"{city_key}_irv_difference.png", dpi=220, bbox_inches="tight")
    plt.close(figure)
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--nyc-routes", type=Path, nargs="+")
    parser.add_argument("--chicago-routes", type=Path, nargs="+")
    parser.add_argument("--output-dir", type=Path, default=ROOT / "outputs" / "irv_perturbation" / "full_day")
    parser.add_argument("--nyc-grid", type=Path)
    parser.add_argument(
        "--nyc-boundary-buffer-px",
        type=int,
        default=1,
        help="Extra pixels beyond the C# 3x3 NYC region query (default: 1, giving a 5x5 window)",
    )
    parser.add_argument("--chicago-boundary", type=Path)
    parser.add_argument("--city", choices=("nyc", "chicago"), help="Render one city per invocation for long full-day runs")
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)

    summary = []
    configurations = []
    if args.city in (None, "nyc"):
        if not args.nyc_routes or args.nyc_grid is None:
            parser.error("NYC rendering requires --nyc-routes and --nyc-grid")
        configurations.append(
            ("nyc", args.nyc_routes, csharp_nyc_mapper(args.nyc_grid, args.nyc_boundary_buffer_px))
        )
    if args.city in (None, "chicago"):
        if not args.chicago_routes or args.chicago_boundary is None:
            parser.error("Chicago rendering requires --chicago-routes and --chicago-boundary")
        configurations.append(
            ("chicago", args.chicago_routes, chicago_mapper(args.chicago_boundary))
        )

    for key, route_file, mapper in configurations:
        baseline, deltas, selected, count = aggregate(route_file, mapper)
        city_rows = render(key, mapper, baseline, deltas, selected, args.output_dir)
        for row in city_rows:
            row["all_valid_paths"] = count
        summary.extend(city_rows)

    summary_name = "summary.csv" if args.city is None else f"summary_{args.city}.csv"
    with (args.output_dir / summary_name).open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(summary[0]))
        writer.writeheader()
        writer.writerows(summary)


if __name__ == "__main__":
    main()
