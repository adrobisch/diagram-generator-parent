package com.github.rmannibucau.graph.renderer;

import com.github.rmannibucau.graph.layout.LevelLayout;
import com.github.rmannibucau.loader.spi.graph.Diagram;
import com.github.rmannibucau.loader.spi.graph.Edge;
import com.github.rmannibucau.loader.spi.graph.Node;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.collections15.Transformer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.*;

public class BatikRenderer implements DiagramRenderer {
    SVGGraphics2D svgGenerator;

    int nodeHeight = 50;
    int nodeWidth = 50;

    int margin = 20;
    int fontSize = 5;

    Transformer<Node, Shape> vertexShapeTransformer = new Transformer<Node, Shape>() {

        @Override
        public Shape transform(Node node) {
            return new Rectangle(0, 0, nodeWidth + margin, nodeHeight + margin);
        }
    };

    @Override
    public void render(Diagram diagram, int width, int height, OutputFormat format, File output) {
        final LevelLayout layout = new LevelLayout(diagram);
        layout.setSize(new Dimension(width, height));
        layout.setVertexShapeTransformer(vertexShapeTransformer);
        layout.initialize();
        Document document = createSvgDocument();
        svgGenerator = new SVGGraphics2D(document);
        drawNodes(layout);
        drawEdges(layout);

        switch (format) {
            case PNG:
                writePng(new File(output, diagram.getName() + ".png"), svgGenerator);
                break;
            case JPG:
                writeJpg(new File(output, diagram.getName() + ".jpg"), svgGenerator);
                break;
            case SVG:
                writeSvg(new File(output, diagram.getName() + ".svg"), svgGenerator);
                break;
        }
    }

    private void drawNodes(LevelLayout layout) {
        for (Node node : layout.getGraph().getVertices()) {
            int xpos = (int) layout.getX(node) + margin;
            int ypos = (int) layout.getY(node) + +margin;

            svgGenerator.setPaint(Color.GREEN);
            svgGenerator.fill(new Rectangle(xpos, ypos, nodeWidth, nodeHeight));
            svgGenerator.setPaint(Color.black);
            svgGenerator.drawRect(xpos, ypos, nodeWidth, nodeHeight);
            svgGenerator.setPaint(Color.black);
            svgGenerator.setBackground(Color.white);
            drawCenteredNodeText(node, xpos, ypos);
        }
    }

    private void drawCenteredNodeText(Node node, int xpos, int ypos) {
        svgGenerator.drawString(node.getText(), (xpos + nodeWidth / 2) - (node.getText().length() /2)*fontSize, ypos + nodeHeight / 2);
    }

    private void drawEdges(LevelLayout layout) {
        for (Edge edge : layout.getGraph().getEdges()) {
            int sourceX = (int) layout.getX(layout.getGraph().getSource(edge));
            int sourceY = (int) layout.getY(layout.getGraph().getSource(edge));

            int targetX = (int) layout.getX(layout.getGraph().getDest(edge));
            int targetY = (int) layout.getY(layout.getGraph().getDest(edge));

            svgGenerator.setPaint(Color.black);
            drawArrow(svgGenerator, sourceX + margin + nodeWidth / 2, sourceY + margin + nodeHeight, targetX + margin + nodeWidth / 2, targetY + margin);
            svgGenerator.drawString(edge.getText(), sourceX + (targetX - sourceX) / 2 - margin, targetY + (targetY - sourceY) / 2 - margin);
        }
    }

    private void drawArrow(SVGGraphics2D svgGenerator, int sourceX, int sourceY, int targetX, int targetY) {
        AffineTransform tx = new AffineTransform();
        Line2D.Double line = new Line2D.Double(sourceX, sourceY, targetX, targetY);
        svgGenerator.drawLine(sourceX, sourceY, targetX, targetY);

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(0, 5);
        arrowHead.addPoint(-5, -5);
        arrowHead.addPoint(5, -5);

        tx.setToIdentity();
        double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
        tx.translate(line.x2, line.y2);
        tx.rotate((angle - Math.PI / 2d));

        Graphics2D g = (Graphics2D) svgGenerator.create();
        g.setTransform(tx);
        g.fill(arrowHead);
        g.dispose();
    }

    private Document createSvgDocument() {
        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        return domImpl.createDocument(svgNS, "svg", null);
    }

    public void writeJpg(File outputFile, SVGGraphics2D svgGenerator) {
        JPEGTranscoder transcoder = new JPEGTranscoder();
        transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY,
                new Float(.8));

        transcode(outputFile, svgGenerator, transcoder);
    }

    public void writePng(File outputFile, SVGGraphics2D svgGenerator) {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcode(outputFile, svgGenerator, transcoder);
    }

    private void transcode(File outputFile, SVGGraphics2D svgGenerator, ImageTranscoder transcoder) {
        try {
            TranscoderInput input = new TranscoderInput(svgGenerator.getDOMFactory());
            OutputStream ostream = new FileOutputStream(outputFile);
            TranscoderOutput output = new TranscoderOutput(ostream);
            transcoder.transcode(input, output);
            ostream.flush();
            ostream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSvg(File output, SVGGraphics2D svgGenerator) {
        try {
            boolean useCSS = true; // we want to use CSS style attributes
            Writer out = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");
            svgGenerator.stream(out, useCSS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
