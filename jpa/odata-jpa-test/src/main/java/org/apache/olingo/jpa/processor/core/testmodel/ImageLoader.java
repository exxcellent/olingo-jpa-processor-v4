package org.apache.olingo.jpa.processor.core.testmodel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

public class ImageLoader {
  private static final String SELECT_PERSON_IMAGE =
      "SELECT * FROM \"OLINGO\".\"org.apache.olingo.jpa::PersonImage\" WHERE ID = '$&1'";
  private static final String SELECT_ORGANIZATION_IMAGE =
      "SELECT * FROM \"OLINGO\".\"org.apache.olingo.jpa::OrganizationImage\" WHERE ID = '$&1'";
  private static final String PATH = "images/";
  private static final String TEST_IMAGE = "test.png";
  private static final String ENTITY_MANAGER_DATA_SOURCE = "javax.persistence.nonJtaDataSource";
  private static final String PUNIT_NAME = "org.apache.olingo.jpa";

  public static void main(final String[] args) throws Exception {
    final ImageLoader i = new ImageLoader();
    final EntityManager em = createEntityManager();
    i.loadPerson(em, "OlingoOrangeTM.png", "99");

  }

  public void loadPerson(final EntityManager em, final String imageName, final String businessPartnerID) {
    final byte[] image = loadImage(imageName);
    storePersonImageDB(em, image, businessPartnerID, SELECT_PERSON_IMAGE);
    storeImageLocal(image, "restored.png");
  }

  public void loadPerson(final String imageName, final String businessPartnerID) {
    final byte[] image = loadImage(imageName);
    storePersonImageDB(createEntityManager(), image, businessPartnerID, SELECT_PERSON_IMAGE);
    storeImageLocal(image, "restored.png");
  }

  public void loadOrg(final EntityManager em, final String imageName, final String businessPartnerID) {
    final byte[] image = loadImage(imageName);
    storeOrgImageDB(em, image, businessPartnerID, SELECT_ORGANIZATION_IMAGE);
    storeImageLocal(image, "restored.png");
  }

  public void loadOrg(final String imageName, final String businessPartnerID) {
    final byte[] image = loadImage(imageName);
    storeOrgImageDB(createEntityManager(), image, businessPartnerID, SELECT_ORGANIZATION_IMAGE);
    storeImageLocal(image, "restored.png");
  }

  private void storePersonImageDB(final EntityManager em, final byte[] image, final String businessPartnerID, final String query) {

    final String s = query.replace("$&1", businessPartnerID);
    final Query q = em.createNativeQuery(s, PersonImage.class);
    @SuppressWarnings("unchecked")
    final
    List<PersonImage> result = q.getResultList();
    result.get(0).setImage(image);
    updateDB(em, result);

    final Query storedImageQ = em.createNativeQuery(s, PersonImage.class);
    @SuppressWarnings("unchecked")
    final
    List<PersonImage> result2 = storedImageQ.getResultList();
    final byte[] storedImage = result2.get(0).getImage();
    System.out.println(storedImage.length);
    compareImage(image, storedImage);
    storeImageLocal(storedImage, TEST_IMAGE);

  }

  private void storeOrgImageDB(final EntityManager em, final byte[] image, final String businessPartnerID, final String query) {

    final String s = query.replace("$&1", businessPartnerID);
    final Query q = em.createNativeQuery(s, OrganizationImage.class);
    @SuppressWarnings("unchecked")
    final
    List<OrganizationImage> result = q.getResultList();
    result.get(0).setImage(image);
    updateDB(em, result);

    final Query storedImageQ = em.createNativeQuery(s, OrganizationImage.class);
    @SuppressWarnings("unchecked")
    final
    List<OrganizationImage> result2 = storedImageQ.getResultList();
    final byte[] storedImage = result2.get(0).getImage();
    System.out.println(storedImage.length);
    compareImage(image, storedImage);
    storeImageLocal(storedImage, TEST_IMAGE);

  }

  private void updateDB(final EntityManager em, final List<?> result) {
    em.getTransaction().begin();
    em.persist(result.get(0));
    em.getTransaction().commit();
  }

  private static EntityManager createEntityManager() {
    final Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(ENTITY_MANAGER_DATA_SOURCE, DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.H2));
    final EntityManagerFactory emf = Persistence.createEntityManagerFactory(PUNIT_NAME, properties);
    final EntityManager em = emf.createEntityManager();
    return em;
  }

  private void compareImage(final byte[] image, final byte[] storedImage) {
    if (image.length != storedImage.length) {
      System.out.println("[Image]: length miss match");
    } else {
      for (int i = 0; i < image.length; i++) {
        if (image[i] != storedImage[i]) {
          System.out.println("[Image]: missmatch at" + Integer.toString(i));
          break;
        }
      }
    }
  }

  public void storeImageLocal(final byte[] storedImage, final String fileName) {

    final String home = System.getProperty("java.io.tmpdir");
    final File fileDir = new File(home + File.separator+"olingo-downloads");
    fileDir.mkdirs();
    final File filePath = new File(fileDir, fileName);

    try (OutputStream o = new FileOutputStream(filePath)) {
      o.write(storedImage);
      o.flush();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private byte[] loadImage(final String imageName) {
    final String path = PATH + imageName;
    final URL u = this.getClass().getClassLoader().getResource(path);
    try (InputStream i = u.openStream()) {
      final byte[] image = new byte[i.available()];
      i.read(image);
      return image;
    } catch (final IOException e1) {
      e1.printStackTrace();
    }
    return null;
  }
}
