package model.segmentation;

import java.util.HashMap;

import model.FeatureMatrix;
import model.SegmentationObserver;

public abstract class SegmentationAlgorithm extends Thread {

	public abstract void process(FeatureMatrix image, SegmentationObserver observer, HashMap<String, String> params);
}
