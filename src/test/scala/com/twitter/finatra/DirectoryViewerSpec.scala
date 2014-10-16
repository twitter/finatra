package com.twitter.finatra

import com.twitter.finatra.test.ShouldSpec

class DirectoryViewerSpec extends ShouldSpec {

  "DirectoryViewer" should "not crash under normal conditions" in {
    try {
      val file = FileResolver.getLocalDirectory("public/components").get
    }catch {
      case e : Throwable => fail(e.getMessage)
    }
  }


  it should "crash under abnormal conditions" in {
    try{
      val file = FileResolver.getLocalDirectory("/public/components/imaginary/folder").get
      fail("getLocalDirectory should have failed")
    }catch{
      case e : Throwable =>
    }
  }

  it should "create a correct ListingView" in {
    val listView = DirectoryViewer.renderView(FileResolver.getLocalDirectory("public/components").get)

    listView.root should be(DirectoryViewer.FileView("/components", "components"))

    listView.files.toSet should be(Set(DirectoryViewer.FileView("/components/bootstrap.css", "bootstrap.css"), DirectoryViewer.FileView("/components/test_file.txt", "test_file.txt")))
    listView.directories.toSet should be(Set(DirectoryViewer.FileView("/components/test_folder", "test_folder")))
  }
}
