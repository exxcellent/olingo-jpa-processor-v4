package org.apache.olingo.jpa.generator.api.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

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

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  private RepositorySystemSession repositorySystemSession;

  @Parameter(defaultValue = "${plugin}", readonly = true)
  private PluginDescriptor pluginDescriptor;

  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
  private List<RemoteRepository> remoteProjectRepositories;

  @SuppressWarnings("deprecation")
  @Component
  private RepositorySystem repositorySystem;

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
      throw new MojoExecutionException("Task " + task.getId() + " has no configured 'persistenceUnit'");
    }
    getLog().debug("Prepare generator for: " + task.getId() + "...");
    try {

      final GeneratorProject generatorProject = new GeneratorProject(getLog(), generationDirectory, generateProtocolCode);

      // add project dependencies as classpath
      for (org.apache.maven.artifact.Artifact artifact : project.getArtifacts()) {
        getLog().debug("Add dependency from project: " + artifact.getId());
        generatorProject.addClasspath(artifact.getFile());
      }

      //add plugin jar+dependencis to get generator classes resolved in nested class loader
      for (final org.apache.maven.artifact.Artifact artifact : pluginDescriptor.getArtifacts()) {
        getLog().debug("Add dependency from plugin: " + artifact.getId());
        generatorProject.addClasspath(artifact.getFile());
      }

      // add plugin configuration (task) as classpath
      MavenProject lProject = null;
      try {
        lProject  = session.getProjects().stream()
            .filter(p -> p.getGroupId().equals(task.getGroupId()) && p.getArtifactId().equals(task.getArtifactId()) && p.getVersion().equals(task.getVersion()))
            .findFirst().get();
        getLog().debug("Add local dependency from configuration: " + lProject.getArtifact().getGroupId() + ":" + lProject.getArtifact().getArtifactId() 
            + " (found in workspace), and " + lProject.getModel().getDependencies().size() + " more dependencies");
        generatorProject.addClasspath(lProject.getArtifact().getFile());
        //add also all transient dependencies
        for(Dependency dep: lProject.getModel().getDependencies()) {
          Collection<File> deps = resolveDependency(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
          for (File file : deps) {
            generatorProject.addClasspath(file);
          }          
        }
      } catch(RuntimeException ex) {
        //ignore
      }
      if(lProject == null) {
        //resolve remote (outside workspace)
        Collection<File> deps = resolveDependency(task.getGroupId(), task.getArtifactId(), task.getVersion());
        for (File file : deps) {
          generatorProject.addClasspath(file);
        }
      }      

      getLog().info("Generate code for task: " + task.getId() + "...");
      generatorProject.generate(task.getPersistenceUnit());
    } catch (final LinkageError e) {
      getLog().error(e);
      throw new MojoExecutionException("Incomplete classpath", e);
    }
  }

  private Collection<File> resolveDependency(String groupId, String artifactId, String version) throws MojoExecutionException {
    CollectRequest theCollectRequest = new CollectRequest();
    String theScope = JavaScopes.COMPILE;
    theCollectRequest
        .setRoot(new org.eclipse.aether.graph.Dependency(new DefaultArtifact(groupId, artifactId, null, "jar", version), null));
    for (RemoteRepository theRepository : remoteProjectRepositories) {
      theCollectRequest.addRepository(theRepository);
    }

    // We filter dependencies here, as we only want compile scope
    DependencyFilter theDependencyFilter = DependencyFilterUtils.classpathFilter(theScope);
    DependencyRequest theDependencyRequest = new DependencyRequest(theCollectRequest, theDependencyFilter);
    try {        
      DependencyResult theDependencyResult = repositorySystem.resolveDependencies(repositorySystemSession,
          theDependencyRequest);
      List<File> depFiles = new LinkedList<File>();
      for (ArtifactResult theArtifactResult : theDependencyResult.getArtifactResults()) {
        org.eclipse.aether.artifact.Artifact rArtifact = theArtifactResult.getArtifact();
        // Now we have the artifact file locally stored available and we can do something with it
        //TODO: getPath() is currently not implemented in Artifact, so we have to use the deprecated file
        @SuppressWarnings("deprecation")
        File theLocallyStoredFile = rArtifact.getFile();
        getLog().debug("Add resolved dependency: " + theLocallyStoredFile);
        depFiles.add(theLocallyStoredFile);
      }
      return depFiles;
    } catch (DependencyResolutionException e) {
      throw new MojoExecutionException("Error while resolving dependency", e);
    }    
  }
}
