package com.github.rmannibucau;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertTrue;

/**
 * @author Romain Manni-Bucau, Andreas Drobisch
 */
public class DiagramGeneratorMojoTest {
  @Test
  public void executeWithViewerRenderer() throws Exception {
    File input = new File(getClass().getResource("/spring/").getFile());

    DiagramGeneratorMojo mojo = testMojo(input, "viewer", "png");
    mojo.execute();

    assertTrue(new File(mojo.getOutput(), "camel.png").exists());
  }

  @Test
  public void executeWithBatikRenderer() throws Exception {
    File input = new File(getClass().getResource("/spring/").getFile());

    DiagramGeneratorMojo mojo = testMojo(input, "batik", "svg");
    mojo.execute();

    assertTrue(new File(mojo.getOutput(), "camel.svg").exists());
  }

  private DiagramGeneratorMojo testMojo(File input, String renderer, String format) {
    DiagramGeneratorMojo mojo = new DiagramGeneratorMojo();
    mojo.setInput(input.getPath());
    mojo.setOutput(input.getParentFile().getParentFile());
    mojo.setRenderer(renderer);
    mojo.setType("camel");
    mojo.setFileType("xml");
    mojo.setFormat(format);
    mojo.setWidth(480);
    mojo.setHeight(640);
    mojo.setView(false);
    return mojo;
  }

}
