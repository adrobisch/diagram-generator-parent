package com.github.rmannibucau.graph.renderer;

import com.github.rmannibucau.loader.spi.graph.Diagram;

import java.io.File;

public interface DiagramRenderer {
  public void render(Diagram diagram, int width, int height, OutputFormat format, File output);
}
