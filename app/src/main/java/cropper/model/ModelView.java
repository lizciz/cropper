package cropper.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface ModelView {

	void imageWasLoaded(BufferedImage image, String filename, int index);

	void imagesFailedToLoad(List<File> failed);

	void displayImage(int index, BufferedImage image);

	void refreshThumbnail(int index, BufferedImage image);

}
