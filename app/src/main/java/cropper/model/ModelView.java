package cropper.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface ModelView {

	void imageWasLoaded(BufferedImage image, String filename, String id);

	void imagesFailedToLoad(List<File> failed);

	void imageWasUpdated(String id, BufferedImage image, boolean hasUnsavedChanges);

	void imageWasRemoved(String id);

	void reportProgress(String string);

}
