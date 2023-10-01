package util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class Utils {

	public static String rmFileExt(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex != -1) {
			return fileName.substring(0, lastDotIndex);
		}
		return fileName;
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
