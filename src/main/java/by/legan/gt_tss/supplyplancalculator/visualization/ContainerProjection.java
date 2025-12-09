package by.legan.gt_tss.supplyplancalculator.visualization;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;

import java.util.List;

/**
 * Проецирует данные контейнеров в формат визуализации.
 * Преобразует объекты Container в объекты ContainerVisualization для 3D рендеринга.
 */
public class ContainerProjection extends AbstractProjection<Container> {

	/**
	 * Проецирует список контейнеров в визуализацию упаковки.
	 * Создает объекты визуализации для контейнеров, стеков, уровней и коробок.
	 *
	 * @param inputContainers список контейнеров для визуализации
	 * @return PackagingVisualization содержащий все визуализации контейнеров
	 */
	public PackagingVisualization project(List<Container> inputContainers) {
		int step = 0;
		PackagingVisualization visualization = new PackagingVisualization();
		for(Container inputContainer : inputContainers) {
			ContainerVisualization containerVisualization = new ContainerVisualization();
			containerVisualization.setStep(step++);
			
			// В версии 4.x Container использует getDx(), getDy(), getDz() вместо getWidth(), getDepth(), getHeight()
			containerVisualization.setDx(inputContainer.getDx());
			containerVisualization.setDy(inputContainer.getDy());
			containerVisualization.setDz(inputContainer.getDz());

			containerVisualization.setLoadDx(inputContainer.getDx());
			containerVisualization.setLoadDy(inputContainer.getDy());
			containerVisualization.setLoadDz(inputContainer.getDz());

			// В версии 4.x Container использует getDescription() вместо getName()
			String containerName = inputContainer.getDescription();
			containerVisualization.setId(containerName);
			containerVisualization.setName(containerName);

			StackVisualization stackVisualization = new StackVisualization();
			stackVisualization.setStep(step++);
			containerVisualization.setStack(stackVisualization);
			
			// В версии 4.x Container использует getStack() вместо getLevels()
			Stack stack = inputContainer.getStack();
			for (Placement placement : stack) {
					// В версии 4.x Placement использует getBoxItem() вместо getBox()
					BoxItem boxItem = placement.getBoxItem();
					Box box = boxItem.getBox();
					BoxVisualization boxVisualization = new BoxVisualization();
					// В версии 4.x Box использует getId() и getDescription()
					String boxName = box.getId() != null ? box.getId() : box.getDescription();
					boxVisualization.setId(boxName);
					boxVisualization.setName(boxName);
					boxVisualization.setStep(step);

					// В версии 4.x размеры Box хранятся в BoxStackValue
					BoxStackValue stackValue = box.getStackValue(0);
					boxVisualization.setDx(stackValue.getDx());
					boxVisualization.setDy(stackValue.getDy());
					boxVisualization.setDz(stackValue.getDz());
					
					StackPlacementVisualization stackPlacement = new StackPlacementVisualization();

					stackPlacement.setX(placement.getAbsoluteX()+23);
					stackPlacement.setY(placement.getAbsoluteY()-4840);
					stackPlacement.setZ(placement.getAbsoluteZ()+4817);

/*
					stackPlacement.setX(placement.getAbsoluteX());
					stackPlacement.setY(placement.getAbsoluteY());
					stackPlacement.setZ(placement.getAbsoluteZ());
*/
					stackPlacement.setStackable(boxVisualization);
					stackPlacement.setStep(step);

					stackVisualization.add(stackPlacement);
					
					step++;
			}
			
			visualization.add(containerVisualization);
		}
		return visualization;
	}
}
