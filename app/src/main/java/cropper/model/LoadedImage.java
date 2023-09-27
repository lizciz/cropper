package cropper.model;

import java.awt.image.BufferedImage;
import java.io.File;

import util.Utils;

public class LoadedImage {

	private static int ID_COUNTER = 1;

	private final String id;
	private final File file;
	private final BufferedImage originalImage;
	private BufferedImage image;

	private final String filename;

	public LoadedImage(File file, BufferedImage originalImage) {
		this.id = Integer.toString(ID_COUNTER++);
		this.file = file;
		this.originalImage = originalImage;
		this.image = originalImage;

		this.filename = Utils.rmFileExt(file.getName());
	}

	public void undoChanges() {
		image = originalImage;
	}

	public void updateImage(BufferedImage newImage) {
		this.image = newImage;
	}

	public String getId() {
		return id;
	}

	public File getFile() {
		return file;
	}

	public BufferedImage getOriginalImage() {
		return originalImage;
	}

	public BufferedImage getImage() {
		return image;
	}

	public String getFilename() {
		return filename;
	}

}
