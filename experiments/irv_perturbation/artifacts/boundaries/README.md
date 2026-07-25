# 地图边界数据

- `NYC_862.txt`：来自用户提供的 `data_processing_c#` 工程。它是
  2400×2400 的区域编号网格，共 862 个区域；映射范围为北纬
  40.486–40.918、西经 74.259–73.700，并使用 Web-Mercator 的纵向换算。
- `chicago_community_areas.geojson`：Chicago 77 个 Community Areas 的
  GeoJSON 边界。绘图时用 `area_num_1 - 1` 作为内部区域编号。

这两份文件是生成已上传热力图所使用的原始边界快照。
