package com.posterous.finatra

import com.github.mustachejava._
import com.twitter.io.TempFile
import com.twitter.mustache._

import java.io.IOException
import java.io.StringWriter
import java.io.File
import java.io.Writer
import java.util.Arrays
import java.util.List

class LayoutHelperFactory {
  def apply(yld: String) = {
    new LayoutHelper(yld)
  }
}

class LayoutHelper(yld: String) {
  def render = yld
}

class FinatraMustacheFactory extends DefaultMustacheFactory {
  override def encode(str: String, wtr: Writer) {
    wtr.append(str, 0, str.length)
  }
}

class TemplateHandler {

  def captureTemplate(template: String, layout: String, exports: Any): String  = {
    val tpath = TempFile.fromResourcePath("/templates/" + template)
    val lpath = TempFile.fromResourcePath("/templates/layouts/" + layout)

    //what remembers the templates
    val mf = new FinatraMustacheFactory

    mf.setObjectHandler(new TwitterObjectHandler)

    if(tpath.exists){

      val compiledTemplate = mf.compile(tpath.toString)
      var output = new StringWriter
      compiledTemplate.execute(output, exports).flush()
      val templateRender = output.toString
      var layoutRender = ""

      if(lpath.exists){
        val compiledLayout = mf.compile(lpath.toString)
        output = new StringWriter
        compiledLayout.execute(output, FinatraServer.layoutHelperFactory(templateRender)).flush()
        layoutRender = output.toString
      } else {
        layoutRender = templateRender
      }

      layoutRender
    } else {
      throw new IllegalArgumentException("Template file not found")
    }
  }

}

