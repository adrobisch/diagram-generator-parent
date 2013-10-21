package com.github.rmannibucau;

import com.github.rmannibucau.graph.renderer.BatikRenderer;
import com.github.rmannibucau.graph.renderer.DiagramRenderer;
import com.github.rmannibucau.graph.renderer.GraphViewerRenderer;
import com.github.rmannibucau.graph.renderer.OutputFormat;
import com.github.rmannibucau.loader.spi.FileType;
import com.github.rmannibucau.loader.spi.Loader;
import com.github.rmannibucau.loader.spi.LoaderHelper;
import com.github.rmannibucau.loader.spi.graph.Diagram;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Romain Manni-Bucau
 *         <p/>
 *         This mojo aims to generate a diagram route diagram from camel code/context.
 */
@Mojo(threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, name = "generate")
public class DiagramGeneratorMojo extends AbstractMojo {
  @Parameter(property = "diagram.input", required = true)
  private String input;

  @Parameter(property = "diagram.viewer", defaultValue = "false")
  private boolean view;

  @Parameter(property = "diagram.width", defaultValue = "640")
  private int width;

  @Parameter(property = "diagram.height", defaultValue = "480")
  private int height;

  @Parameter(property = "diagram.adjust", defaultValue = "true")
  private boolean adjust;

  /**
   * Base output directory for reports.
   */
  @Parameter(property = "diagram.output", defaultValue = "${project.build.directory}/diagram/")
  private File output;

  @Parameter(property = "diagram.type", defaultValue = "camel")
  private String type;

  @Parameter(property = "diagram.fileType", defaultValue = "xml")
  private String fileType;

  @Parameter(property = "diagram.format", defaultValue = "png")
  private String format;

  @Parameter(property = "diagram.renderer", defaultValue = "viewer")
  private String renderer;

  @Parameter
  private List<String> additionalClasspathElements;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    final ClassLoader oldClassloader = setClassLoader();

    try {
      initOutput();
      final Loader loader = LoaderHelper.getLoader(type);
      final List<Diagram> diagrams = loader.load(input, FileType.valueOf(fileType.toUpperCase()));
      for (Diagram diagram : diagrams) {
        createRenderer().render(diagram, width, height, OutputFormat.valueOf(format.toUpperCase()), output);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassloader);
    }
  }

  private ClassLoader setClassLoader() {
    final ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
    final ClassLoader extendedClassloader = classloaderWithAdditionalElements();

    Thread.currentThread().setContextClassLoader(extendedClassloader);
    return oldClassloader;
  }

  private DiagramRenderer createRenderer() {
    if (renderer.equalsIgnoreCase("viewer")) {
      return new GraphViewerRenderer(this);
    } else if (renderer.equalsIgnoreCase("batik")) {
      return new BatikRenderer();
    }
    throw new IllegalArgumentException("Unknown renderer specified.");
  }

  private void initOutput() {
    // saving it too
    if (!output.exists()) {
      output.mkdirs();
    }
  }

  private ClassLoader classloaderWithAdditionalElements() {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (additionalClasspathElements == null || additionalClasspathElements.isEmpty()) {
      additionalClasspathElements = new ArrayList<String>();
      additionalClasspathElements.add("target/classes");
    }

    final List<URL> urls = new ArrayList<URL>(additionalClasspathElements.size());
    for (String add : additionalClasspathElements) {
      final File file = new File(add);
      if (file.exists()) {
        try {
          urls.add(file.toURI().toURL());
        } catch (MalformedURLException e) {
          getLog().warn("Ignoring '" + add + "'", e);
        }
      } else {
        getLog().warn("Ignoring '" + add + "' since it doesn't exist.");
      }
    }

    return new URLClassLoader(urls.toArray(new URL[urls.size()]), cl);
  }

  public List<String> getAdditionalClasspathElements() {
    return additionalClasspathElements;
  }

  public void setAdditionalClasspathElements(List<String> additionalClasspathElements) {
    this.additionalClasspathElements = additionalClasspathElements;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public File getOutput() {
    return output;
  }

  public void setOutput(File output) {
    this.output = output;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public boolean isView() {
    return view;
  }

  public void setView(boolean view) {
    this.view = view;
  }

  public boolean getAdjust() {
    return adjust;
  }

  public void setAdjust(boolean adjust) {
    this.adjust = adjust;
  }

  public String getRenderer() {
    return renderer;
  }

  public void setRenderer(String renderer) {
    this.renderer = renderer;
  }

}
