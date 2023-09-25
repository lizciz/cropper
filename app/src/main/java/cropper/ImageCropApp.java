package cropper;

import java.io.IOException;

import javax.swing.UnsupportedLookAndFeelException;

import cropper.model.DefaultModel;
import cropper.view.MainView;

public class ImageCropApp {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    	DefaultModel model = new DefaultModel();
        MainView view = new MainView(model);
    }
}
