package by.legan.gt_tss.supplyplancalculator.visualization;

/**
 * Базовый класс для визуализаций имеющих 3D размеры (dx, dy, dz).
 * Используется как для контейнеров, так и для коробок.
 */
public class StackableVisualization extends AbstractVisualization {

	private int dx;
	private int dy;
	private int dz;

	/**
	 * Получает размер в направлении X.
	 *
	 * @return размер X
	 */
	public int getDx() {
		return dx;
	}
	
	/**
	 * Устанавливает размер в направлении X.
	 *
	 * @param dx размер X
	 */
	public void setDx(int dx) {
		this.dx = dx;
	}
	
	/**
	 * Получает размер в направлении Y.
	 *
	 * @return размер Y
	 */
	public int getDy() {
		return dy;
	}
	
	/**
	 * Устанавливает размер в направлении Y.
	 *
	 * @param dy размер Y
	 */
	public void setDy(int dy) {
		this.dy = dy;
	}
	
	/**
	 * Получает размер в направлении Z.
	 *
	 * @return размер Z
	 */
	public int getDz() {
		return dz;
	}
	
	/**
	 * Устанавливает размер в направлении Z.
	 *
	 * @param dz размер Z
	 */
	public void setDz(int dz) {
		this.dz = dz;
	}

}
