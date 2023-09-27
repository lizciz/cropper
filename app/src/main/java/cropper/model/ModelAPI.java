package cropper.model;

import java.awt.Rectangle;
import java.io.File;

public interface ModelAPI {

	void setView(ModelView view);

	void openImageDirectory(File dir);

	void trashImage(String id);

	void deleteImage(String id);

	void undoImageChanges(String id);

	void redoImageChanges(String id);

	void performCrop(String id, Rectangle area);

	void save();

}
