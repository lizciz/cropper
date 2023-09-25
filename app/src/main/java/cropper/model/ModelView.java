package cropper.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import util.Tuple;

public interface ModelView {

	void imagesWereLoaded(List<Tuple<BufferedImage, String>> loaded, List<File> failed);

	void displayImage(int index, BufferedImage image);

}
