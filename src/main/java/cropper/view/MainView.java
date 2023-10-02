package cropper.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import cropper.CropperProps;
import cropper.model.ModelAPI;
import cropper.model.ModelView;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.miginfocom.swing.MigLayout;
import util.FormattedStringBuilder;
import util.Lang;

public class MainView extends JFrame implements ModelView {

	private static final String TITLE = "Cropper";

	private JTextField txtPreferredWidth;
	private JTextField txtPreferredHeight;

	private JPanel thumbnailPanel;

	private ModelAPI model;
	private CropperProps props;
	private Lang lang;

	private JTextField txtCopyright;

	private Thumbnail selectedThumbnail;
	private ImagePanel currentImagePanel;

	private final MouseListener unfocus = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			contentPane.requestFocus();
		}
	};
	private JScrollPane thumbnailScroller;
	private JPanel contentPane;
	private ProgressOverlay overlay;
	private JLabel lblImageIndex;

	public MainView(ModelAPI model, CropperProps props, Lang lang)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {

		this.model = model;
		this.props = props;
		this.lang = lang;
		model.setView(this);

		Platform.startup(() -> {
		});

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		setTitle(TITLE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(700, 700));
		setLocationRelativeTo(null);

		createUIComponents();

		setVisible(true);

	}

	@Override
	public void imageWasLoaded(final String id, final String filename, final BufferedImage image) {
		SwingUtilities.invokeLater(() -> {

			Thumbnail thumbnail = new Thumbnail(id, filename, image);
			thumbnail.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					selectThumbnail(id);
				}
			});
			thumbnailPanel.add(thumbnail);
			lblImageIndex.setText(String.format("-/%d", thumbnailPanel.getComponentCount()));
			revalidate();

		});
	}

	@Override
	public void imagesFailedToLoad(final List<File> failed) {
		SwingUtilities.invokeLater(() -> {
			FormattedStringBuilder sb = new FormattedStringBuilder();
			sb.a("<html><p>").a(lang.get(Lang.LOAD_FAILED_MESSAGE, failed.size())).a("</p><ul>");
			for (File file : failed) {
				sb.f("<li>%s</li>", file.getName());
			}
			sb.a("</ul></html>");
			JOptionPane.showMessageDialog(MainView.this, sb.toString(), lang.get(Lang.LOAD_FAILED_TITLE),
					JOptionPane.WARNING_MESSAGE);
		});

	}

	@Override
	public void imageWasUpdated(String id, BufferedImage image, boolean hasUnsavedChanges) {
		SwingUtilities.invokeLater(() -> {
			Thumbnail thumbnail = findThumbnail(id);
			if (thumbnail == null)
				return;

			thumbnail.updateImage(image);
			if (selectedThumbnail != null && selectedThumbnail.getId().equals(id)) {
				currentImagePanel.setImage(image);
			}
			SwingUtilities.invokeLater(this::updateTitle);
			SwingUtilities.invokeLater(this::scrollToCurrentThumnail);
		});
	}

	@Override
	public void imageWasRemoved(final String id) {
		SwingUtilities.invokeLater(() -> {
			int index = findThumbnailIndex(id);
			if (index != -1) {
				thumbnailPanel.remove(index);
				_selectHelper(index);
			}
		});
	}

	private Thumbnail findThumbnail(String id) {
		Component[] components = thumbnailPanel.getComponents();
		for (Component t : components) {
			if (Objects.equals(id, ((Thumbnail) t).getId())) {
				return (Thumbnail) t;
			}
		}
		return null;
	}

	private int findThumbnailIndex(String id) {
		Component[] thumbnails = thumbnailPanel.getComponents();
		for (int i = 0; i < thumbnails.length; i++) {
			Thumbnail thumbnail = (Thumbnail) thumbnails[i];
			if (Objects.equals(id, thumbnail.getId())) {
				return i;
			}
		}
		return -1;
	}

	private void createUIComponents() {

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				scrollToCurrentThumnail();
			}
		});

		contentPane = new JPanel(new BorderLayout(0, 0));
		setContentPane(contentPane);
		contentPane.addMouseListener(unfocus);

		InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "leftArrow");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "rightArrow");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "trash");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK), "delete");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");

		ActionMap actionMap = contentPane.getActionMap();
		actionMap.put("leftArrow", action(e -> selectPreviousImage()));
		actionMap.put("rightArrow", action(e -> selectNextImage()));
		actionMap.put("trash", action(e -> trashSelectedImage()));
		actionMap.put("delete", action(e -> deleteSelectedImage()));
		actionMap.put("undo", action(e -> undoSelectedImageChanges()));
		actionMap.put("redo", action(e -> redoSelectedImageChanges()));

		contentPane.setLayout(new BorderLayout());

		JPanel toolbar = new JPanel();
		contentPane.add(toolbar, BorderLayout.NORTH);
		toolbar.setLayout(new MigLayout("", "[][][][][][][][][][][grow]", "[][][grow][][]"));

		JButton btnSelectImageDir = new JButton(lang.get(Lang.SELECT_IMG_DIR_BUTTON_TEXT));
		btnSelectImageDir.addActionListener(e -> Platform.runLater(() -> {
			DirectoryChooser dirChooser = new DirectoryChooser();
			dirChooser.setTitle("Open image directory");
			File file = new File(props.getLastPath());
			if (file.exists() && file.isDirectory())
				dirChooser.setInitialDirectory(file);
			else
				dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));

			File selectedDir = dirChooser.showDialog(new Stage());

			if (selectedDir != null && selectedDir.isDirectory()) {
				props.setLastPath(selectedDir.getAbsolutePath());
				model.openImageDirectory(selectedDir);
			}
		}));
		toolbar.add(btnSelectImageDir, "cell 0 0");

		Component rigidArea = Box.createRigidArea(new Dimension(20, 20));
		toolbar.add(rigidArea, "cell 1 0 1 2");

		JLabel lblPreferredSize = new JLabel(lang.get(Lang.PREFERRED_SIZE_TEXT));
		toolbar.add(lblPreferredSize, "cell 2 0 4 1,alignx left");

		JLabel lblCopyright = new JLabel(lang.get(Lang.COPYRIGHT_LABEL));
		toolbar.add(lblCopyright, "cell 7 0");

		JButton btnSave = new JButton(lang.get(Lang.SAVE_ALL_BUTTON_TEXT));
		btnSave.addActionListener(e -> model.save(txtCopyright.getText().trim()));
		toolbar.add(btnSave, "cell 9 0,growx");

		JCheckBox checkLimitZoom = new JCheckBox(lang.get(Lang.LIMIT_ZOOM_LABEL), props.isMaxZoom100());
		checkLimitZoom.addItemListener(e -> {
			props.setMaxZoom100(e.getStateChange() == ItemEvent.SELECTED);
			contentPane.requestFocus();
			currentImagePanel.repaint();
		});
		toolbar.add(checkLimitZoom, "cell 0 1");

		txtPreferredWidth = new DebouncedTextField(props.getWidth(), 1000, txt -> props.setWidth(txt));
		txtPreferredWidth.setPreferredSize(new Dimension(30, 20));
		txtPreferredWidth.setMinimumSize(new Dimension(30, 20));
		toolbar.add(txtPreferredWidth, "cell 2 1,alignx right");
		txtPreferredWidth.setColumns(4);

		JLabel lblSizeSeparator = new JLabel("x");
		toolbar.add(lblSizeSeparator, "cell 3 1,alignx center");

		txtPreferredHeight = new DebouncedTextField(props.getHeight(), 1000, txt -> props.setHeight(txt));
		txtPreferredHeight.setPreferredSize(new Dimension(30, 20));
		txtPreferredHeight.setMinimumSize(new Dimension(30, 20));
		toolbar.add(txtPreferredHeight, "cell 4 1,alignx left");
		txtPreferredHeight.setColumns(4);

		JLabel lblSizeUnit = new JLabel("px");
		toolbar.add(lblSizeUnit, "cell 5 1");

		Component rigidArea_1 = Box.createRigidArea(new Dimension(20, 20));
		toolbar.add(rigidArea_1, "cell 6 0 1 2");

		txtCopyright = new DebouncedTextField(props.getCopyright(), 1000, txt -> props.setCopyright(txt));
		txtCopyright.setMinimumSize(new Dimension(200, 20));
		txtCopyright.setPreferredSize(new Dimension(250, 20));
		txtCopyright.setColumns(15);
		toolbar.add(txtCopyright, "cell 7 1,alignx left");

		Component glue = Box.createGlue();
		toolbar.add(glue, "flowx,cell 8 1");

		JButton btnResize = new JButton(lang.get(Lang.SAVE_RESIZE_BUTTON_TEXT));
		btnResize.addActionListener(e -> {
			model.save(txtCopyright.getText().trim());
			try {
				int width = Integer.parseInt(txtPreferredWidth.getText());
				int height = Integer.parseInt(txtPreferredHeight.getText());
				model.resize(width, height, txtCopyright.getText().trim());
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(MainView.this,
						lang.get(Lang.INVALID_SIZE_MESSAGE), lang.get(Lang.INVALID_SIZE_TITLE),
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		toolbar.add(btnResize, "cell 9 1,growx");

		Component glue_1 = Box.createGlue();
		toolbar.add(glue_1, "cell 10 0 1 2");

		thumbnailScroller = new JScrollPane();
		thumbnailScroller.setEnabled(false);
		thumbnailScroller.setWheelScrollingEnabled(false);
		thumbnailScroller.setFocusable(false);
		thumbnailScroller.setBorder(null);
		thumbnailScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		thumbnailScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		toolbar.add(thumbnailScroller, "cell 0 2 10 1,grow");

		thumbnailPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) thumbnailPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEADING);
		thumbnailScroller.setViewportView(thumbnailPanel);
		thumbnailPanel.setBorder(null);

		JPanel panel = new JPanel();
		toolbar.add(panel, "cell 0 3 11 1,grow");
		panel.setLayout(new MigLayout("", "[100px][grow][100px]", "[]"));

		lblImageIndex = new JLabel("-/-");
		lblImageIndex.setFont(new Font("Tahoma", Font.PLAIN, 14));
		panel.add(lblImageIndex, "cell 0 0,alignx center");

		JLabel lblZoomLevel = new JLabel("-");
		panel.add(lblZoomLevel, "cell 1 0,alignx center");
		lblZoomLevel.setHorizontalAlignment(SwingConstants.CENTER);
		lblZoomLevel.setFont(new Font("Tahoma", Font.BOLD, 14));

		Component glue_2 = Box.createGlue();
		panel.add(glue_2, "cell 2 0");

		JSeparator separator = new JSeparator();
		separator.setForeground(Color.GRAY);
		toolbar.add(separator, "cell 0 4 11 1,growx,aligny center");

		Component rigidArea_2 = Box.createRigidArea(new Dimension(20, 20));
		toolbar.add(rigidArea_2, "cell 8 0 1 2");

		currentImagePanel = new ImagePanel(
				area -> model.performCrop(selectedThumbnail.getId(), area),
				zoom -> lblZoomLevel.setText(zoom + "%"),
				props);
		currentImagePanel.addMouseListener(unfocus);
		contentPane.add(currentImagePanel, BorderLayout.CENTER);

		overlay = new ProgressOverlay();
		setGlassPane(overlay);

		// new Thread(() -> {
		// try {
		// Thread.sleep(5000);
		// } catch (InterruptedException e1) {
		// }
		// showOverlay("Resizing empty space", "Unknown how...");
		// }).start();

	}

	private void selectNextImage() {
		if (overlay.isOverlayShowing())
			return;
		if (selectedThumbnail == null) {
			_selectHelper(0);
		} else {
			int index = findThumbnailIndex(selectedThumbnail.getId());
			_selectHelper(index + 1);
		}
	}

	private void selectPreviousImage() {
		if (overlay.isOverlayShowing())
			return;
		if (selectedThumbnail == null) {
			_selectHelper(0);
		} else {
			int index = findThumbnailIndex(selectedThumbnail.getId());
			_selectHelper(index - 1);
		}
	}

	private void selectThumbnail(String id) {
		int index = findThumbnailIndex(id);
		_selectHelper(index);
	}

	private void _selectHelper(int index) {
		if (thumbnailPanel.getComponentCount() == 0) {
			lblImageIndex.setText("-/-");
			return;
		}
		index = Math.max(0, Math.min(index, thumbnailPanel.getComponentCount() - 1));
		Thumbnail newThumbnail = (Thumbnail) thumbnailPanel.getComponent(index);

		if (selectedThumbnail != null && selectedThumbnail.getId().equals(newThumbnail.getId())) {
			return;
		}
		if (selectedThumbnail != null) {
			selectedThumbnail.deselect();
		}
		newThumbnail.select();
		selectedThumbnail = newThumbnail;
		currentImagePanel.setImage(newThumbnail.getOriginalImage());

		lblImageIndex.setText(String.format("%d/%d", index + 1, thumbnailPanel.getComponentCount()));
		updateTitle();
		scrollToCurrentThumnail();
	}

	private void updateTitle() {
		if (selectedThumbnail != null) {
			String filename = selectedThumbnail.getFilename();
			int[] info = model.getSizeInfo(selectedThumbnail.getId());
			setTitle(String.format("%s - %s [%d x %d px, %d kb]",
					TITLE, filename, info[0], info[1], info[2]));
		} else {
			setTitle("Cropper");
		}
	}

	private void trashSelectedImage() {
		if (overlay.isOverlayShowing())
			return;
		if (selectedThumbnail == null) {
			return;
		}
		model.trashImage(selectedThumbnail.getId());
	}

	private void deleteSelectedImage() {
		if (overlay.isOverlayShowing())
			return;
		if (selectedThumbnail == null) {
			return;
		}

		int choice = JOptionPane.showConfirmDialog(
				MainView.this,
				lang.get(Lang.CONFIRM_DELETE_MESSAGE),
				lang.get(Lang.CONFIRM_DELETE_TITLW),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (choice == JOptionPane.YES_OPTION) {
			model.deleteImage(selectedThumbnail.getId());
		}
	}

	private void undoSelectedImageChanges() {
		if (overlay.isOverlayShowing())
			return;
		if (selectedThumbnail == null) {
			return;
		}
		model.undoImageChanges(selectedThumbnail.getId());
	}

	private void redoSelectedImageChanges() {
		if (overlay.isOverlayShowing())
			return;
		if (selectedThumbnail == null) {
			return;
		}
		model.redoImageChanges(selectedThumbnail.getId());
	}

	private Action action(ActionListener listener) {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.actionPerformed(e);
			}
		};
	}

	@Override
	public void showOverlay(String message, String extraInfo) {
		overlay.showOverlay(message, extraInfo);
	}

	@Override
	public void hideOverlay() {
		overlay.hideOverlay();
	}

	private void scrollToCurrentThumnail() {
		if (selectedThumbnail != null) {
			String id = selectedThumbnail.getId();
			Component[] thumbnails = thumbnailPanel.getComponents();

			int leftWidth = 0;

			// Find all thumb nails to the left of the selected one
			int index = 0;
			for (; index < thumbnails.length; index++) {
				Thumbnail t = (Thumbnail) thumbnails[index];
				if (t.getId().equals(id))
					break;
				leftWidth += t.getWidth() + 5;
			}
			int imgX = leftWidth + 5 + selectedThumbnail.getWidth() / 2;

			int panelWidth = thumbnailPanel.getWidth();
			int viewportWidth = thumbnailScroller.getViewport().getWidth();
			int maxX = Math.max(0, panelWidth - viewportWidth);

			if (index > thumbnailPanel.getComponentCount() - 3) {
				int viewportX = Math.min(imgX + viewportWidth / 2, maxX);
				// If viewing the last thumb nail, align right edge
				thumbnailScroller.getViewport().setViewPosition(new Point(viewportX, 0));
			} else {
				int viewportX = Math.max(0, imgX - viewportWidth / 2);
				thumbnailScroller.getViewport().setViewPosition(new Point(viewportX, 0));
			}

			thumbnailScroller.revalidate();
			thumbnailPanel.revalidate();
		}
	}

}
