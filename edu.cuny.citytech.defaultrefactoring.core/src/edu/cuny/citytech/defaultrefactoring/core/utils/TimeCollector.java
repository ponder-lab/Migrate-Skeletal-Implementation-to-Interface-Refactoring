/**
 *
 */
package edu.cuny.citytech.defaultrefactoring.core.utils;

/**
 * @author raffi
 *
 */
public class TimeCollector {

	private long collectedTime;
	private long start;

	public void clear() {
		collectedTime = 0;
	}

	public long getCollectedTime() {
		return collectedTime;
	}

	public void start() {
		start = System.currentTimeMillis();
	}

	public void stop() {
		final long elapsed = System.currentTimeMillis() - start;
		collectedTime += elapsed;
	}
}
