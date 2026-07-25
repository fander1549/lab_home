# IRV 实验数据快照

该目录保存论文可视化所对应的固定数据快照：

- `boundaries/`：绘制和区域映射所需的 NYC 网格及 Chicago 行政区边界。
- `results/routes/`：全日 CH 最短路径及受限严格次优路径，JSON Lines 格式。
- `results/final/`：5%、10%、15%、20% 替换比例的区域 IRV 差值、汇总表和最终图片。

NYC 使用 2014-02-01 的 39,144 条有效路径；Chicago 使用 2019-01-01 的
24,861 条有效路径。候选次优路径与最短路不同，且距离绕行不超过 15%。

文件完整性：

| 文件 | SHA-256 |
| --- | --- |
| `results/routes/nyc_2014-02-01.jsonl` | `0dfd2daa32d906b489eb413c193e8f5ca9564c21dacd9ccf6e421169bf53af03` |
| `results/routes/chicago_2019-01-01.jsonl` | `9ca7a774a594c45458796b44a98884c9dfc3a440f66f6d7c46de4bdc0e9c4dfe` |
| `boundaries/NYC_862.txt` | `ed4d5e3bee6b7336895e0bf0598ebf60c81a58778897490ec49453ff94f2ba2c` |
| `boundaries/chicago_community_areas.geojson` | `bb096e30de7d59866613223c9ee5a3c9337a1a4039a0a7e0a398fb5d0881d858` |
