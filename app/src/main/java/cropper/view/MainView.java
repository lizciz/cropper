package cropper.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import cropper.model.DefaultModel;
import cropper.model.ModelView;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.miginfocom.swing.MigLayout;
import util.FormattedStringBuilder;
import util.Tuple;

@SuppressWarnings("serial")
public class MainView extends JFrame implements ModelView {

	private JTextField txtPreferredWidth;
	private JTextField txtPreferredHeight;

	private JPanel thumbnailPanel;

	private DefaultModel model;

	private final ExecutorService pool = Executors.newCachedThreadPool();
	private JTextField txtCopyright;

	private Thumbnail selectedThumbnail;
	private ImagePanel currentImagePanel;

	private final MouseListener unfocus = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			getContentPane().requestFocus();
		}
	};
	private JScrollPane thumbnailScroller;

	public MainView(DefaultModel model) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {

		this.model = model;
		model.setView(this);

		Platform.startup(() -> {
		});

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		setTitle("Cropper");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(800, 600));
		setLocationRelativeTo(null);

		createUIComponents();

		setVisible(true);

	}

	@Override
	public void imagesWereLoaded(final List<Tuple<BufferedImage, String>> loaded, final List<File> failed) {
		if (!failed.isEmpty()) {
			SwingUtilities.invokeLater(() -> {
				FormattedStringBuilder sb = new FormattedStringBuilder();
				sb.a("<html>").f("<p>Failed to load %d files:</p>", failed.size()).a("<ul>");
				for (File file : failed) {
					sb.f("<li>%s</li>", file.getName());
				}
				sb.a("</ul></html>");
				JOptionPane.showMessageDialog(MainView.this, sb.toString(), "Error when loading",
						JOptionPane.WARNING_MESSAGE);
			});
		}
		if (!loaded.isEmpty()) {

			SwingUtilities.invokeLater(() -> {

				List<Thumbnail> thumbnails = new ArrayList<>();
				for (int i = 0; i < loaded.size(); i++) {
					Tuple<BufferedImage, String> tuple = loaded.get(i);
					final String filename = tuple.v2();
					thumbnails.add(addThumbnail(filename, i));
				}
				revalidate();

				for (int i = 0; i < loaded.size(); i++) {

					Thumbnail thumbnail = thumbnails.get(i);
					Tuple<BufferedImage, String> tuple = loaded.get(i);
					final BufferedImage originalImage = tuple.v1();

					pool.submit(() -> {

						// Define the dimensions for the thumb nail
						int thumbnailHeight = 100;

						// Calculate the scaling factors to fit within the thumb nail size
						double scaleFactor = (double) thumbnailHeight / originalImage.getHeight();

						// Create a new BufferedImage for the thumb nail
						BufferedImage scaled = new BufferedImage((int) (originalImage.getWidth() * scaleFactor),
								(int) (originalImage.getHeight() * scaleFactor), BufferedImage.TYPE_INT_ARGB);

						// Apply the transformation
						AffineTransform transform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
						Graphics2D g2d = scaled.createGraphics();
						g2d.drawImage(originalImage, transform, null);
						g2d.dispose();

						SwingUtilities.invokeLater(() -> {
							thumbnail.updateImage(scaled);
							revalidate();
						});

					});
				}

			});
		}

	}

	@Override
	public void displayImage(final int index, final BufferedImage image) {
		SwingUtilities.invokeLater(() -> {
			int idx = Math.max(0, Math.min(index, thumbnailPanel.getComponentCount()));
			if (selectedThumbnail != null && selectedThumbnail.getIndex() == index) {
				return;
			}
			Thumbnail newThumbnail = (Thumbnail) thumbnailPanel.getComponent(idx);

			if (selectedThumbnail != null) {
				selectedThumbnail.deselect();
			}
			newThumbnail.select();
			selectedThumbnail = newThumbnail;
			currentImagePanel.setImage(image);

			scrollToCurrentThumnail();
		});
	}

	private void createUIComponents() {

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				scrollToCurrentThumnail();
			}
		});

		JComponent contentPane = (JComponent) getContentPane();
		contentPane.addMouseListener(unfocus);

		InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "leftArrow");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "rightArrow");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");

		ActionMap actionMap = contentPane.getActionMap();
		actionMap.put("leftArrow", action(e -> model.selectPreviousImage()));
		actionMap.put("rightArrow", action(e -> model.selectNextImage()));
		actionMap.put("delete", action(e -> model.deleteSelectedImage()));
		actionMap.put("undo", action(e -> model.undoSelectedImageChanges()));
		actionMap.put("redo", action(e -> model.redoSelectedImageChanges()));

		getContentPane().setLayout(new BorderLayout());

		JPanel toolbar = new JPanel();
		getContentPane().add(toolbar, BorderLayout.NORTH);
		toolbar.setLayout(new MigLayout("", "[][][][][][][][][][][grow]", "[][][grow][][]"));

		JButton btnSelectImageDir = new JButton("Select image directory");
		btnSelectImageDir.addActionListener(e -> Platform.runLater(() -> {
			DirectoryChooser dirChooser = new DirectoryChooser();
			dirChooser.setTitle("Open image directory");
			dirChooser.setInitialDirectory(new File("./src/main/resources/"));

			File selectedDir = dirChooser.showDialog(new Stage());

			if (selectedDir != null && selectedDir.isDirectory()) {
				model.openImageDirectory(selectedDir);
			}
		}));
		toolbar.add(btnSelectImageDir, "cell 0 0");

		JLabel lblSelectedDirectory = new JLabel("path to the selected image directory");
		toolbar.add(lblSelectedDirectory, "cell 1 0");

		Component rigidArea = Box.createRigidArea(new Dimension(20, 20));
		toolbar.add(rigidArea, "cell 2 0 1 2");

		JLabel lblPreferredSize = new JLabel("Preferred size (w/h):");
		toolbar.add(lblPreferredSize, "cell 3 0 4 1,alignx left");

		JLabel lblCopyright = new JLabel("Copyright text:");
		toolbar.add(lblCopyright, "cell 8 0");

		txtPreferredWidth = new JTextField();
		txtPreferredWidth.setPreferredSize(new Dimension(30, 20));
		txtPreferredWidth.setMinimumSize(new Dimension(30, 20));
		toolbar.add(txtPreferredWidth, "cell 3 1,alignx right");
		txtPreferredWidth.setColumns(4);

		JLabel lblSizeSeparator = new JLabel("x");
		toolbar.add(lblSizeSeparator, "cell 4 1,alignx center");

		txtPreferredHeight = new JTextField();
		txtPreferredHeight.setPreferredSize(new Dimension(30, 20));
		txtPreferredHeight.setMinimumSize(new Dimension(30, 20));
		toolbar.add(txtPreferredHeight, "cell 5 1,alignx left");
		txtPreferredHeight.setColumns(4);

		JLabel lblSizeUnit = new JLabel("px");
		toolbar.add(lblSizeUnit, "cell 6 1");

		Component rigidArea_1 = Box.createRigidArea(new Dimension(20, 20));
		toolbar.add(rigidArea_1, "cell 7 0 1 2");

		txtCopyright = new JTextField();
		txtCopyright.setMinimumSize(new Dimension(250, 20));
		txtCopyright.setPreferredSize(new Dimension(250, 20));
		txtCopyright.setColumns(15);
		toolbar.add(txtCopyright, "cell 8 1,alignx left");

		Component glue = Box.createGlue();
		toolbar.add(glue, "cell 9 1");

		thumbnailScroller = new JScrollPane();
		thumbnailScroller.setEnabled(false);
		thumbnailScroller.setWheelScrollingEnabled(false);
		thumbnailScroller.setFocusable(false);
		thumbnailScroller.setBorder(null); // BorderFactory.createLineBorder(Color.RED, 2));
		thumbnailScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		thumbnailScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		toolbar.add(thumbnailScroller, "cell 0 2 11 1,grow");

		thumbnailPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) thumbnailPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEADING);
		thumbnailScroller.setViewportView(thumbnailPanel);
		thumbnailPanel.setBorder(null); // BorderFactory.createLineBorder(Color.GREEN, 2));

		JLabel lblZoomLevel = new JLabel("-");
		lblZoomLevel.setHorizontalAlignment(SwingConstants.CENTER);
		lblZoomLevel.setFont(new Font("Tahoma", Font.BOLD, 14));
		toolbar.add(lblZoomLevel, "cell 0 3 11 1,growx");

		JSeparator separator = new JSeparator();
		separator.setForeground(Color.GRAY);
		toolbar.add(separator, "cell 0 4 11 1,growx,aligny center");

		currentImagePanel = new ImagePanel(area -> model.performCrop(area), zoom -> lblZoomLevel.setText(zoom + "%"));
		currentImagePanel.addMouseListener(unfocus);
		getContentPane().add(currentImagePanel, BorderLayout.CENTER);

	}

	private Thumbnail addThumbnail(final String filename, final int index) {
		Thumbnail thumbnail = new Thumbnail(filename, index);
		thumbnail.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				model.selectImage(index);
			}
		});

		thumbnailPanel.add(thumbnail);

		return thumbnail;
	}

	private Action action(ActionListener listener) {
		return new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				listener.actionPerformed(e);
			}
		};
	}

	private void scrollToCurrentThumnail() {
		if (selectedThumbnail != null) {
			int index = selectedThumbnail.getIndex();

			Component[] components = thumbnailPanel.getComponents();

			int leftWidth = 0;
			int totalWidth = 0;

			for (int i = 0; i < components.length; i++) {
				if (i < index)
					leftWidth += components[i].getWidth() + 5;
				totalWidth += components[i].getWidth() + 5;
			}
			int imgX = leftWidth + 5 + components[index].getWidth() / 2;

			int panelWidth = thumbnailPanel.getWidth();
			int viewportWidth = thumbnailScroller.getViewport().getWidth();
			int maxX = Math.max(0, panelWidth - viewportWidth);

			System.out.println(
					"panel/total with " + panelWidth + " " + totalWidth + " for " + components.length + " images");

			if (index > thumbnailPanel.getComponentCount() - 4) {

				int viewportX = Math.min(imgX + viewportWidth / 2, maxX);

				// If viewing the last thumb nail, align right edge
				thumbnailScroller.getViewport().setViewPosition(new Point(viewportX, 0));
				System.out.println("scrolling RIGHT " + viewportX);
			} else {

				int viewportX = Math.max(0, imgX - viewportWidth / 2);

				thumbnailScroller.getViewport().setViewPosition(new Point(viewportX, 0));
				System.out.println("scrolling LEFT " + viewportX);
			}

			thumbnailScroller.revalidate();
			thumbnailPanel.revalidate();
		}
	}

//	private void scrollToThumbnail(JPanel panel, int selectedIndex) {
//
//	}
//
//	private static void scrollToThumbnailOLD(JPanel panel, int selectedIndex) {
//		Component[] components = panel.getComponents();
//
//		int visibleWidth = panel.getParent().getWidth();
//		int usableWidth = visibleWidth - components[selectedIndex].getWidth() + 100;
//
//		int leftIdx = selectedIndex - 1;
//		int rightIdx = selectedIndex + 1;
//		int leftWidth = 0;
//		int rightWidth = 0;
//		int totalWidth = 0;
//
//		boolean doLeft = true;
//		boolean doRight = true;
//
//		// Find the first and last thumb nails to show
//		while ((leftIdx >= 0 || rightIdx < components.length) && (doLeft || doRight)) {
//			if (doLeft && (leftWidth <= rightWidth || !doRight)) {
//				if (leftIdx >= 0) {
//					int w = components[leftIdx].getWidth();
//					if (totalWidth + w > usableWidth) {
//						doLeft = false;
//					} else {
//						totalWidth += w;
//						leftWidth += w;
//						leftIdx--;
//					}
//				} else {
//					doLeft = false;
//				}
//			} else if (doRight) {
//				if (rightIdx < components.length) {
//					int w = components[rightIdx].getWidth();
//					if (totalWidth + w > usableWidth) {
//						doRight = false;
//					} else {
//						totalWidth += w;
//						rightWidth += w;
//						rightIdx++;
//					}
//				} else {
//					doRight = false;
//				}
//			}
//		}
//
//		// Set which components are visible based on the total width
//		for (int i = 0; i < components.length; i++) {
//			components[i].setVisible(leftIdx < i && i < rightIdx);
//		}
//
//	}
}
