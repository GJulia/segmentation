package model.converters;

import java.util.HashMap;

import model.FeatureMatrix;
import model.ImageMatrix;

public abstract class ImageConverter {
	protected ImageMatrix image;
	protected HashMap<String, String> params;
	
	public ImageConverter(ImageMatrix image, HashMap<String, String> params) {
		this.image = image;
		this.params = params;
	}

	public FeatureMatrix createFeatureMatrix() {
		FeatureMatrix imageMatrix = new FeatureMatrix(image.getWidth(), 
					image.getHeight(), this.getDepth());
		
		for (int i=0; i<image.getHeight(); i++) {
			for (int j=0; j<image.getWidth(); j++) {
				createFeature(i, j, imageMatrix.getData()[i][j]);
			}
		}
		
		return imageMatrix;
	}

	protected abstract int getDepth();

	protected abstract void createFeature(int i, int j, int[] feature);
}
