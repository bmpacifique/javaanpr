/*
 * Copyright 2013 JavaANPR contributors
 * Copyright 2006 Ondrej Martinsky
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package net.sf.javaanpr.imageanalysis;

import net.sf.javaanpr.configurator.Configurator;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public class CarSnapshot extends Photo {

    private static int distributor_margins =
            Configurator.getConfigurator().getIntProperty("carsnapshot_distributormargins");
    public static Graph.ProbabilityDistributor distributor =
            new Graph.ProbabilityDistributor(0, 0, CarSnapshot.distributor_margins, CarSnapshot.distributor_margins);
    private static int carsnapshot_graphrankfilter =
            Configurator.getConfigurator().getIntProperty("carsnapshot_graphrankfilter");
    private static int numberOfCandidates = Configurator.getConfigurator().getIntProperty("intelligence_numberOfBands");
    private CarSnapshotGraph graphHandle = null;

    public CarSnapshot(String filename) throws IOException {
        super(Configurator.getConfigurator().getResourceAsStream(filename));

    }

    public CarSnapshot(BufferedImage bi) {
        super(bi);
    }

    public CarSnapshot(InputStream is) throws IOException {
        super(is);
    }

    public BufferedImage renderGraph() {
        this.computeGraph();
        return this.graphHandle.renderVertically(100, this.getHeight());
    }

    private Vector<Graph.Peak> computeGraph() {
        if (this.graphHandle != null) {
            return this.graphHandle.peaks;
        }
        BufferedImage imageCopy = duplicateBufferedImage(getImage());
        this.verticalEdgeBi(imageCopy);
        Photo.thresholding(imageCopy);
        this.graphHandle = this.histogram(imageCopy);
        this.graphHandle.rankFilter(CarSnapshot.carsnapshot_graphrankfilter);
        this.graphHandle.applyProbabilityDistributor(CarSnapshot.distributor);
        this.graphHandle.findPeaks(CarSnapshot.numberOfCandidates); // sort by height
        return this.graphHandle.peaks;
    }

    /**
     * Recommended: 3 bands.
     *
     * @return bands
     */
    public Vector<Band> getBands() {
        Vector<Band> out = new Vector<Band>();
        Vector<Graph.Peak> peaks = this.computeGraph();
        for (int i = 0; i < peaks.size(); i++) {
            // Cut from the original image of the plate and save to a vector.
            // ATTENTION: Cutting from original,
            // we have to apply an inverse transformation to the coordinates calculated from imageCopy
            Graph.Peak p = peaks.elementAt(i);
            out.add(new Band(getImage().getSubimage(0, (p.getLeft()), getImage().getWidth(), (p.getDiff()))));
        }
        return out;
    }

    public void verticalEdgeBi(BufferedImage image) {
        BufferedImage imageCopy = Photo.duplicateBufferedImage(image);
        float[] data = {-1, 0, 1, -1, 0, 1, -1, 0, 1, -1, 0, 1};
        new ConvolveOp(new Kernel(3, 4, data), ConvolveOp.EDGE_NO_OP, null).filter(imageCopy, image);
    }

    public CarSnapshotGraph histogram(BufferedImage bi) {
        CarSnapshotGraph graph = new CarSnapshotGraph(this);
        for (int y = 0; y < bi.getHeight(); y++) {
            float counter = 0;
            for (int x = 0; x < bi.getWidth(); x++) {
                counter += Photo.getBrightness(bi, x, y);
            }
            graph.addPeak(counter);
        }
        return graph;
    }
}
