package com.twitter.finatra.utils.tests;

import java.io.File;
import java.io.InputStream;

import scala.Option;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

import org.junit.Test;

import com.twitter.finatra.test.AbstractTempFolder;
import com.twitter.finatra.test.LocalFilesystemTestUtils;
import com.twitter.finatra.utils.AutoClosable;
import com.twitter.finatra.utils.FileResolver;
import com.twitter.finatra.utils.FileResolvers;
import com.twitter.io.StreamIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileResolversTest extends AbstractTempFolder {

  private static final String TEST_FILE_TEXT = "testfile123";
  private static final String TEST_INDEX_HTML_TEXT = "testindex";

  @Test
  public void testApply1() {
    /* local resolver */

    withTempFolder(new AbstractFunction0<Object>() {
      @Override
      public Object apply() {
      setup(folderName());

      FileResolver resolver =
        FileResolvers.newLocalResolver(documentRoot(folderName()));

      Option<String> bodyOption =
        resolver
          .getInputStream("/testfile.txt")
          .map(
            new AbstractFunction1<InputStream, String>() {
              @Override
              public String apply(InputStream inputStream) {
                return body(inputStream);
              }
            });

      assertTrue(bodyOption.isDefined());
      assertEquals(TEST_FILE_TEXT, bodyOption.get());

      bodyOption =
        resolver
          .getInputStream("/testindex.html")
          .map(
            new AbstractFunction1<InputStream, String>() {
              @Override
              public String apply(InputStream inputStream) {
                return body(inputStream);
              }
            });

      assertTrue(bodyOption.isDefined());
      assertEquals(TEST_INDEX_HTML_TEXT, bodyOption.get());

      return BoxedUnit.UNIT;
      }
    });
  }

  @Test
  public void testApply2() {
    /* classpath resolver */

    withTempFolder(new AbstractFunction0<Object>() {
      @Override
      public Object apply() {
      FileResolver resolver =
        FileResolvers.newResolver("");

      Option<String> bodyOption =
        resolver
          .getInputStream("/foo.txt")
          .map(
            new AbstractFunction1<InputStream, String>() {
              @Override
              public String apply(InputStream inputStream) {
                return body(inputStream);
              }
            });

      assertTrue(bodyOption.isDefined());
      assertEquals("foo\nbar\n", bodyOption.get());

      bodyOption =
        resolver
          .getInputStream("/app/docs.txt")
          .map(
            new AbstractFunction1<InputStream, String>() {
              @Override
              public String apply(InputStream inputStream) {
                return body(inputStream);
              }
            });

      assertTrue(bodyOption.isDefined());
      assertEquals("documents\n", bodyOption.get());

      bodyOption =
        resolver
          .getInputStream("/index.html")
          .map(
            new AbstractFunction1<InputStream, String>() {
              @Override
              public String apply(InputStream inputStream) {
                return body(inputStream);
              }
            });

      assertTrue(bodyOption.isDefined());
      assertEquals("<h1>INDEX</h1>\n", bodyOption.get());

      return BoxedUnit.UNIT;
      }
    });
  }

  private void setup(String folderName) {
    // create src/main/webapp directory and add files
    File webAppDirectory = LocalFilesystemTestUtils.createFile(documentRoot(folderName));
    LocalFilesystemTestUtils
      .writeStringToFile(
        LocalFilesystemTestUtils
          .createFile(webAppDirectory, "testfile.txt"),
        TEST_FILE_TEXT);
    LocalFilesystemTestUtils
      .writeStringToFile(
        LocalFilesystemTestUtils
          .createFile(webAppDirectory, "testindex.html"),
        TEST_INDEX_HTML_TEXT);
  }

  private String documentRoot(String folderName) {
    return folderName + "src/main/webapp";
  }

  private String body(InputStream inputStream) {
    return AutoClosable.tryWith(inputStream, new AbstractFunction1<InputStream, String>() {
      @Override
      public String apply(InputStream v1) {
        return StreamIO.buffer(v1).toString();
      }
    });
  }
}
