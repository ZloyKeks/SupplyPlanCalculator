package by.legan.gt_tss.supplyplancalculator;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;
import by.legan.gt_tss.supplyplancalculator.visualization.ContainerProjection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class VisualizationTest {

//	@Test
	public void testPackager() throws Exception {
		
		// issue 159
		List<Container> containers = new ArrayList<>();
		Container container = new Container("X", 100, 100, 100, 21000);
		containers.add(container);


		List<BoxItem> products = new ArrayList<>();
		for (int i=0; i<500; i++){
			Random random = new Random();
			products.add(new BoxItem(new Box(""+i, 20, 35, 20, 10)));
			products.add(new BoxItem(new Box(""+i, 10, 10, 10, 10)));
			products.add(new BoxItem(new Box("m"+i, 5, 5, 15, 10)));
		}

//		Packager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).withRotate2D().build();
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, true, true, true, 1);
		Container pack = packager.pack(products, Long.MAX_VALUE);
		
		ContainerProjection projection = new ContainerProjection();
		List<Container> containerList = Arrays.asList(pack);
//		List<Container> containerList = packager.packList(products,10, Long.MAX_VALUE);

		System.out.println(containerList);
		
		File file = new File("containers.json");
		projection.project(containerList , file);

		
	}
}
