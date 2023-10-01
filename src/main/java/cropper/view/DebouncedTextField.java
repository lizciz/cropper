package cropper.view;

import java.util.function.Consumer;

import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@SuppressWarnings("serial")
public class DebouncedTextField extends JTextField {

	public DebouncedTextField(String initialText, int delay, Consumer<String> callback) {

		super(initialText);

		Timer timer = new Timer(delay, e -> {
			callback.accept(getText());
		});
		timer.setRepeats(false);

		// Add a DocumentListener to the text field
		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				timer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				timer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// This is not relevant for text fields
			}
		});
	}

}
