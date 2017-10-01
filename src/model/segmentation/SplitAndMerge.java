package model.segmentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.FeatureMatrix;
import model.SegmentationObserver;
public class SplitAndMerge extends SegmentationAlgorithm {

	private FeatureMatrix image;
	private SegmentationObserver observer;

	private double splitStandardDeviation = 5;
	private double mergeStandardDeviation = 5;
	private int minSize = 3;

	public void process(FeatureMatrix image, SegmentationObserver observer,
			HashMap<String, String> params) {
		this.image = image;
		this.observer = observer;

		Double auxDouble;
		Integer auxInteger;
		String s;
		s = params.get("splitStandardDeviation");
		System.out.println(s);
		if (s != null && (auxDouble = Double.parseDouble(s)) != null) {
			splitStandardDeviation = auxDouble;
		}
		s = params.get("mergeStandardDeviation");
		System.out.println(s);
		if (s != null && (auxDouble = Double.parseDouble(s)) != null) {
			mergeStandardDeviation = auxDouble;
		}
		s = params.get("minSize");
		System.out.println(s);
		if (s != null && (auxInteger = Integer.parseInt(s)) != null) {
			minSize = auxInteger;
		}

		System.out.println("Split and merge "
				+ splitStandardDeviation + " " + mergeStandardDeviation + " "
				+ minSize);
	}

	public void run() {
		Set<ImageZone> zones = new HashSet<ImageZone>();
		int currentSegmentIndex = 1;
		boolean changed = true;

		zones.add(new ImageZone(0, image.getWidth(), 0, image.getHeight(),
				currentSegmentIndex));

		/* SPLIT */
		while (changed) {
			if (Thread.interrupted())
				return;
			Set<ImageZone> auxZones = new HashSet<ImageZone>();

			changed = false;

			for (ImageZone zone : zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						image.getSegment()[i][j] = (byte) zone.segment.segmentIndex;
					}
				}
				if (zone.isHomogeneus() || zone.size() <= minSize
						|| zone.xTo - zone.xFrom == 1
						|| zone.yTo - zone.yFrom == 1) {
					auxZones.add(zone);
				} else {
					changed = true;
					auxZones.addAll(Arrays.asList(zone
							.divide(currentSegmentIndex)));
					currentSegmentIndex += 4;
				}
			}

			observer.onChange();
			zones = auxZones;
		}

		/* COSMOVISION */
		ArrayList<ImageSegment> segments = new ArrayList<ImageSegment>();
		for (ImageZone zone : zones) {
			if (Thread.interrupted())
				return;
			ImageSegment currentSegment = zone.segment;

			Set<ImageZone> neighbourZones = new HashSet<ImageZone>();
			neighbourZones.addAll(zone.north);
			neighbourZones.addAll(zone.south);
			neighbourZones.addAll(zone.east);
			neighbourZones.addAll(zone.west);

			for (ImageZone z : neighbourZones) {
				if (z.segment != currentSegment) {
					currentSegment.neighbours.add(z.segment);
				}
			}
			segments.add(currentSegment);
		}

		/* MERGE */
		changed = true;
		while (changed) {
			if (Thread.interrupted())
				return;
			changed = false;

			sortAndRemoveEmpty(segments);

			for (ImageSegment segment : segments) {

				if (segment.zones.size() == 0)
					continue;

				ImageSegment bestNeighbour = segment
						.getBestHomogeneousNeighbour();

				if (bestNeighbour != null) {

					segment.mergeWithNeighbour(bestNeighbour);
					changed = true;

					for (ImageZone zone : segment.zones) {
						for (int i = zone.yFrom; i < zone.yTo; i++) {
							for (int j = zone.xFrom; j < zone.xTo; j++) {
								image.getSegment()[i][j] = (byte) (segment.segmentIndex);
							}
						}
					}
				}
			}
			observer.onChange();
		}
		if (observer != null) {
			observer.onComplete();
		}
	}

	private void sortAndRemoveEmpty(ArrayList<ImageSegment> segments) {

		List<ImageSegment> auxList = new ArrayList<ImageSegment>();
		for (ImageSegment s : segments) {
			if (s.size() != 0) {
				auxList.add(s);
			}
		}

		ImageSegment[] segmentsArray = new ImageSegment[auxList.size()];
		Arrays.sort(auxList.toArray(segmentsArray),
				new Comparator<ImageSegment>() {
					public int compare(ImageSegment arg0, ImageSegment arg1) {
						return arg0.size() - arg1.size();
					}
				});

		segments.clear();
		int index = 1;
		for (ImageSegment s : segmentsArray) {
			segments.add(s);
			s.segmentIndex = index++;
		}
		return;
	}

	class ImageSegment {

		private Set<ImageSegment> neighbours = new HashSet<ImageSegment>();
		private Set<ImageZone> zones = new HashSet<ImageZone>();
		private int segmentIndex;

		public ImageSegment(ImageZone zone, int segment) {
			this.zones.add(zone);
			zone.segment = this;
			this.segmentIndex = segment;
		}

		public ImageSegment getBestHomogeneousNeighbour() {

			double bestNeighbourDistance = Double.MAX_VALUE;
			ImageSegment bestNeighbour = null;

			for (ImageSegment neighbour : this.neighbours) {
				if (neighbour.size() == 0) {
					continue;
				}
				if (this.isHomogeneousWithRespectTo(neighbour)) {
					if (this.distanceWithNeighbour(neighbour) < bestNeighbourDistance) {
						bestNeighbourDistance = this
								.distanceWithNeighbour(neighbour);
						bestNeighbour = neighbour;
					}
				}
			}
			return bestNeighbour;
		}

		public boolean isHomogeneousWithRespectTo(ImageSegment segment) {
			for (int i = 0; i < image.getDepth(); i++) {
				if (this.distanceWith(segment, i) > mergeStandardDeviation)
					return false;
			}
			return true;
		}

		public void mergeWithNeighbour(ImageSegment segment) {

			segment.segmentIndex = 0;

			for (ImageZone zone : segment.zones) {
				zone.segment = this;
			}
			this.zones.addAll(segment.zones);
			segment.zones.clear();

			for (ImageSegment neighbour : segment.neighbours) {
				neighbour.neighbours.remove(segment);

				if (neighbour != this) {
					neighbour.neighbours.add(this);
				}
			}

			this.neighbours.addAll(segment.neighbours);
			this.neighbours.remove(this);

			segment.neighbours.clear();
			segment.zones.clear();
		}

		public double distanceWithNeighbour(ImageSegment segment) {
			double acum = 0;
			for (int i = 0; i < image.getDepth(); i++) {
				acum += Math.pow(this.distanceWith(segment, i), 2);
			}
			return Math.sqrt(acum);
		}

		private double distanceWith(ImageSegment segment, int feature) {

			double mean = 0;
			double standardDeviation = 0;
			int count = 0;

			for (ImageZone zone : zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						mean += image.getData()[i][j][feature];
						count++;
					}
				}
			}

			for (ImageZone zone : segment.zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						mean += image.getData()[i][j][feature];
						count++;
					}
				}
			}

			mean = mean / count;

			for (ImageZone zone : zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						standardDeviation += Math.pow(
								image.getData()[i][j][feature] - mean, 2);
					}
				}
			}
			for (ImageZone zone : segment.zones) {
				for (int i = zone.yFrom; i < zone.yTo; i++) {
					for (int j = zone.xFrom; j < zone.xTo; j++) {
						standardDeviation += Math.pow(
								image.getData()[i][j][feature] - mean, 2);
					}
				}
			}
			standardDeviation = Math.sqrt(standardDeviation / count);
			return standardDeviation;
		}

		public int size() {
			int count = 0;
			for (ImageZone zone : zones) {
				count += (zone.yTo - zone.yFrom) * (zone.xTo - zone.xFrom);
			}
			return count;
		}
	}

	class ImageZone {
		private int xFrom;
		private int yFrom;
		private int xTo;
		private int yTo;

		private ImageSegment segment;

		private Set<ImageZone> east = new HashSet<ImageZone>();
		private Set<ImageZone> west = new HashSet<ImageZone>();
		private Set<ImageZone> north = new HashSet<ImageZone>();
		private Set<ImageZone> south = new HashSet<ImageZone>();

		public ImageZone(int x_from, int x_to, int y_from, int y_to,
				int segmentIndex) {

			if (x_to <= x_from || y_to <= y_from) {
				throw new RuntimeException("Zone cann not be less than 1 pixel.");
			}

			this.xFrom = x_from;
			this.yFrom = y_from;
			this.xTo = x_to;
			this.yTo = y_to;

			this.segment = new ImageSegment(this, segmentIndex);
		}

		public ImageZone[] divide(int segment) {

			int x_mid = xFrom + ((xTo - xFrom) / 2);
			int y_mid = yFrom + ((yTo - yFrom) / 2);

			ImageZone nw = new ImageZone(xFrom, x_mid, yFrom, y_mid, segment);
			ImageZone ne = new ImageZone(x_mid, xTo, yFrom, y_mid, segment + 1);
			ImageZone sw = new ImageZone(xFrom, x_mid, y_mid, yTo, segment + 2);
			ImageZone se = new ImageZone(x_mid, xTo, y_mid, yTo, segment + 3);


			nw.north = new HashSet<ImageZone>();
			ne.north = new HashSet<ImageZone>();
			for (ImageZone neighbour : north) {
				neighbour.south.remove(this);

				if (neighbour.xFrom < nw.xTo) {
					nw.north.add(neighbour);
					neighbour.south.add(nw);
				}

				if (neighbour.xTo > ne.xFrom) {
					ne.north.add(neighbour);
					neighbour.south.add(ne);
				}
			}

			sw.south = new HashSet<ImageZone>();
			se.south = new HashSet<ImageZone>();
			for (ImageZone neighbour : south) {
				neighbour.north.remove(this);

				if (neighbour.xFrom < sw.xTo) {
					sw.south.add(neighbour);
					neighbour.north.add(sw);
				}

				if (neighbour.xTo > se.xFrom) {
					se.south.add(neighbour);
					neighbour.north.add(se);
				}
			}

			nw.west = new HashSet<ImageZone>();
			sw.west = new HashSet<ImageZone>();
			for (ImageZone neighbour : west) {
				neighbour.east.remove(this);

				if (neighbour.yFrom < nw.yTo) {
					nw.west.add(neighbour);
					neighbour.east.add(nw);
				}

				if (neighbour.yFrom < sw.yTo) {
					sw.west.add(neighbour);
					neighbour.east.add(sw);
				}
			}

			ne.east = new HashSet<ImageZone>();
			se.east = new HashSet<ImageZone>();
			for (ImageZone neighbour : east) {
				neighbour.west.remove(this);

				if (neighbour.yFrom < ne.yTo) {
					ne.east.add(neighbour);
					neighbour.west.add(ne);
				}

				if (neighbour.yFrom < se.yTo) {
					se.east.add(neighbour);
					neighbour.west.add(se);
				}
			}

			nw.south = new HashSet<ImageZone>(Arrays.asList(sw));
			nw.east = new HashSet<ImageZone>(Arrays.asList(ne));
			ne.west = new HashSet<ImageZone>(Arrays.asList(nw));
			ne.south = new HashSet<ImageZone>(Arrays.asList(se));
			sw.north = new HashSet<ImageZone>(Arrays.asList(nw));
			sw.east = new HashSet<ImageZone>(Arrays.asList(se));
			se.north = new HashSet<ImageZone>(Arrays.asList(ne));
			se.west = new HashSet<ImageZone>(Arrays.asList(sw));

			return new ImageZone[] { nw, ne, sw, se };
		}

		public boolean isHomogeneus() {
			for (int i = 0; i < image.getDepth(); i++) {
				if (this.standardDeviation(i) > splitStandardDeviation)
					return false;
			}
			return true;
		}

		public double standardDeviation(int feature) {
			double mean = mean(feature);
			double acum = 0;
			int count = 0;

			for (int i = yFrom; i < yTo; i++) {
				for (int j = xFrom; j < xTo; j++) {
					acum += Math.pow(image.getData()[i][j][feature] - mean, 2);
					count++;
				}
			}

			acum = acum / count;
			return Math.sqrt(acum);
		}

		public double mean(int feature) {
			double acum = 0;
			int count = 0;

			for (int i = yFrom; i < yTo; i++) {
				for (int j = xFrom; j < xTo; j++) {
					acum += image.getData()[i][j][feature];
					count++;
				}
			}

			return acum / count;
		}

		public int size() {
			return (xTo - xFrom) * (yTo - yFrom);
		}
	}

}
