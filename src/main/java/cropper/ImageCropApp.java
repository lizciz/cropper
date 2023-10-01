package cropper;

import java.io.IOException;

import javax.swing.UnsupportedLookAndFeelException;

import cropper.model.DefaultModel;
import cropper.model.ModelAPI;
import cropper.view.MainView;
import util.Lang;

public class ImageCropApp {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		CropperProps props = new CropperProps();
		Lang lang = new Lang(props);
		ModelAPI model = new DefaultModel(lang);
		new MainView(model, props, lang);
	}
}
