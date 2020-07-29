package org.apache.olingo.jpa.generator.api.client;

public class Task {

  private String groupId;
  private String artifactId;
  private String version;
  /**
   * The name of the persistence unit to generate code for contained JPA model classes.
   */
  private String persistenceUnit;

  public String getArtifactId() {
    return artifactId;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getVersion() {
    return version;
  }

  public String getPersistenceUnit() {
    return persistenceUnit;
  }

  /**
   *
   * @return The identifier for this task based on {@link #getGroupId()}, {@link #getArtifactId()} and
   * {@link #getVersion()}.
   */
  public String getId() {
    return getGroupId() + ":" + getArtifactId() + (getVersion() != null ? ":" + getVersion() : "");
  }
}
