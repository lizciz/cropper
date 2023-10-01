package cropper.view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cropper.CropperProps;

@SuppressWarnings("serial")
class ImagePanel extends JPanel {

	private Point startPoint = new Point();
	private Point endPoint = new Point();

	private Point anchorPoint = new Point();
	private Point startAnchor = new Point();
	private Point endAnchor = new Point();
	private Rectangle imgBox;
	private boolean cropInProgress;
	private boolean activeCrop;

	private BufferedImage image;

	private int zoom;
	private Observer<Integer> zoomLabel;
	private CropperProps props;

	private enum DragPoint {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ALL, NONE
	}

	private DragPoint dragPoint = DragPoint.NONE;

	public ImagePanel(final CropAction cropAction, final Observer<Integer> zoomLabel, final CropperProps props) {
		this.image = null;
		this.zoomLabel = zoomLabel;
		this.props = props;

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() != 1) {
					cropInProgress = activeCrop = false;
					imgBox = null;
					dragPoint = DragPoint.NONE;
					repaint();
					return;
				}

				if (activeCrop) {

					cropInProgress = false;

					int x = e.getPoint().x;
					int y = e.getPoint().y;

					int handleSize = 8;
					Rectangle selection = makeRect(x, y, handleSize);

					int startX = Math.min(startPoint.x, endPoint.x);
					int startY = Math.min(startPoint.y, endPoint.y);
					int width = Math.abs(endPoint.x - startPoint.x);
					int height = Math.abs(endPoint.y - startPoint.y);

					Rectangle cropArea = new Rectangle(startX, startY, width, height);
					if (e.getClickCount() == 2 && selection.intersects(cropArea)) {

						int actualWidth = image.getWidth();

						int relx = cropArea.x - imgBox.x;
						int rely = cropArea.y - imgBox.y;

						double f = (double) actualWidth / (double) imgBox.width;

						cropAction.crop(new Rectangle((int) (relx * f), (int) (rely * f), (int) (cropArea.width * f),
								(int) (cropArea.height * f)));
						return;
					}

					if (selection.intersects(makeRect(startX, startY, handleSize))) {
						dragPoint = DragPoint.TOP_LEFT;
						startPoint.setLocation(startX + width, startY + height);
						endPoint.setLocation(startX, startY);

					} else if (selection.intersects(makeRect(startX + width, startY, handleSize))) {
						dragPoint = DragPoint.TOP_RIGHT;
						startPoint.setLocation(startX, startY + height);
						endPoint.setLocation(startX + width, startY);

					} else if (selection.intersects(makeRect(startX, startY + height, handleSize))) {
						dragPoint = DragPoint.BOTTOM_LEFT;
						startPoint.setLocation(startX + width, startY);
						endPoint.setLocation(startX, startY + height);

					} else if (selection.intersects(makeRect(startX + width, startY + height, handleSize))) {
						dragPoint = DragPoint.BOTTOM_RIGHT;
						startPoint.setLocation(startX, startY);
						endPoint.setLocation(startX + width, startY + height);

					} else if (selection.intersects(cropArea)) {
						dragPoint = DragPoint.ALL;
						anchorPoint.setLocation(e.getPoint());
						startAnchor.setLocation(startPoint);
						endAnchor.setLocation(endPoint);

					} else {
						dragPoint = DragPoint.NONE;
					}
				} else {
					startPoint.setLocation(e.getPoint());
					endPoint.setLocation(e.getPoint());
					cropInProgress = true;
				}
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {

				dragPoint = DragPoint.NONE;
				if (cropInProgress) {
					activeCrop = true;
					cropInProgress = false;
				}

				repaint();
			}
		});

		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (cropInProgress || activeCrop && dragPoint != DragPoint.NONE) {
					if (dragPoint == DragPoint.ALL) {
						int dx = e.getPoint().x - anchorPoint.x;
						int dy = e.getPoint().y - anchorPoint.y;

						int startX = Math.min(startAnchor.x, endAnchor.x);
						int startY = Math.min(startAnchor.y, endAnchor.y);
						int width = Math.abs(endAnchor.x - startAnchor.x);
						int height = Math.abs(endAnchor.y - startAnchor.y);

						if (startX + dx < imgBox.x)
							dx = imgBox.x - startX;
						if (startX + width + dx > imgBox.x + imgBox.width)
							dx = imgBox.x + imgBox.width - (startX + width);
						if (startY + dy < imgBox.y)
							dy = imgBox.y - startY;
						if (startY + height + dy > imgBox.y + imgBox.height)
							dy = imgBox.y + imgBox.height - (startY + height);

						startPoint.setLocation(startAnchor.x + dx, startAnchor.y + dy);
						endPoint.setLocation(endAnchor.x + dx, endAnchor.y + dy);

					} else {
						endPoint = getPointInsideBounds(e.getPoint(), imgBox);
					}
				}
				repaint();
			}
		});
	}

	public void setImage(BufferedImage newImage) {
		this.image = newImage;
		activeCrop = cropInProgress = false;
		imgBox = null;
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			int panelWidth = getWidth();
			int panelHeight = getHeight();
			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();

			// Calculate the scaled dimensions while maintaining aspect ratio
			int newWidth, newHeight;
			double widthRatio = (double) panelWidth / imageWidth;
			double heightRatio = (double) panelHeight / imageHeight;
			if (props.isMaxZoom100()) {
				double min = Math.min(widthRatio, heightRatio);
				if (min > 1.0D) {
					widthRatio /= min;
					heightRatio /= min;
				}
			}

			int r;
			if (widthRatio < heightRatio) {
				newWidth = (int) (imageWidth * widthRatio);
				newHeight = (int) (imageHeight * widthRatio);
				r = (int) Math.round(widthRatio * 100);
			} else {
				newWidth = (int) (imageWidth * heightRatio);
				newHeight = (int) (imageHeight * heightRatio);
				r = (int) Math.round(heightRatio * 100);
			}
			if (r != zoom) {
				updateZoomLabel(r);
			}

			// Calculate the position to center the image
			int x = (panelWidth - newWidth) / 2;
			int y = (panelHeight - newHeight) / 2;

			// Draw the scaled image centered within the panel
			g.drawImage(image, x, y, newWidth, newHeight, null);
			imgBox = new Rectangle(x, y, newWidth, newHeight);

			if (cropInProgress || activeCrop) {

				int startX = Math.min(startPoint.x, endPoint.x);
				int startY = Math.min(startPoint.y, endPoint.y);
				int width = Math.abs(endPoint.x - startPoint.x);
				int height = Math.abs(endPoint.y - startPoint.y);

				// Create a rectangle representing the selection
				Rectangle selection = new Rectangle(startX, startY, width, height);

				// Create a shape representing the entire panel minus the selection
				Area area = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
				area.subtract(new Area(selection));

				// Create a semi-transparent overlay
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				g2d.setColor(Color.BLACK);
				g2d.fill(area);
				g2d.dispose();

				// Draw handles for the corners
				int handleSize = 8;
				g.setColor(Color.BLUE);
				g.fillRect(startX - handleSize / 2, startY - handleSize / 2, handleSize, handleSize);
				g.fillRect(startX + width - handleSize / 2, startY - handleSize / 2, handleSize, handleSize);
				g.fillRect(startX - handleSize / 2, startY + height - handleSize / 2, handleSize, handleSize);
				g.fillRect(startX + width - handleSize / 2, startY + height - handleSize / 2, handleSize, handleSize);
			}

		}
	}

	private void updateZoomLabel(final int r) {
		zoom = r;
		SwingUtilities.invokeLater(() -> zoomLabel.valueChanged(r));
	}

	private static Rectangle makeRect(int x, int y, int size) {
		return new Rectangle(x - size / 2, y - size / 2, size, size);
	}

	public static Point getPointInsideBounds(Point point, Rectangle bounds) {
		if (bounds == null || bounds.contains(point)) {
			return point; // Point is already inside the bounds
			// (or null, if render is not complete, so we don't know bounds yet)
		}
		int newX = Math.max(bounds.x, Math.min(bounds.x + bounds.width, point.x));
		int newY = Math.max(bounds.y, Math.min(bounds.y + bounds.height, point.y));
		return new Point(newX, newY);
	}

}
