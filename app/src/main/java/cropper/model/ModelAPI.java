package cropper.model;

import java.awt.Rectangle;
import java.io.File;

public interface ModelAPI {

	void setView(ModelView view);

	void openImageDirectory(File dir);

	void selectImage(int index);

	void selectPreviousImage();

	void selectNextImage();

	void deleteSelectedImage();

	void undoSelectedImageChanges();

	void redoSelectedImageChanges();

	void performCrop(Rectangle area);

	void save();

}
