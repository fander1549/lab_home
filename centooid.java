import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;

public class centooid {
    public static void main(String[] args) {
        // 指定 Shapefile 文件的路径
        File shpFile = new File("path/to/your/shapefile.shp");

        try {
            // 读取 Shapefile 文件
            FileDataStore store = FileDataStoreFinder.getDataStore(shpFile);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            FeatureIterator<SimpleFeature> features = featureSource.getFeatures().features();

            // 遍历每个几何对象
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                // 计算中心点
                Point centroid = geometry.getCentroid();

                // 输出中心点的坐标
                System.out.println("Centroid: " + centroid.getX() + ", " + centroid.getY());
            }

            // 关闭文件数据存储
            store.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
