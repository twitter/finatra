package com.twitter.http.tests.integration.jsonpatch

object JsonPatchTestSuite {
  // all test cases with disabled=true have been manually removed (finatraObjectMapper refuses to parse objects with duplicate fields for example)
  // also removed this --v, because it tests the case class mapping, not the json patch implementation itself
  //  { "comment": "unrecognized op should fail",
  //    "doc": {"foo": 1},
  //    "patch": [{"op": "spam", "path": "/foo", "value": 1}],
  //    "error": "Unrecognized op 'spam'" },

  // files downloaded Fri 17. Mar, git sha: aa68c84fb12767e1388935c8bd1984d5960bde30
  // https://github.com/json-patch/json-patch-tests/blob/master/tests.json
  val tests: String = """
[
    { "comment": "empty list, empty docs",
      "doc": {},
      "patch": [],
      "expected": {} },

    { "comment": "empty patch list",
      "doc": {"foo": 1},
      "patch": [],
      "expected": {"foo": 1} },

    { "comment": "rearrangements OK?",
      "doc": {"foo": 1, "bar": 2},
      "patch": [],
      "expected": {"bar":2, "foo": 1} },

    { "comment": "rearrangements OK?  How about one level down ... array",
      "doc": [{"foo": 1, "bar": 2}],
      "patch": [],
      "expected": [{"bar":2, "foo": 1}] },

    { "comment": "rearrangements OK?  How about one level down...",
      "doc": {"foo":{"foo": 1, "bar": 2}},
      "patch": [],
      "expected": {"foo":{"bar":2, "foo": 1}} },

    { "comment": "add replaces any existing field",
      "doc": {"foo": null},
      "patch": [{"op": "add", "path": "/foo", "value":1}],
      "expected": {"foo": 1} },

    { "comment": "toplevel array",
      "doc": [],
      "patch": [{"op": "add", "path": "/0", "value": "foo"}],
      "expected": ["foo"] },

    { "comment": "toplevel array, no change",
      "doc": ["foo"],
      "patch": [],
      "expected": ["foo"] },

    { "comment": "toplevel object, numeric string",
      "doc": {},
      "patch": [{"op": "add", "path": "/foo", "value": "1"}],
      "expected": {"foo":"1"} },

    { "comment": "toplevel object, integer",
      "doc": {},
      "patch": [{"op": "add", "path": "/foo", "value": 1}],
      "expected": {"foo":1} },

    { "comment": "Add, / target",
      "doc": {},
      "patch": [ {"op": "add", "path": "/", "value":1 } ],
      "expected": {"":1} },

    { "comment": "Add, /foo/ deep target (trailing slash)",
      "doc": {"foo": {}},
      "patch": [ {"op": "add", "path": "/foo/", "value":1 } ],
      "expected": {"foo":{"": 1}} },

    { "comment": "Add composite value at top level",
      "doc": {"foo": 1},
      "patch": [{"op": "add", "path": "/bar", "value": [1, 2]}],
      "expected": {"foo": 1, "bar": [1, 2]} },

    { "comment": "Add into composite value",
      "doc": {"foo": 1, "baz": [{"qux": "hello"}]},
      "patch": [{"op": "add", "path": "/baz/0/foo", "value": "world"}],
      "expected": {"foo": 1, "baz": [{"qux": "hello", "foo": "world"}]} },

    { "doc": {"bar": [1, 2]},
      "patch": [{"op": "add", "path": "/bar/8", "value": "5"}],
      "error": "Out of bounds (upper)" },

    { "doc": {"bar": [1, 2]},
      "patch": [{"op": "add", "path": "/bar/-1", "value": "5"}],
      "error": "Out of bounds (lower)" },

    { "doc": {"foo": 1},
      "patch": [{"op": "add", "path": "/bar", "value": true}],
      "expected": {"foo": 1, "bar": true} },

    { "doc": {"foo": 1},
      "patch": [{"op": "add", "path": "/bar", "value": false}],
      "expected": {"foo": 1, "bar": false} },

    { "doc": {"foo": 1},
      "patch": [{"op": "add", "path": "/bar", "value": null}],
      "expected": {"foo": 1, "bar": null} },

    { "comment": "0 can be an array index or object element name",
      "doc": {"foo": 1},
      "patch": [{"op": "add", "path": "/0", "value": "bar"}],
      "expected": {"foo": 1, "0": "bar" } },

    { "doc": ["foo"],
      "patch": [{"op": "add", "path": "/1", "value": "bar"}],
      "expected": ["foo", "bar"] },

    { "doc": ["foo", "sil"],
      "patch": [{"op": "add", "path": "/1", "value": "bar"}],
      "expected": ["foo", "bar", "sil"] },

    { "doc": ["foo", "sil"],
      "patch": [{"op": "add", "path": "/0", "value": "bar"}],
      "expected": ["bar", "foo", "sil"] },

    { "comment": "push item to array via last index + 1",
      "doc": ["foo", "sil"],
      "patch": [{"op":"add", "path": "/2", "value": "bar"}],
      "expected": ["foo", "sil", "bar"] },

    { "comment": "add item to array at index > length should fail",
      "doc": ["foo", "sil"],
      "patch": [{"op":"add", "path": "/3", "value": "bar"}],
      "error": "index is greater than number of items in array" },

    { "comment": "test against implementation-specific numeric parsing",
      "doc": {"1e0": "foo"},
      "patch": [{"op": "test", "path": "/1e0", "value": "foo"}],
      "expected": {"1e0": "foo"} },

    { "comment": "test with bad number should fail",
      "doc": ["foo", "bar"],
      "patch": [{"op": "test", "path": "/1e0", "value": "bar"}],
      "error": "test op shouldn't get array element 1" },

    { "doc": ["foo", "sil"],
      "patch": [{"op": "add", "path": "/bar", "value": 42}],
      "error": "Object operation on array target" },

    { "doc": ["foo", "sil"],
      "patch": [{"op": "add", "path": "/1", "value": ["bar", "baz"]}],
      "expected": ["foo", ["bar", "baz"], "sil"],
      "comment": "value in array add not flattened" },

    { "doc": {"foo": 1, "bar": [1, 2, 3, 4]},
      "patch": [{"op": "remove", "path": "/bar"}],
      "expected": {"foo": 1} },

    { "doc": {"foo": 1, "baz": [{"qux": "hello"}]},
      "patch": [{"op": "remove", "path": "/baz/0/qux"}],
      "expected": {"foo": 1, "baz": [{}]} },

    { "doc": {"foo": 1, "baz": [{"qux": "hello"}]},
      "patch": [{"op": "replace", "path": "/foo", "value": [1, 2, 3, 4]}],
      "expected": {"foo": [1, 2, 3, 4], "baz": [{"qux": "hello"}]} },

    { "doc": {"foo": [1, 2, 3, 4], "baz": [{"qux": "hello"}]},
      "patch": [{"op": "replace", "path": "/baz/0/qux", "value": "world"}],
      "expected": {"foo": [1, 2, 3, 4], "baz": [{"qux": "world"}]} },

    { "doc": ["foo"],
      "patch": [{"op": "replace", "path": "/0", "value": "bar"}],
      "expected": ["bar"] },

    { "doc": [""],
      "patch": [{"op": "replace", "path": "/0", "value": 0}],
      "expected": [0] },

    { "doc": [""],
      "patch": [{"op": "replace", "path": "/0", "value": true}],
      "expected": [true] },

    { "doc": [""],
      "patch": [{"op": "replace", "path": "/0", "value": false}],
      "expected": [false] },

    { "doc": [""],
      "patch": [{"op": "replace", "path": "/0", "value": null}],
      "expected": [null] },

    { "doc": ["foo", "sil"],
      "patch": [{"op": "replace", "path": "/1", "value": ["bar", "baz"]}],
      "expected": ["foo", ["bar", "baz"]],
      "comment": "value in array replace not flattened" },

    { "comment": "replace whole document",
      "doc": {"foo": "bar"},
      "patch": [{"op": "replace", "path": "", "value": {"baz": "qux"}}],
      "expected": {"baz": "qux"} },

    { "comment": "spurious patch properties",
      "doc": {"foo": 1},
      "patch": [{"op": "test", "path": "/foo", "value": 1, "spurious": 1}],
      "expected": {"foo": 1} },

    { "doc": {"foo": null},
      "patch": [{"op": "test", "path": "/foo", "value": null}],
      "comment": "null value should be valid obj property" },

    { "doc": {"foo": null},
      "patch": [{"op": "replace", "path": "/foo", "value": "truthy"}],
      "expected": {"foo": "truthy"},
      "comment": "null value should be valid obj property to be replaced with something truthy" },

    { "doc": {"foo": null},
      "patch": [{"op": "move", "from": "/foo", "path": "/bar"}],
      "expected": {"bar": null},
      "comment": "null value should be valid obj property to be moved" },

    { "doc": {"foo": null},
      "patch": [{"op": "copy", "from": "/foo", "path": "/bar"}],
      "expected": {"foo": null, "bar": null},
      "comment": "null value should be valid obj property to be copied" },

    { "doc": {"foo": null},
      "patch": [{"op": "remove", "path": "/foo"}],
      "expected": {},
      "comment": "null value should be valid obj property to be removed" },

    { "doc": {"foo": "bar"},
      "patch": [{"op": "replace", "path": "/foo", "value": null}],
      "expected": {"foo": null},
      "comment": "null value should still be valid obj property replace other value" },

    { "doc": {"foo": {"foo": 1, "bar": 2}},
      "patch": [{"op": "test", "path": "/foo", "value": {"bar": 2, "foo": 1}}],
      "comment": "test should pass despite rearrangement" },

    { "doc": {"foo": [{"foo": 1, "bar": 2}]},
      "patch": [{"op": "test", "path": "/foo", "value": [{"bar": 2, "foo": 1}]}],
      "comment": "test should pass despite (nested) rearrangement" },

    { "doc": {"foo": {"bar": [1, 2, 5, 4]}},
      "patch": [{"op": "test", "path": "/foo", "value": {"bar": [1, 2, 5, 4]}}],
      "comment": "test should pass - no error" },

    { "doc": {"foo": {"bar": [1, 2, 5, 4]}},
      "patch": [{"op": "test", "path": "/foo", "value": [1, 2]}],
      "error": "test op should fail" },

    { "comment": "Empty-string element",
      "doc": { "": 1 },
      "patch": [{"op": "test", "path": "/", "value": 1}] },

    { "doc": {
            "foo": ["bar", "baz"],
            "": 0,
            "a/b": 1,
            "c%d": 2,
            "e^f": 3,
            "g|h": 4,
            "i\\j": 5,
            "k\"l": 6,
            " ": 7,
            "m~n": 8
            },
      "patch": [{"op": "test", "path": "/foo", "value": ["bar", "baz"]},
                {"op": "test", "path": "/foo/0", "value": "bar"},
                {"op": "test", "path": "/", "value": 0},
                {"op": "test", "path": "/a~1b", "value": 1},
                {"op": "test", "path": "/c%d", "value": 2},
                {"op": "test", "path": "/e^f", "value": 3},
                {"op": "test", "path": "/g|h", "value": 4},
                {"op": "test", "path":  "/i\\j", "value": 5},
                {"op": "test", "path": "/k\"l", "value": 6},
                {"op": "test", "path": "/ ", "value": 7},
                {"op": "test", "path": "/m~0n", "value": 8}] },

    { "comment": "Move to same location has no effect",
      "doc": {"foo": 1},
      "patch": [{"op": "move", "from": "/foo", "path": "/foo"}],
      "expected": {"foo": 1} },

    { "doc": {"foo": 1, "baz": [{"qux": "hello"}]},
      "patch": [{"op": "move", "from": "/foo", "path": "/bar"}],
      "expected": {"baz": [{"qux": "hello"}], "bar": 1} },

    { "doc": {"baz": [{"qux": "hello"}], "bar": 1},
      "patch": [{"op": "move", "from": "/baz/0/qux", "path": "/baz/1"}],
      "expected": {"baz": [{}, "hello"], "bar": 1} },

    { "doc": {"baz": [{"qux": "hello"}], "bar": 1},
      "patch": [{"op": "copy", "from": "/baz/0", "path": "/boo"}],
      "expected": {"baz":[{"qux":"hello"}],"bar":1,"boo":{"qux":"hello"}} },

    { "comment": "replacing the root of the document is possible with add",
      "doc": {"foo": "bar"},
      "patch": [{"op": "add", "path": "", "value": {"baz": "qux"}}],
      "expected": {"baz":"qux"}},

    { "comment": "Adding to \"/-\" adds to the end of the array",
      "doc": [ 1, 2 ],
      "patch": [ { "op": "add", "path": "/-", "value": { "foo": [ "bar", "baz" ] } } ],
      "expected": [ 1, 2, { "foo": [ "bar", "baz" ] } ]},

    { "comment": "Adding to \"/-\" adds to the end of the array, even n levels down",
      "doc": [ 1, 2, [ 3, [ 4, 5 ] ] ],
      "patch": [ { "op": "add", "path": "/2/1/-", "value": { "foo": [ "bar", "baz" ] } } ],
      "expected": [ 1, 2, [ 3, [ 4, 5, { "foo": [ "bar", "baz" ] } ] ] ]},

    { "comment": "test remove with bad number should fail",
      "doc": {"foo": 1, "baz": [{"qux": "hello"}]},
      "patch": [{"op": "remove", "path": "/baz/1e0/qux"}],
      "error": "remove op shouldn't remove from array with bad number" },

    { "comment": "test remove on array",
      "doc": [1, 2, 3, 4],
      "patch": [{"op": "remove", "path": "/0"}],
      "expected": [2, 3, 4] },

    { "comment": "test repeated removes",
      "doc": [1, 2, 3, 4],
      "patch": [{ "op": "remove", "path": "/1" },
                { "op": "remove", "path": "/2" }],
      "expected": [1, 3] },

    { "comment": "test remove with bad index should fail",
      "doc": [1, 2, 3, 4],
      "patch": [{"op": "remove", "path": "/1e0"}],
      "error": "remove op shouldn't remove from array with bad number" },

    { "comment": "test replace with bad number should fail",
      "doc": [""],
      "patch": [{"op": "replace", "path": "/1e0", "value": false}],
      "error": "replace op shouldn't replace in array with bad number" },

    { "comment": "test copy with bad number should fail",
      "doc": {"baz": [1,2,3], "bar": 1},
      "patch": [{"op": "copy", "from": "/baz/1e0", "path": "/boo"}],
      "error": "copy op shouldn't work with bad number" },

    { "comment": "test move with bad number should fail",
      "doc": {"foo": 1, "baz": [1,2,3,4]},
      "patch": [{"op": "move", "from": "/baz/1e0", "path": "/foo"}],
      "error": "move op shouldn't work with bad number" },

    { "comment": "test add with bad number should fail",
      "doc": ["foo", "sil"],
      "patch": [{"op": "add", "path": "/1e0", "value": "bar"}],
      "error": "add op shouldn't add to array with bad number" },

    { "comment": "missing 'value' parameter to add",
      "doc": [ 1 ],
      "patch": [ { "op": "add", "path": "/-" } ],
      "error": "missing 'value' parameter" },

    { "comment": "missing 'value' parameter to replace",
      "doc": [ 1 ],
      "patch": [ { "op": "replace", "path": "/0" } ],
      "error": "missing 'value' parameter" },

    { "comment": "missing 'value' parameter to test",
      "doc": [ null ],
      "patch": [ { "op": "test", "path": "/0" } ],
      "error": "missing 'value' parameter" },

    { "comment": "missing value parameter to test - where undef is falsy",
      "doc": [ false ],
      "patch": [ { "op": "test", "path": "/0" } ],
      "error": "missing 'value' parameter" },

    { "comment": "missing from parameter to copy",
      "doc": [ 1 ],
      "patch": [ { "op": "copy", "path": "/-" } ],
      "error": "missing 'from' parameter" },

    { "comment": "missing from parameter to move",
      "doc": { "foo": 1 },
      "patch": [ { "op": "move", "path": "" } ],
      "error": "missing 'from' parameter" },

    { "comment": "test with bad array number that has leading zeros",
      "doc": ["foo", "bar"],
      "patch": [{"op": "test", "path": "/00", "value": "foo"}],
      "error": "test op should reject the array value, it has leading zeros" },

    { "comment": "test with bad array number that has leading zeros",
      "doc": ["foo", "bar"],
      "patch": [{"op": "test", "path": "/01", "value": "bar"}],
      "error": "test op should reject the array value, it has leading zeros" }

]

"""

  //https://github.com/json-patch/json-patch-tests/blob/master/spec_tests.json
  val specTests: String = """
[
  {
    "comment": "4.1. add with missing object",
    "doc": { "q": { "bar": 2 } },
    "patch": [ {"op": "add", "path": "/a/b", "value": 1} ],
    "error":
       "path /a does not exist -- missing objects are not created recursively"
  },

  {
    "comment": "A.1.  Adding an Object Member",
    "doc": {
  "foo": "bar"
},
    "patch": [
  { "op": "add", "path": "/baz", "value": "qux" }
],
    "expected": {
  "baz": "qux",
  "foo": "bar"
}
  },

  {
    "comment": "A.2.  Adding an Array Element",
    "doc": {
  "foo": [ "bar", "baz" ]
},
    "patch": [
  { "op": "add", "path": "/foo/1", "value": "qux" }
],
    "expected": {
  "foo": [ "bar", "qux", "baz" ]
}
  },

  {
    "comment": "A.3.  Removing an Object Member",
    "doc": {
  "baz": "qux",
  "foo": "bar"
},
    "patch": [
  { "op": "remove", "path": "/baz" }
],
    "expected": {
  "foo": "bar"
}
  },

  {
    "comment": "A.4.  Removing an Array Element",
    "doc": {
  "foo": [ "bar", "qux", "baz" ]
},
    "patch": [
  { "op": "remove", "path": "/foo/1" }
],
    "expected": {
  "foo": [ "bar", "baz" ]
}
  },

  {
    "comment": "A.5.  Replacing a Value",
    "doc": {
  "baz": "qux",
  "foo": "bar"
},
    "patch": [
  { "op": "replace", "path": "/baz", "value": "boo" }
],
    "expected": {
  "baz": "boo",
  "foo": "bar"
}
  },

  {
    "comment": "A.6.  Moving a Value",
    "doc": {
  "foo": {
    "bar": "baz",
    "waldo": "fred"
  },
  "qux": {
    "corge": "grault"
  }
},
    "patch": [
  { "op": "move", "from": "/foo/waldo", "path": "/qux/thud" }
],
    "expected": {
  "foo": {
    "bar": "baz"
  },
  "qux": {
    "corge": "grault",
    "thud": "fred"
  }
}
  },

  {
    "comment": "A.7.  Moving an Array Element",
    "doc": {
  "foo": [ "all", "grass", "cows", "eat" ]
},
    "patch": [
  { "op": "move", "from": "/foo/1", "path": "/foo/3" }
],
    "expected": {
  "foo": [ "all", "cows", "eat", "grass" ]
}

  },

  {
    "comment": "A.8.  Testing a Value: Success",
    "doc": {
  "baz": "qux",
  "foo": [ "a", 2, "c" ]
},
    "patch": [
  { "op": "test", "path": "/baz", "value": "qux" },
  { "op": "test", "path": "/foo/1", "value": 2 }
],
    "expected": {
     "baz": "qux",
     "foo": [ "a", 2, "c" ]
    }
  },

  {
    "comment": "A.9.  Testing a Value: Error",
    "doc": {
  "baz": "qux"
},
    "patch": [
  { "op": "test", "path": "/baz", "value": "bar" }
],
    "error": "string not equivalent"
  },

  {
    "comment": "A.10.  Adding a nested Member Object",
    "doc": {
  "foo": "bar"
},
    "patch": [
  { "op": "add", "path": "/child", "value": { "grandchild": { } } }
],
    "expected": {
  "foo": "bar",
  "child": {
    "grandchild": {
    }
  }
}
  },

  {
    "comment": "A.11.  Ignoring Unrecognized Elements",
    "doc": {
  "foo":"bar"
},
    "patch": [
  { "op": "add", "path": "/baz", "value": "qux", "xyz": 123 }
],
    "expected": {
  "foo":"bar",
  "baz":"qux"
}
  },

 {
    "comment": "A.12.  Adding to a Non-existent Target",
    "doc": {
  "foo": "bar"
},
    "patch": [
  { "op": "add", "path": "/baz/bat", "value": "qux" }
],
    "error": "add to a non-existent target"
  },

  {
    "comment": "A.14. ~ Escape Ordering",
    "doc": {
       "/": 9,
       "~1": 10
    },
    "patch": [{"op": "test", "path": "/~01", "value": 10}],
    "expected": {
       "/": 9,
       "~1": 10
    }
  },

  {
    "comment": "A.15. Comparing Strings and Numbers",
    "doc": {
       "/": 9,
       "~1": 10
    },
    "patch": [{"op": "test", "path": "/~01", "value": "10"}],
    "error": "number is not equal to string"
  },

  {
    "comment": "A.16. Adding an Array Value",
    "doc": {
       "foo": ["bar"]
    },
    "patch": [{ "op": "add", "path": "/foo/-", "value": ["abc", "def"] }],
    "expected": {
      "foo": ["bar", ["abc", "def"]]
    }
  }

]
"""
}
