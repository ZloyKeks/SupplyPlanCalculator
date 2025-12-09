package by.legan.gt_tss.supplyplancalculator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import by.legan.gt_tss.supplyplancalculator.visualization.ContainerProjection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VisualizationTest {

//	@Test
	public void testPackager() throws Exception {
		
		// issue 159 - API 4.0.0 uses builders
		Container container = Container.newBuilder()
				.withDescription("X")
				.withSize(100, 100, 100)
				.withEmptyWeight(1)
				.withMaxLoadWeight(21000)
				.build();
		
		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(container)
				.build();

		List<BoxItem> products = new ArrayList<>();
		for (int i=0; i<500; i++){
			products.add(new BoxItem(Box.newBuilder().withId(""+i).withSize(20, 35, 20).withWeight(10).withRotate3D().build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId(""+i).withSize(10, 10, 10).withWeight(10).withRotate3D().build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("m"+i).withSize(5, 5, 15).withWeight(10).withRotate3D().build(), 1));
		}

		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
				.newBuilder()
				.build();
		
		PackagerResult result = packager
				.newResultBuilder()
				.withContainerItems(containerItems)
				.withBoxItems(products)
				.build();
		
		if (!result.isSuccess()) {
			throw new RuntimeException("Packaging failed");
		}
		
		Container pack = result.get(0);
		
		ContainerProjection projection = new ContainerProjection();
		List<Container> containerList = new ArrayList<>();
		containerList.add(pack);

		System.out.println(containerList);
		
		File file = new File("containers.json");
		projection.project(containerList , file);

		
	}
}
