package util;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Utils {

	public static String rmFileExt(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex != -1) {
			return fileName.substring(0, lastDotIndex);
		}
		return fileName;
	}

	public static BufferedImage scaleImage(BufferedImage image, int height) {

		// Calculate the scaling factors to fit within the thumb nail size
		double scaleFactor = (double) height / image.getHeight();

		// Create a new BufferedImage for the thumb nail
		BufferedImage scaled = new BufferedImage((int) (image.getWidth() * scaleFactor),
				(int) (image.getHeight() * scaleFactor), BufferedImage.TYPE_INT_ARGB);

		// Apply the transformation
		AffineTransform transform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
		Graphics2D g2d = scaled.createGraphics();
		g2d.drawImage(image, transform, null);
		g2d.dispose();

		return scaled;
	}

}
