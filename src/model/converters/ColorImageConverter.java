package model.converters;

import java.awt.Color;
import java.util.HashMap;

import model.ImageMatrix;

public class ColorImageConverter extends ImageConverter {

	public ColorImageConverter(ImageMatrix image, HashMap<String, String> params) {
		super(image, params);
	}

	
	@Override
	protected void createFeature(int i, int j, int[] feature) {
		Color c = new Color(image.getPixels()[i][j]);
		String colorSpace = params.get("colorSpace");
		if (colorSpace == null || colorSpace.equalsIgnoreCase("RGB")) {
			feature[0] = c.getRed();
			feature[1] = c.getGreen();
			feature[2] = c.getBlue();
		} else if (colorSpace.equalsIgnoreCase("HSB")) {
			float[] hsbvals = new float[3];
			hsbvals = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(),
					hsbvals);
			feature[0] = (int) (hsbvals[0] * 255);
			feature[1] = (int) (hsbvals[1] * 255);
			feature[2] = (int) (hsbvals[2] * 255);
		} else {
			feature[0] = (int)(0.3*c.getRed() + 0.59*c.getGreen() + 0.11 * c.getBlue());
		}
	}

	@Override
	protected int getDepth() {
		String colorSpace = params.get("colorSpace");
		if (colorSpace != null && colorSpace.equalsIgnoreCase("GRAYSCALE")) {
			return 1;
		} else {
			return 3;
		}
	}
}
