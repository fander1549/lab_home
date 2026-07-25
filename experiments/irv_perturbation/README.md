# 全日严格次优路径的 IRV 扰动实验

本实验分别统计 NYC 和 Chicago 一个自然日的**全部有效路径**。CH 最短路径构成
基线；固定随机种子产生嵌套的 5%、10%、15% 和 20% 替换集合。

替换请求使用 GraphHopper `ALT_ROUTE` 的 flexible mode，从与 CH 最短路不同的候选
中选择耗时最小、且距离绕行不超过 15% 的路径。绕行上限可通过 JVM 参数调整，例如
`-Dirv.max_detour_ratio=1.10`。

IRV 采用匀速假设：

`IRV(r) = Σ duration × segment_length / path_length / 1800`。

## 已上传的数据与依赖

- 可直接复核的逐路径结果、区域统计、汇总表和图片位于 `artifacts/results/`。
- NYC 的 `NYC_862.txt` 和 Chicago Community Areas GeoJSON 位于
  `artifacts/boundaries/`。
- 若要从原始行程重新生成路径，仍需 NYC 路径源文件和 Chicago trip 文件。
- `NewYork2.osm.pbf`、`Chicago.osm.pbf`。
- Python：`numpy`、`matplotlib`、`geopandas`、`shapely`。
- Chicago 路网解释可选依赖：GDAL/OGR。

仓库中的边界文件可以直接传给绘图脚本，不依赖个人目录。

## 1. 生成全日 CH 与严格次优路径

```bash
mvn -pl example -am -DskipTests compile

# NYC：2014-02-01
java -Dirv.max_detour_ratio=1.15 -cp <project-classpath> \
  com.graphhopper.example.IrvFullDayRouteGenerator \
  nyc taxi_route/2014_green_02.csv_route.txt - \
  outputs/irv_perturbation/strict_second_best/nyc_routes.jsonl \
  2014-02-01 20260720

# Chicago：先提取 2019-01-01
python3 experiments/irv_perturbation/extract_day_records.py \
  taxi_route/taxi_trip1-12flow_data.txt \
  outputs/irv_perturbation/chicago_2019-01-01.csv \
  --date 01/01/2019

java -Dirv.max_detour_ratio=1.15 -cp <project-classpath> \
  com.graphhopper.example.IrvFullDayRouteGenerator \
  chicago - outputs/irv_perturbation/chicago_2019-01-01.csv \
  outputs/irv_perturbation/strict_second_best/chicago_routes.jsonl \
  2019-01-01 20260720
```

长任务可使用生成器末尾的可选参数
`start-index batch-size rank-start alternative-quota` 分块运行。若每块保留全部符合
约束的候选，再用以下脚本按全日随机顺序连续编号，确保 5%–20% 是嵌套集合：

```bash
python3 experiments/irv_perturbation/rerank_constrained_alternatives.py \
  --routes-dir outputs/irv_perturbation/strict_second_best/nyc_chunks \
  --output outputs/irv_perturbation/strict_second_best/nyc_routes.jsonl \
  --selected-paths 7829
```

## 2. 区域映射和热力图

NYC 映射移植自 `data_processing_c#/Tool.cs`：2400×2400 Web-Mercator 网格、
3×3 像素查询、边界点四方向回退及多区域等时长分配。默认再扩展 1 个像素，形成
5×5 边缘冗余；`--nyc-boundary-buffer-px 0` 可恢复严格的 C# 3×3 查询。

```bash
# NYC
python3 experiments/irv_perturbation/render_full_day_irv_perturbation.py \
  --city nyc \
  --nyc-routes experiments/irv_perturbation/artifacts/results/routes/nyc_2014-02-01.jsonl \
  --nyc-grid experiments/irv_perturbation/artifacts/boundaries/NYC_862.txt \
  --output-dir outputs/irv_perturbation/strict_second_best/final

# Chicago
python3 experiments/irv_perturbation/render_full_day_irv_perturbation.py \
  --city chicago \
  --chicago-routes experiments/irv_perturbation/artifacts/results/routes/chicago_2019-01-01.jsonl \
  --chicago-boundary experiments/irv_perturbation/artifacts/boundaries/chicago_community_areas.geojson \
  --output-dir outputs/irv_perturbation/strict_second_best/final
```

输出包括四个替换比例的区域 CSV、城市热力图和汇总 CSV。

## 3. Chicago 红色集中区的路网解释

先从本地 Chicago OSM PBF 提取 Near West Side 周边主干道路：

```bash
ogr2ogr -f GeoJSON /tmp/chicago-major-roads.geojson Chicago.osm.pbf lines \
  -spat -87.76 41.81 -87.56 41.98 \
  -where "highway IN ('motorway','trunk','primary','secondary','tertiary')"
```

再分析 20% 替换时为区域 28（Near West Side）增加 IRV 的路径：

```bash
python3 experiments/irv_perturbation/analyze_chicago_red_corridor.py \
  --routes experiments/irv_perturbation/artifacts/results/routes/chicago_2019-01-01.jsonl \
  --boundaries experiments/irv_perturbation/artifacts/boundaries/chicago_community_areas.geojson \
  --roads /tmp/chicago-major-roads.geojson \
  --output-dir outputs/irv_perturbation/strict_second_best/final \
  --selected-paths 4972 \
  --target-region 28
```

该分析输出目标区域的重路由走廊图、主干道路增量统计和来源区域统计。重新运行产生的
临时文件仍写入不会提交的 `outputs/irv_perturbation/`；本次实验的固定结果快照和边界
数据已单独整理到 `artifacts/` 并提交。
