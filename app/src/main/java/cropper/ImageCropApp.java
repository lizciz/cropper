package cropper;

import java.io.IOException;

import javax.swing.UnsupportedLookAndFeelException;

import cropper.model.DefaultModel;
import cropper.model.ModelAPI;
import cropper.view.MainView;

public class ImageCropApp {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		CropperProps props = new CropperProps();
		ModelAPI model = new DefaultModel();
		new MainView(model, props);
	}
}
