package util;

import java.awt.Graphics2D;
import java.awt.Image;
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

	public static BufferedImage scaleImage(BufferedImage image, int maxWidth, int maxHeight) {
		// Calculate the scaling factors to fit within the maximum width and height
		double widthScaleFactor = (double) maxWidth / image.getWidth();
		double heightScaleFactor = (double) maxHeight / image.getHeight();

		// Choose the smaller of the two scale factors to maintain aspect ratio
		double scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);

		// If already within max width and height
		if (scaleFactor >= 1.0) {
			return image;
		}

		// Create a new BufferedImage for the thumbnail
		BufferedImage scaled = new BufferedImage((int) (image.getWidth() * scaleFactor),
				(int) (image.getHeight() * scaleFactor), BufferedImage.TYPE_INT_ARGB);

		// Apply the transformation
		AffineTransform transform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
		Graphics2D g2d = scaled.createGraphics();
		g2d.drawImage(image, transform, null);
		g2d.dispose();

		return scaled;
	}

	public static BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
		if (originalImage.getWidth() <= maxWidth && originalImage.getHeight() <= maxHeight) {
			return originalImage;
		}

		double widthRatio = (double) maxWidth / originalImage.getWidth();
		double heightRatio = (double) maxHeight / originalImage.getHeight();

		double scaleFactor = Math.min(widthRatio, heightRatio);

		int newWidth = (int) (originalImage.getWidth() * scaleFactor);
		int newHeight = (int) (originalImage.getHeight() * scaleFactor);

		Image resultingImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT);

		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = resizedImage.createGraphics();
		g2d.drawImage(resultingImage, 0, 0, null);
		g2d.dispose();

		return resizedImage;
	}

}
