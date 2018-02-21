package com.twitter.inject.tests.utils

import com.twitter.inject.Test
import com.twitter.inject.utils.StringUtils._

class StringUtilsTest extends Test {

  test("StringUtils#camelify") {
    camelify("foo_bar") should equal("fooBar")
    pascalify("foo_bar") should equal("FooBar")

    camelify("foo__bar") should equal("fooBar")
    pascalify("foo__bar") should equal("FooBar")

    camelify("foo___bar") should equal("fooBar")
    pascalify("foo___bar") should equal("FooBar")

    camelify("_foo_bar") should equal("fooBar")
    pascalify("_foo_bar") should equal("FooBar")

    camelify("foo_bar_") should equal("fooBar")
    pascalify("foo_bar_") should equal("FooBar")

    camelify("_foo_bar_") should equal("fooBar")
    pascalify("_foo_bar_") should equal("FooBar")

    camelify("a_b_c_d") should equal("aBCD")
    pascalify("a_b_c_d") should equal("ABCD")
  }

  test("StringUtils#camelify return an empty string if given null") {
    pascalify(null) should equal("")
  }

  test("StringUtils#camelify leave a CamelCased name untouched") {
    pascalify("GetTweets") should equal("GetTweets")
    pascalify("FooBar") should equal("FooBar")
    pascalify("HTML") should equal("HTML")
    pascalify("HTML5") should equal("HTML5")
    pascalify("Editor2TOC") should equal("Editor2TOC")
  }

  test("StringUtils#camelifyMethod") {
    camelify("GetTweets") should equal("getTweets")
    camelify("FooBar") should equal("fooBar")
    camelify("HTML") should equal("hTML")
    camelify("HTML5") should equal("hTML5")
    camelify("Editor2TOC") should equal("editor2TOC")
  }

  test("StringUtils#camelify Method function") {
    // test both produce empty strings
    pascalify(null).isEmpty && camelify(null).isEmpty

    val name = "emperor_norton"
    // test that the first letter for camelifyMethod is lower-cased
    camelify(name).toList.head.isLower should be(true)
    // test that camelify and camelifyMethod upper-casing first letter are the same
    pascalify(name) should equal(camelify(name).capitalize)
  }

  test("StringUtils#snakify replace upper case with underscore") {
    snakify("MyCamelCase") should equal("my_camel_case")
    snakify("CamelCase") should equal("camel_case")
    snakify("Camel") should equal("camel")
    snakify("MyCamel12Case") should equal("my_camel12_case")
    snakify("CamelCase12") should equal("camel_case12")
    snakify("Camel12") should equal("camel12")
    snakify("Foobar") should equal("foobar")
  }

  test("StringUtils#snakify not modify existing snake case strings") {
    snakify("my_snake_case") should equal("my_snake_case")
    snakify("snake") should equal("snake")
  }

  test("StringUtils#snakify handle abbeviations") {
    snakify("ABCD") should equal("abcd")
    snakify("HTML") should equal("html")
    snakify("HTMLEditor") should equal("html_editor")
    snakify("EditorTOC") should equal("editor_toc")
    snakify("HTMLEditorTOC") should equal("html_editor_toc")

    snakify("HTML5") should equal("html5")
    snakify("HTML5Editor") should equal("html5_editor")
    snakify("Editor2TOC") should equal("editor2_toc")
    snakify("HTML5Editor2TOC") should equal("html5_editor2_toc")
  }

}
