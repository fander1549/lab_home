#!/usr/bin/env python3
"""Explain the strongest Chicago IRV gain through route and road-network shifts."""
from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

import geopandas as gpd
import matplotlib.pyplot as plt
import numpy as np
from shapely import LineString

sys.path.insert(0, str(Path(__file__).parent))
from render_full_day_irv_perturbation import chicago_mapper, haversine_meters, path_irv  # noqa: E402


def target_segments(points: list[list[float]], target_index: int, mapper, duration_seconds: float):
    """Return route segments in one community with their uniform IRV weights."""
    path = np.asarray(points, dtype=float)
    if len(path) < 2:
        return []
    lengths = haversine_meters(path)
    total_length = lengths[lengths > 0].sum()
    if total_length == 0:
        return []
    midpoints = (path[:-1] + path[1:]) / 2
    memberships = mapper.region_lists(midpoints)
    result = []
    for first, second, length, regions in zip(path[:-1], path[1:], lengths, memberships):
        if length <= 0 or target_index not in regions:
            continue
        contribution = duration_seconds * length / total_length / 1800 / len(regions)
        result.append((LineString([(first[1], first[0]), (second[1], second[0])]), contribution))
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--routes", type=Path, required=True)
    parser.add_argument("--boundaries", type=Path, required=True)
    parser.add_argument("--roads", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--selected-paths", type=int, required=True)
    parser.add_argument("--target-region", type=int, default=28)
    parser.add_argument("--top-routes", type=int, default=120)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)

    boundaries = gpd.read_file(args.boundaries).to_crs("EPSG:4326")
    boundaries["region"] = boundaries["area_num_1"].astype(int) - 1
    names = dict(zip(boundaries["region"], boundaries["community"]))
    mapper = chicago_mapper(args.boundaries)
    target = args.target_region - 1

    donors = np.zeros(mapper.region_count, dtype=float)
    gain_routes: list[tuple[float, dict]] = []
    total_delta = np.zeros(mapper.region_count, dtype=float)
    with args.routes.open() as source:
        for line in source:
            record = json.loads(line)
            if record.get("replacement_rank") is None or record["replacement_rank"] >= args.selected_paths:
                continue
            shortest = path_irv(record["shortest"], float(record["duration_s"]), mapper)
            alternative = path_irv(record["alternative"], float(record["duration_s"]), mapper)
            delta = alternative - shortest
            total_delta += delta
            gain = float(delta[target])
            if gain > 0:
                donors += np.maximum(-delta, 0)
                gain_routes.append((gain, record))

    gain_routes.sort(key=lambda item: item[0], reverse=True)
    selected_routes = gain_routes[: args.top_routes]
    alternative_segments = []
    shortest_segments = []
    for _, record in selected_routes:
        duration = float(record["duration_s"])
        alternative_segments.extend(target_segments(record["alternative"], target, mapper, duration))
        shortest_segments.extend(target_segments(record["shortest"], target, mapper, duration))

    def to_frame(segments):
        return gpd.GeoDataFrame(
            {"irv": [weight for _, weight in segments]},
            geometry=[geometry for geometry, _ in segments],
            crs="EPSG:4326",
        )

    alternative_frame = to_frame(alternative_segments)
    shortest_frame = to_frame(shortest_segments)
    # WGS 84 / UTM zone 16N avoids a missing local NAD83 grid transform in
    # the bundled PROJ installation while keeping metre-based nearest joins.
    roads = gpd.read_file(args.roads).to_crs("EPSG:32616")

    def road_use(frame, prefix: str):
        matched = gpd.sjoin_nearest(
            frame.to_crs("EPSG:32616"),
            roads[["name", "highway", "geometry"]],
            how="left",
            max_distance=35,
            distance_col="distance_m",
        )
        return (
            matched.dropna(subset=["name"])
            .groupby(["name", "highway"], as_index=False)
            .agg(**{f"{prefix}_irv": ("irv", "sum"), f"{prefix}_segments": ("irv", "size")})
        )

    alternative_use = road_use(alternative_frame, "alternative")
    shortest_use = road_use(shortest_frame, "shortest")
    road_gains = alternative_use.merge(shortest_use, on=["name", "highway"], how="outer").fillna(0)
    road_gains["delta_irv"] = road_gains["alternative_irv"] - road_gains["shortest_irv"]
    road_gains = road_gains.sort_values("delta_irv", ascending=False)
    road_gains.to_csv(args.output_dir / "near_west_side_major_road_use.csv", index=False)

    summary_rows = []
    for region, value in sorted(enumerate(donors), key=lambda item: item[1], reverse=True)[:8]:
        summary_rows.append({
            "kind": "donor_region",
            "name": names[region],
            "region_id": region + 1,
            "irv_units": value,
        })
    for _, row in road_gains.head(8).iterrows():
        summary_rows.append({
            "kind": "alternative_road_in_near_west_side",
            "name": row["name"],
            "region_id": args.target_region,
            "irv_units": row["delta_irv"],
        })
    with (args.output_dir / "near_west_side_network_summary.csv").open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["kind", "name", "region_id", "irv_units"])
        writer.writeheader()
        writer.writerows(summary_rows)

    target_boundary = boundaries[boundaries["region"] == target]
    figure, axis = plt.subplots(figsize=(9, 8))
    boundaries.boundary.plot(ax=axis, color="#a0a0a0", linewidth=0.35)
    roads_wgs = roads.to_crs("EPSG:4326")
    roads_wgs.plot(ax=axis, color="#b6b6b6", linewidth=0.35, alpha=0.7)
    target_boundary.plot(ax=axis, color="#f1c8be", edgecolor="#9c5346", linewidth=1.0, alpha=0.65)
    if not shortest_frame.empty:
        shortest_frame.plot(ax=axis, color="#5083a2", linewidth=0.65, alpha=0.28, label="CH shortest")
    if not alternative_frame.empty:
        alternative_frame.plot(ax=axis, color="#c65e48", linewidth=0.8, alpha=0.35, label="Strict second-best")
    west, south, east, north = target_boundary.total_bounds
    axis.set_xlim(west - 0.025, east + 0.025)
    axis.set_ylim(south - 0.022, north + 0.022)
    axis.set_aspect("equal")
    axis.set_title(f"{names[target]}: top {len(selected_routes)} routes adding IRV at 20% replacement")
    axis.set_xlabel("Longitude")
    axis.set_ylabel("Latitude")
    axis.legend(loc="upper left")
    for _, road in road_gains.head(4).iterrows():
        candidates = roads_wgs[roads_wgs["name"] == road["name"]]
        if candidates.empty:
            continue
        line = max(candidates.geometry, key=lambda geometry: geometry.length)
        point = line.interpolate(0.5, normalized=True)
        axis.annotate(road["name"], (point.x, point.y), xytext=(3, 3), textcoords="offset points", fontsize=8)
    figure.savefig(args.output_dir / "near_west_side_reroute_corridors.png", dpi=220, bbox_inches="tight")
    plt.close(figure)

    print(f"Target: {names[target]} (region {args.target_region}), delta={total_delta[target]:+.3f}")
    print(f"Positive-gain routes: {len(gain_routes)}, plotted: {len(selected_routes)}")
    print("Top donor regions:")
    for region, value in sorted(enumerate(donors), key=lambda item: item[1], reverse=True)[:5]:
        print(f"  {names[region]}: {value:.3f}")
    print("Top nearby named roads used by reroutes:")
    for _, row in road_gains.head(5).iterrows():
        print(f"  {row['name']}: {row['delta_irv']:+.3f}")


if __name__ == "__main__":
    main()
