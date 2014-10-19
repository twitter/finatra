/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
