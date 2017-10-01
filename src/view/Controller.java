package view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import model.FeatureMatrix;
import model.ImageMatrix;
import model.SegmentationObserver;
import model.converters.ImageConverter;
import model.segmentation.SegmentationAlgorithm;

public class Controller {

	private CgTpe1 view;

	private BufferedImage original;

	private ImageMatrix matrix;

	private double zoom;

	private SegmentationAlgorithm algorithm;

	public Controller(CgTpe1 frame) {
		view = frame;
		original = null;
		matrix = null;
		zoom = 1.0;
		algorithm = null;
	}

	public void open() {
		File f = view.getFileFromFileChooser();
		if (f != null) {
			try {
				original = ImageIO.read(f);
			} catch (IOException e) {
				raiseException(String.format("Error file", f.toString(), e
						.getLocalizedMessage()));
			}
			undoAll();
		}
	}

	public void saveAs() {
		if (original == null || matrix == null) {
			raiseException("Nothing to save.");
			return;
		}
		saveAs(view.saveFileWithFileChooser());
	}

	public void saveAs(File f) {
		if (f == null) {
			return;
		}
		try {
			String name = f.getName();
			int i = name.lastIndexOf('.');
			String extension = (i < 0 ? "png" : name.substring(i + 1));
			BufferedImage segmented = matrix.getBufferedImage();
			ImageIO.write(segmented, extension, f);
		} catch (IOException e) {
			view.showErrorDialog("Error", "File can not be saved");
		}
	}

	public void undoAll() {
		if (original == null) {
			raiseException("No image");
			return;
		}

		zoom = 1.0;
		ImageView iv = view.getImageView();
		iv.redrawImages(original, original, zoom);

		try {
			matrix = new ImageMatrix(original);
		} catch (IOException e) {
			raiseException(e.getMessage());
			return;
		}
	}

	public void quit() {
		view.quit();
	}

	public void applySegmentation() {
		if (original == null || matrix == null) {
			raiseException("Nothing to save");
			return;
		}

		OptionsPanel p = new OptionsPanel();
		p.setVisible(true);
		if (view.showConfirmDialog(p, "Segmentation")) {
			ImageConverter ic = p.getSelectedFeature(matrix);
			final FeatureMatrix fm = ic.createFeatureMatrix();
			stopSegmentation();
			algorithm = p.getSelectedSegmentationMethod();
			algorithm.process(fm, new SegmentationObserver() {

				private int i = 0;

				public void onChange() {
					matrix = fm.getImageMatrix();
					view.getImageView().redrawImage(matrix.getBufferedImage(),
							zoom);
				}

				public void onComplete() {
				}

			}, p.getSegmentationParameters());
			algorithm.start();
		}
	}

	public void stopSegmentation() {
		if (algorithm == null) {
			return;
		}
		algorithm.interrupt();
		algorithm = null;
	}

	public void raiseException(String message) {
		view.showErrorDialog("Error", message);
	}

}
