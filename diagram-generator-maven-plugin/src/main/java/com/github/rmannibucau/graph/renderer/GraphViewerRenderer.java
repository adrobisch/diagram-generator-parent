package com.github.rmannibucau.graph.renderer;

import com.github.rmannibucau.DiagramGeneratorMojo;
import com.github.rmannibucau.graph.GraphViewer;
import com.github.rmannibucau.graph.layout.LevelLayout;
import com.github.rmannibucau.listener.CloseWindowWaiter;
import com.github.rmannibucau.loader.spi.graph.Diagram;
import com.github.rmannibucau.loader.spi.graph.Edge;
import com.github.rmannibucau.loader.spi.graph.Node;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public class GraphViewerRenderer implements DiagramRenderer {

  Logger log = LoggerFactory.getLogger(GraphViewerRenderer.class);

  private final DiagramGeneratorMojo configuration;

  VisualizationViewer<Node, Edge> viewer;

  public GraphViewerRenderer(DiagramGeneratorMojo configuration) {
    this.configuration = configuration;
  }

  @Override
  public void render(Diagram diagram, int width, int height, OutputFormat format, File output) {
    Dimension outputSize = new Dimension(width, height);

    final LevelLayout layout = new LevelLayout(diagram);
    viewer = new GraphViewer(layout);

    layout.setVertexShapeTransformer(viewer.getRenderContext().getVertexShapeTransformer());
    layout.setSize(outputSize);
    layout.setIgnoreSize(configuration.getAdjust());
    layout.reset();
    viewer.setPreferredSize(layout.getSize());
    viewer.setSize(layout.getSize());

    saveView(layout.getSize(), outputSize, diagram.getName(), output);
    showViewer(diagram);
  }

  private void showViewer(Diagram diagram) {
    // viewing the window if necessary
    if (configuration.isView()) {
      // creating a realized window to be sure the viewer will be able to draw correctly the graph
      final JFrame window = createWindow(diagram.getName());

      DefaultModalGraphMouse<Node, Edge> gm = new DefaultModalGraphMouse<Node, Edge>();
      gm.setMode(DefaultModalGraphMouse.Mode.PICKING);
      viewer.setGraphMouse(gm);

      CountDownLatch latch = new CountDownLatch(1);
      CloseWindowWaiter waiter = new CloseWindowWaiter(latch);
      window.setVisible(true);
      window.addWindowListener(waiter);
      try {
        latch.await();
      } catch (InterruptedException e) {
        log.error("can't await window close event", e);
      }
    }
  }

  private JFrame createWindow(String name) {
    viewer.setBackground(Color.WHITE);

    JFrame frame = new JFrame(name + " viewer");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setLayout(new GridLayout());
    frame.getContentPane().add(viewer);
    frame.pack();

    return frame;
  }

  private void saveView(Dimension currentSize, Dimension desiredSize, String name, File output) {
    BufferedImage bi = createImage(currentSize, desiredSize);
    writeImage(name, bi, output);
  }

  private BufferedImage createImage(Dimension currentSize, Dimension desiredSize) {
    BufferedImage bi = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_INT_ARGB);
    bi.getGraphics().fillRect(0,0, currentSize.width, currentSize.height);
    Graphics2D g = bi.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

    boolean db = viewer.isDoubleBuffered();
    viewer.setDoubleBuffered(false);
    viewer.paint(g);
    viewer.setDoubleBuffered(db);
    bi = resizeImage(currentSize, desiredSize, bi);
    g.dispose();
    return bi;
  }

  private BufferedImage resizeImage(Dimension currentSize, Dimension desiredSize, BufferedImage bi) {
    if (!currentSize.equals(desiredSize)) {
      double xFactor = desiredSize.width * 1. / currentSize.width;
      double yFactor = desiredSize.height * 1. / currentSize.height;
      double factor = Math.min(xFactor, yFactor);
      log.info("optimal size is (" + currentSize.width + ", " + currentSize.height + ")");
      log.info("scaling with a factor of " + factor);

      AffineTransform tx = new AffineTransform();
      tx.scale(factor, factor);
      AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
      BufferedImage biNew = new BufferedImage((int) (bi.getWidth() * factor), (int) (bi.getHeight() * factor), bi.getType());
      bi = op.filter(bi, biNew);
    }
    return bi;
  }

  private void writeImage(String name, BufferedImage bi, File output) {
    OutputStream os = null;
    try {
      os = new FileOutputStream(new File(output, name + "." + configuration.getFormat()));
      if (!ImageIO.write(bi, configuration.getFormat(), os)) {
        throw new RuntimeException("can't save picture " + name + "." + configuration.getFormat());
      }
    } catch (IOException e) {
      throw new RuntimeException("can't save the diagram", e);
    } finally {
      if (os != null) {
        try {
          os.flush();
          os.close();
        } catch (IOException e) {
          throw new RuntimeException("can't close diagram", e);
        }
      }
    }
  }

}
