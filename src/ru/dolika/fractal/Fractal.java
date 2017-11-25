package ru.dolika.fractal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Fractal extends JPanel implements Runnable {

	private static final long serialVersionUID = -4151180503983659941L;

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Fractal");
		Fractal fractal = new Fractal();
		frame.setLayout(new BorderLayout());
		frame.setContentPane(fractal);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fractal.run();
	}

	public Fractal() {
		setFocusable(true);
		setPreferredSize(new Dimension(640, 480));
		imgBuf = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		MouseAdapter mouseAd = new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int rot = e.getWheelRotation();
				int x = e.getX();
				int y = e.getY();
				if (rot < 0) {
					zoomIn(x, y);
				} else if (rot > 0) {
					zoomOut(x, y);
				}
			}

			int oldX = -1;
			int oldY = -1;

			@Override
			public void mouseReleased(MouseEvent arg0) {

			}

			@Override
			public void mousePressed(MouseEvent e) {
				oldX = e.getX();
				oldY = e.getY();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int dX = e.getX() - oldX;
				int dY = e.getY() - oldY;
				oldX = e.getX();
				oldY = e.getY();
				x0 -= dX / fZoom;
				y0 -= dY / fZoom;
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println(e.getClickCount());
				if (e.getClickCount() == 2) {
					int x = e.getX();
					int y = e.getY();

					if (e.getButton() == MouseEvent.BUTTON1) {
						zoomIn(x, y);
					} else {
						zoomOut(x, y);
					}
				}
			}
		};
		addMouseListener(mouseAd);
		addMouseMotionListener(mouseAd);
		addMouseWheelListener(mouseAd);
		resetZoom();

	}

	protected void decColMult() {
		COLOR_MULT -= 1;
		if (COLOR_MULT <= 2) {
			COLOR_MULT = 2;
		}
	}

	protected void incColMult() {
		COLOR_MULT += 1;
	}

	private void resetZoom() {
		x0 = -2.0;
		y0 = -1.3;
		fZoom = 100.0;
	}

	private double zoomFactor = 2;

	private void zoomIn(int x, int y) {
		synchronized (x0) {
			synchronized (y0) {
				synchronized (fZoom) {
					x0 = x0 + (x / fZoom) * (1 - 1 / zoomFactor);
					y0 = y0 + (y / fZoom) * (1 - 1 / zoomFactor);
					fZoom = (fZoom * zoomFactor);
				}
			}
		}
	}

	private void zoomOut(int x, int y) {
		synchronized (x0) {
			synchronized (y0) {
				synchronized (fZoom) {
					x0 = x0 + (x / fZoom) * (1 - zoomFactor);
					y0 = y0 + (y / fZoom) * (1 - zoomFactor);
					fZoom = (fZoom / zoomFactor);
					if (fZoom == 0) {
						fZoom = 1.0;
					}
				}
			}
		}
	}

	Random r = new Random();

	public void run() {
		computeBuffer();
		repaint();
		long time = System.currentTimeMillis();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		while (isDisplayable()) {
			try {
				Thread.sleep(1000 / 50);
			} catch (Exception e) {
			}
			repaint();
			if (System.currentTimeMillis() - time > 1000 / 20) {
				time = System.currentTimeMillis();
				computeBuffer();
			}
		}
		System.out.println("Stop repaint");
	}

	BufferedImage imgBuf = null;

	Tiles tileManager = Tiles.getInstance();

	public void computeBuffer() {
		int width = getWidth();
		int height = getHeight();

		if (imgBuf == null
				|| !(imgBuf.getWidth() == width && imgBuf.getHeight() == height)) {
			BufferedImage bi = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_BGR);
			bi.createGraphics().drawImage(imgBuf, 0, 0, null);
			imgBuf = bi;
		}
		Graphics g = imgBuf.getGraphics();
		double x0, y0, fZoom;
		synchronized (this.x0) {
			synchronized (this.y0) {
				synchronized (this.fZoom) {
					x0 = this.x0;
					y0 = this.y0;
					fZoom = this.fZoom;

				}
			}
		}

		long tileNumX = tileManager.getTileNum(x0, fZoom);
		long tileY = tileManager.getTileNum(y0, fZoom);

		long dX = Tiles.MathFloor(x0 * fZoom
				- tileManager.getZeroTileCoord(x0, fZoom));
		long dY = Tiles.MathFloor(y0 * fZoom
				- tileManager.getZeroTileCoord(y0, fZoom));

		for (int x = 0; x < width + Tiles.tileDim; x += Tiles.tileDim) {
			long tileNumY = tileY;
			for (int y = 0; y < height + Tiles.tileDim; y += Tiles.tileDim) {
				g.drawImage(tileManager.getTile(tileNumX, tileNumY, fZoom),
						(int) (x - dX), (int) (y - dY), null);
				tileNumY++;
			}
			tileNumX++;

		}
		g.dispose();
	}

	@Override
	public void paintComponent(Graphics g) {
		g.drawImage(imgBuf, 0, 0, null);
		g.dispose();
	}

	public Double x0;
	public Double y0;
	public Double fZoom;

	public final static int MAX_SEARCH = 10000;
	private static double COLOR_MULT = 128;

	public static int jToRGBint(double j) {
		return Color.HSBtoRGB((float) ((j % COLOR_MULT) / COLOR_MULT), 1, 1);
	}
}