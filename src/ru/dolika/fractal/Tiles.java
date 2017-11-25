package ru.dolika.fractal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

public class Tiles {

	private static Tiles _instance = null;

	public static Tiles getInstance() {
		if (_instance == null) {
			synchronized (Tiles.class) {
				if (_instance == null) {
					_instance = new Tiles();
				}
			}
		}
		return _instance;
	}

	private Tiles() {
		getEmptyTile();
	}

	public static final int tileDim = 256;

	public double getZeroTileCoord(double realCoord, double zoom) {
		return getTileNum(realCoord, zoom) * tileDim;
	}

	public long getTileNum(double realCoord, double zoom) {
		return MathFloor(realCoord / tileDim * zoom);
	}

	public BufferedImage getTile(long numX, long numY, double zoom) {
		BufferedImage img = getTileImage(numX, numY, zoom);
		return img;
	}

	private BufferedImage getTileImage(final long numX, final long numY,
			final double zoom) {
		Runnable generator = new Runnable() {
			public void run() {
				generateAndPutTile(numX, numY, zoom);
			}
		};
		String outputFileName = tileToFile(numX, numY, zoom);
		File need = new File(outputFileName);
		if (!Files.exists(need.toPath())) {
			Path directory = new File(tileToDirectory(zoom)).toPath();
			if (!Files.exists(directory)) {
				try {
					Files.createDirectories(directory, new FileAttribute[0]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				ImageIO.write(getEmptyTile(), "PNG", need);
			} catch (Exception e) {
				// not critical
			}
			Thread thread = new Thread(generator);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setName("Generator Z:" + zoom + ";X:" + numX + ";Y:" + numY);
			thread.start();
			return getEmptyTile();
		}
		BufferedImage img = null;

		try {
			img = ImageIO.read(need);
		} catch (Exception e) {
			return getEmptyTile();
		}
		return img;
	}

	Semaphore generatorSemaphore = new Semaphore(Runtime.getRuntime()
			.availableProcessors() * 2, false);

	private void generateAndPutTile(long numX, long numY, double zoom) {
		try {
			generatorSemaphore.acquire();
			try {
				BufferedImage img = generateTile(numX, numY, zoom);

				String outputFileName = tileToFile(numX, numY, zoom);

				try {

					ImageIO.write(img, "PNG", new File(outputFileName));
				} catch (IOException e) {
					try {
						Files.delete(new File(outputFileName).toPath());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			} finally {
				generatorSemaphore.release();
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private String tileToFile(long numX, long numY, double zoom) {
		return tileToDirectory(zoom) + "tile-" + numX + "-" + numY + ".png";
	}

	private String tileToDirectory(double zoom) {
		return ".fractal/cache/" + zoom + "/";
	}

	private BufferedImage generateTile(long numX, long numY, double zoom) {
		BufferedImage img = new BufferedImage(tileDim, tileDim,
				BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < tileDim; x++) {
			for (int y = 0; y < tileDim; y++) {

				double cr = (numX * tileDim + x) / zoom;
				double ci = (numY * tileDim + y) / zoom;

				double Zr = 0;
				double Zi = 0;
				int i = 0;

				double Zrnew, Zinew;

				while (i < Fractal.MAX_SEARCH && ((Zr * Zr) + (Zi * Zi)) < 4) {
					Zrnew = (Zr * Zr) - (Zi * Zi);
					Zinew = 2.0 * (Zr * Zi);
					Zr = Zrnew + cr;
					Zi = Zinew + ci;
					i++;
				}

				if (((Zr * Zr) + (Zi * Zi)) >= 4) {
					img.setRGB(x, y, Fractal.jToRGBint(i));
				} else {
					img.setRGB(x, y, Color.black.getRGB());
				}
			}
		}
		return img;
	}

	private BufferedImage _emptyTile = null;

	private BufferedImage getEmptyTile() {
		if (_emptyTile == null) {
			_emptyTile = new BufferedImage(tileDim, tileDim,
					BufferedImage.TYPE_INT_RGB);
			Graphics g = _emptyTile.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, tileDim, tileDim);
			g.setColor(Color.BLACK);
			g.drawLine(_emptyTile.getWidth() / 4, _emptyTile.getWidth() / 4,
					_emptyTile.getWidth() * 3 / 4,
					_emptyTile.getWidth() * 3 / 4);
			g.drawLine(_emptyTile.getWidth() / 4,
					_emptyTile.getWidth() * 3 / 4,
					_emptyTile.getWidth() * 3 / 4, _emptyTile.getWidth() / 4);
		}
		return _emptyTile;
	}

	public static long MathFloor(double value) {
		if (value >= 0) {
			return ((long) value);
		} else {
			if ((long) value == value) {
				return ((long) value);
			} else {
				return ((long) (value - 1));
			}
		}
	}

}
