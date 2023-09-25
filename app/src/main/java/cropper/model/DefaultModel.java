package cropper.model;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import util.Tuple;
import util.Utils;

public class DefaultModel implements ModelAPI {

	private ModelView view;

	private ArrayList<File> files;
	private ArrayList<BufferedImage> originalImages;
	private ArrayList<BufferedImage> images;
	private int selectedIndex;

	public DefaultModel() {
		this.files = new ArrayList<>();
		this.originalImages = new ArrayList<>();
		this.images = new ArrayList<>();
		this.selectedIndex = -1;
	}

	@Override
	public void setView(ModelView view) {
		this.view = view;
	}

	@Override
	public void openImageDirectory(File dir) {

		List<Tuple<BufferedImage, String>> thumbnails = new ArrayList<>();
		List<File> failedToLoad = new ArrayList<>();
		for (File f : dir.listFiles()) {
			if (f.getName().toLowerCase().endsWith(".jpg")) {
				try {
					BufferedImage image = ImageIO.read(f);
					originalImages.add(image);
					images.add(image);
					files.add(f);
					thumbnails.add(new Tuple<>(image, Utils.rmFileExt(f.getName())));
				} catch (IOException e) {
					failedToLoad.add(f);
				}
			}
		}
		Collections.sort(thumbnails, Comparator.comparing(Tuple::v2));
		Collections.sort(failedToLoad);

		view.imagesWereLoaded(thumbnails, failedToLoad);

	}

	@Override
	public void selectImage(int index) {
		this.selectedIndex = index;
		view.displayImage(index, images.get(index));
	}

	@Override
	public void selectPreviousImage() {
		if (selectedIndex > 0)
			selectImage(selectedIndex - 1);
	}

	@Override
	public void selectNextImage() {
		if (selectedIndex < images.size() - 1)
			selectImage(selectedIndex + 1);
	}

	@Override
	public void deleteSelectedImage() {
	}

	@Override
	public void undoSelectedImageChanges() {
	}

	@Override
	public void redoSelectedImageChanges() {
	}

	@Override
	public void performCrop(Rectangle cropRegion) {
		BufferedImage originalImage = images.get(selectedIndex);
		// Ensure that the crop region is within the bounds of the original image
		cropRegion = cropRegion.intersection(new Rectangle(0, 0, originalImage.getWidth(), originalImage.getHeight()));

		// Perform the actual cropping
		BufferedImage croppedImage = originalImage.getSubimage(
				cropRegion.x, cropRegion.y, cropRegion.width, cropRegion.height);

		images.set(selectedIndex, croppedImage);

		view.displayImage(0, croppedImage);

	}

	@Override
	public void save() {
		// TODO Auto-generated method stub

	}

}
