package org.geotools.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;

import org.junit.Test;

import com.sun.media.jai.util.SunTileCache;

public class CropImageTest {

    @Test
    public void testCropImagePB() {
        BufferedImage source = buildSource();
        
        ParameterBlock pb = buildParameterBlock(source);
        
        RenderedOp gtCropped = new ImageWorker(source).crop(10f, 50f, 20f, 20f).getRenderedOperation();//JAI.create("crop", pb);
        RenderedOp cropped = JAI.create("crop", pb);
        assertImageEquals(cropped, gtCropped);
    }

    @Test
    public void testTileCache() {
        TileCache tc = new SunTileCache();
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, tc);
        
        BufferedImage source = buildSource();
        //ParameterBlock pb = buildParameterBlock(source);
        
        RenderedOp gtCropped = new ImageWorker(source).setRenderingHints(hints)
                .crop(10f, 50f, 20f, 20f).getRenderedOperation();// JAI.create("GTCrop", pb, hints);
        gtCropped.getColorModel(); // force to compute the image
        assertSame(tc,  gtCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }
    
    @Test
    public void testNullTileCache() {
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        
        BufferedImage source = buildSource();
        //ParameterBlock pb = buildParameterBlock(source);
        
        RenderedOp gtCropped = new ImageWorker(source).setRenderingHints(hints)
                .crop(10f, 50f, 20f, 20f).getRenderedOperation();//JAI.create("GTCrop", pb, hints);
        gtCropped.getColorModel(); // force to compute the image
        assertNull(gtCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }
    
    @Test
    public void testNullTileCacheDescriptor() {
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        
        BufferedImage source = buildSource();
        
        //RenderedOp gtCropped = GTCropDescriptor.create(source, 10f, 10f, 20f, 20f, hints);
        ImageWorker w = new ImageWorker(source);
        RenderedOp gtCropped = w.setRenderingHints(hints).crop(10f, 10f, 20f, 20f)
                .getRenderedOperation();
        gtCropped.getColorModel(); // force to compute the image
        assertNull(gtCropped.getRenderingHint(JAI.KEY_TILE_CACHE));
    }
    
    private BufferedImage buildSource() {
        BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = (Graphics2D) source.getGraphics();
        g.setPaint(new GradientPaint(new Point(0, 0), Color.WHITE, new Point(100, 100), Color.BLACK));
        g.dispose();
        return source;
    }

    private void assertImageEquals(RenderedOp first, RenderedOp second) {
        //RenderedOp difference = SubtractDescriptor.create(first, second, null);
        //RenderedOp stats = ExtremaDescriptor.create(difference, null, 1, 1, false, 1, null);
        ImageWorker w = new ImageWorker(first);
        w.subtract(second).setnoData(null);
        double[] minimum = (double[]) w.getMinimums();//stats.getProperty("minimum");
        double[] maximum = (double[]) w.getMaximums();//stats.getProperty("maximum");
        assertEquals(minimum[0], maximum[0], 0.0);
        assertEquals(minimum[1], maximum[1], 0.0);
        assertEquals(minimum[2], maximum[2], 0.0);
    }

    private ParameterBlock buildParameterBlock(BufferedImage source) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add((float) 10);
        pb.add((float) 50);
        pb.add((float) 20);
        pb.add((float) 20);
        return pb;
    }
}
