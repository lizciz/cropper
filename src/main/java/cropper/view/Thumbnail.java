package cropper.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import util.Utils;

public class Thumbnail extends JPanel {

	private static final long serialVersionUID = 1L;

	public static final int MAX_WIDTH = 200;
	public static final int MAX_HEIGHT = 100;

	private final String id;
	private final JLabel lblImage;
	private final JLabel lblFilename;
	private final String filename;

	private static final Color DEFAULT_BG_COLOR = new Color(200, 200, 200);
	private static final Color SELECTED_BG_COLOR = new Color(150, 150, 150);

	private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(Color.DARK_GRAY, 3);
	private static final Border DEFAULT_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3);

	private BufferedImage originalImage;
	private BufferedImage scaledImage;

	public Thumbnail(String id, String filename, BufferedImage image) {
		this.id = id;
		this.filename = filename;

		setBorder(DEFAULT_BORDER);
		setBackground(DEFAULT_BG_COLOR);
		setLayout(new BorderLayout(0, 0));
		lblImage = new JLabel();
		lblImage.setPreferredSize(new Dimension((int) (MAX_HEIGHT * 1.5), MAX_HEIGHT));
		lblImage.setBorder(null);
		lblImage.setHorizontalAlignment(SwingConstants.CENTER);
		add(lblImage, BorderLayout.CENTER);

		lblFilename = new JLabel(filename);
		lblFilename.setBorder(new EmptyBorder(1, 1, 1, 1));
		lblFilename.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblFilename.setHorizontalAlignment(SwingConstants.CENTER);
		add(lblFilename, BorderLayout.SOUTH);

		updateImage(image);
	}

	public void updateImage(BufferedImage newImage) {
		originalImage = newImage;
		scaledImage = Utils.resizeImage(originalImage, MAX_WIDTH, MAX_HEIGHT);
		lblImage.setPreferredSize(null);
		lblImage.setIcon(new ImageIcon(scaledImage));
		revalidate();
		repaint();
	}

	public void select() {
		setBorder(SELECTED_BORDER);
		setBackground(SELECTED_BG_COLOR);
	}

	public void deselect() {
		setBorder(DEFAULT_BORDER);
		setBackground(DEFAULT_BG_COLOR);
	}

	public String getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public BufferedImage getOriginalImage() {
		return originalImage;
	}

}
