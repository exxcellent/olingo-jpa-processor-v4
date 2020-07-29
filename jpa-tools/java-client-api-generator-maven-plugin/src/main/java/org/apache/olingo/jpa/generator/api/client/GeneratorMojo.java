package org.apache.olingo.jpa.generator.api.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

@Mojo(name = "client-api-generation", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

  @Parameter(name = "generation-directory", defaultValue = "${basedir}/target/generated-sources/odata-client-api")
  private File generationDirectory;

  @Parameter(name = "compileGeneratedCode", defaultValue = "true")
  private boolean compileGeneratedCode;

  @Parameter(name = "skip", defaultValue = "false", property = "client-api-generation.skip")
  private boolean skip;

  @Parameter(name = "process-dependencies")
  private final List<Task> processDependencies = new ArrayList<Task>();

  @Parameter(name = "generateProtocolCode", defaultValue = "true")
  private boolean generateProtocolCode;

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The Maven project helper object
   */
  @Component
  private MavenProjectHelper projectHelper;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Component
  private DependencyResolver dependencyResolver;

  @Component
  private PluginDescriptor pluginDescriptor;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Code generation skipped, will do nothing");
      return;
    }
    if (processDependencies.isEmpty()) {
      throw new MojoFailureException("No dependency to process declared");
    }
    for (final Task dep : processDependencies) {
      processTask(dep);
    }

    if (compileGeneratedCode) {
      getLog().info("Add " + generationDirectory.getAbsolutePath() + " as source directory to compile...");
      project.addCompileSourceRoot(generationDirectory.getAbsolutePath());
    }
  }

  private void processTask(final Task task) throws MojoExecutionException {

    if (task.getPersistenceUnit() == null || task.getPersistenceUnit().isEmpty()) {
      throw new MojoExecutionException("Task "+task.getId()+" has no configured 'persistenceUnit'");
    }
    getLog().debug("Prepare generator for: " + task.getId() + "...");
    try {

      final GeneratorProject generatorProject = new GeneratorProject(getLog(), generationDirectory,
          generateProtocolCode);

      // add project dependencies as classpath
      for (final Object obj : project.getDependencies()) {
        final org.apache.maven.model.Dependency dep = (org.apache.maven.model.Dependency) obj;
        final DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
        coordinate.setGroupId(dep.getGroupId());
        coordinate.setArtifactId(dep.getArtifactId());
        coordinate.setVersion(dep.getVersion());

        final ProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        final Iterator<ArtifactResult> result = dependencyResolver.resolveDependencies(buildingRequest, coordinate,
            null).iterator();
        while (result.hasNext()) {
          final ArtifactResult r = result.next();
          final Artifact artifact = r.getArtifact();
          getLog().debug("Add dependency from project: " + artifact.getId());
          generatorProject.addClasspath(artifact);
        }
      }

      //add plugin jar+dependencis to get generator classes resolved in nested class loader
      for(final Artifact artifact: pluginDescriptor.getArtifacts()) {
        getLog().debug("Add dependency from plugin: " + artifact.getId());
        generatorProject.addClasspath(artifact);
      }

      // add plugin configuration as classpath
      final DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
      coordinate.setGroupId(task.getGroupId());
      coordinate.setArtifactId(task.getArtifactId());
      coordinate.setVersion(task.getVersion());

      final ProjectBuildingRequest buildingRequest =
          new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
      final Iterator<ArtifactResult> result = dependencyResolver.resolveDependencies(buildingRequest, coordinate,
          null).iterator();
      while (result.hasNext()) {
        final ArtifactResult r = result.next();
        final Artifact artifact = r.getArtifact();
        getLog().debug("Add dependency from configuration: " + artifact.getId());
        generatorProject.addClasspath(artifact);
      }
      getLog().info("Generate code for task: " + task.getId() + "...");
      generatorProject.generate(task.getPersistenceUnit());
    } catch (final LinkageError e) {
      getLog().error(e);
      throw new MojoExecutionException("Incomplete classpath", e);
    } catch (final DependencyResolverException e) {
      throw new MojoExecutionException("Problem resolving dependency", e);
    }
  }

}
