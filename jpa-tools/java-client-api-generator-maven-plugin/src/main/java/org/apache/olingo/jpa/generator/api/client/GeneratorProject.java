package org.apache.olingo.jpa.generator.api.client;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

class GeneratorProject {

  private final List<Artifact> classpathEntries = new LinkedList<>();
  private final File generationDirectory;
  private final Log log;
  private final Boolean generateProtocolCode;

  /**
   *
   * @param generationDirectory The target directory for generated sources
   */
  public GeneratorProject(final Log log, final File generationDirectory, final boolean generateProtocolCode)
      throws MojoExecutionException {
    this.generationDirectory = generationDirectory;
    this.log = log;
    this.generateProtocolCode = Boolean.valueOf(generateProtocolCode);
    if (generationDirectory == null) {
      throw new MojoExecutionException("'generation-directory' not configured");
    }
    if (generationDirectory.exists() && !generationDirectory.isDirectory()) {
      throw new MojoExecutionException("'generation-directory' "+generationDirectory.getAbsolutePath()+" points not to an directory");
    }
  }

  public void addClasspath(final Artifact dependency) {
    if (!classpathEntries.contains(dependency)) {
      classpathEntries.add(dependency);
    }
  }

  public void generate(final String pUnit) throws RuntimeException, MojoExecutionException {
    final URL[] urls = classpath2Urls();
    try (URLClassLoader projectClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());) {
      final Class<?> generatorClass = projectClassLoader.loadClass(
          "org.apache.olingo.jpa.generator.api.client.generatorclassloader.Generator");
      final Object generatorInstance = generatorClass.getDeclaredConstructor().newInstance();
      // create generation directory on-demand
      if (!generationDirectory.exists()) {
        generationDirectory.mkdirs();
      }
      final Method configureMethod = generatorClass.getMethod("configure", Object.class, File.class, boolean.class);
      configureMethod.invoke(generatorInstance, log, generationDirectory, generateProtocolCode);
      final Method generateMethod = generatorClass.getMethod("generate", String.class);
      generateMethod.invoke(generatorInstance, pUnit);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (final InstantiationException e) {
      throw new RuntimeException(e);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException | NoSuchMethodException e) {
      e.printStackTrace();
      throw new MojoExecutionException("Problem creating generator instance", e);
    }
  }

  private URL[] classpath2Urls() throws MojoExecutionException {
    final URL[] urls = new URL[classpathEntries.size()];
    for (int i = 0; i < classpathEntries.size(); i++) {
      try {
        urls[i] = classpathEntries.get(i).getFile().toURI().toURL();
      } catch (final MalformedURLException e) {
        throw new MojoExecutionException("Couldn't convert file path " + classpathEntries.get(i).getFile() + " to URL",
            e);
      }
    }
    return urls;
  }

}
