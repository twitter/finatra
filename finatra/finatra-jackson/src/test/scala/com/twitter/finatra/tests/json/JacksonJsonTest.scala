package com.twitter.finatra.tests.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.twitter.finatra.conversions.json._
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.json.annotations._
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonFieldParseException, JsonInjectionNotSupportedException, JsonObjectParseException}
import com.twitter.finatra.json.internal.caseclass.wrapped.JsonWrappedValue
import com.twitter.finatra.json.{FinatraObjectMapper, JsonDiff}
import com.twitter.finatra.tests.json.internal.Obj.NestedCaseClassInObject
import com.twitter.finatra.tests.json.internal._
import com.twitter.finatra.utils.Logging
import java.io.ByteArrayInputStream
import org.joda.time.{DateTime, DateTimeZone}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FeatureSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class JacksonJsonTest extends FeatureSpec with Matchers with Logging {

  DateTimeZone.setDefault(DateTimeZone.UTC)

  /* Class under test */
  val mapper = FinatraObjectMapper.create()

  feature("simple tests") {

    scenario("parse super simple") {
      val foo = parse[SimplePerson]( """{"name": "Steve"}""")
      foo should equal(SimplePerson("Steve"))
    }

    val steve = Person(
      id = 1,
      name = "Steve",
      age = Some(20),
      age_with_default = Some(20),
      nickname = "ace")

    val steveJson =
      """{
         "id" : 1,
         "name" : "Steve",
         "age" : 20,
         "age_with_default" : 20,
         "nickname" : "ace"
       }
      """
    scenario("parse simple") {
      val foo = parse[SimplePerson]( """{"name": "Steve"}""")
      foo should equal(SimplePerson("Steve"))
    }

    scenario("parse json") {
      val person = parse[Person](steveJson)
      person should equal(steve)
    }

    scenario("parse json list of objects") {
      val json = Seq(steveJson, steveJson).mkString("[", ", ", "]")
      val persons = parse[Seq[Person]](json)
      persons should equal(Seq(steve, steve))
    }

    scenario("parse json list of ints") {
      val nums = parse[Seq[Int]]( """[1,2,3]""")
      nums should equal(Seq(1, 2, 3))
    }

    scenario("parse json with extra field at end") {
      val person = parse[Person]( """
      {
         "id" : 1,
         "name" : "Steve",
         "age" : 20,
         "age_with_default" : 20,
         "nickname" : "ace",
         "extra" : "extra"
       }
                                  """
      )
      person should equal(steve)
    }

    scenario("parse json with extra field in middle") {
      val person = parse[Person]( """
      {
         "id" : 1,
         "name" : "Steve",
         "age" : 20,
         "extra" : "extra",
         "age_with_default" : 20,
         "nickname" : "ace"
       }
                                  """
      )
      person should equal(steve)
    }

    scenario("parse json with missing 'id' and 'name' field and invalid age field") {
      assertJsonParse[Person](
        """ {
               "age" : "foo",
               "age_with_default" : 20,
               "nickname" : "ace"
            }""",
        withErrors = Seq(
          "id is a required field",
          "name is a required field",
          "age's value 'foo' is not a valid int"))
    }

    scenario("parse nested json with missing fields") {
      assertJsonParse[Car](
        """
         {
          "id" : 0,
          "make": "Foo",
          "year": 2000,
          "passengers" : [ { "id": "-1", "age": "blah" } ]
         }
        """,
        withErrors = Seq(
          "make's value 'Foo' is not a valid CarMake with valid values: Ford, Honda",
          "model is a required field",
          "passengers.name is a required field",
          "passengers.age's value 'blah' is not a valid int"))
    }

    scenario("parse json with missing 'nickname' field that has a string default") {
      val person = parse[Person]( """
      {
         "id" : 1,
         "name" : "Steve",
         "age" : 20,
         "age_with_default" : 20
       }""")
      person should equal(steve.copy(nickname = "unknown"))
    }

    scenario("parse json with missing 'age' field that is an Option without a default should succeed") {
      parse[Person](
        """
          {
             "id" : 1,
             "name" : "Steve",
             "age_with_default" : 20,
             "nickname" : "bob"
           }
        """)
    }

    scenario("parse json into JsonNode") {
      val person = parse[JsonNode](steveJson)
      trace(person)
    }

    scenario("generate json") {
      val json = generate(steve)
      json.toPrettyJson should equal(steveJson.toPrettyJson)
    }

    scenario("generate then parse") {
      val json = generate(steve)
      val person = parse[Person](json)
      person should equal(steve)
    }

    scenario("generate then parse nested case class") {
      val origCar = Car(1, CarMake.Ford, "Explorer", Seq(steve, steve))
      val carJson = generate(origCar)
      val car = parse[Car](carJson)
      car should equal(origCar)
    }

    scenario("Prevent overrwriting val in case class") {
      parse[CaseClassWithVal]( """{
          "name" : "Bob",
          "type" : "dog"
         }""") should equal(CaseClassWithVal("Bob"))
    }
  }

  feature("enums") {
    scenario("simple") {
      parse[CaseClassWithEnum]( """{
          "name" : "Bob",
          "make" : "ford"
         }
                                """) should equal(CaseClassWithEnum("Bob", CarMakeEnum.ford))
    }

    scenario("complex") {
      JsonDiff.jsonDiff(
        parse[CaseClassWithComplexEnums]( """{
          "name" : "Bob",
          "make" : "vw",
          "make_opt" : "ford",
          "make_seq" : ["vw", "ford"],
          "make_set" : ["ford", "vw"]
         }"""),
        CaseClassWithComplexEnums(
          "Bob",
          CarMakeEnum.vw,
          Some(CarMakeEnum.ford),
          Seq(CarMakeEnum.vw, CarMakeEnum.ford),
          Set(CarMakeEnum.ford, CarMakeEnum.vw)))
    }

    scenario("invalid enum entry") {
      val e = intercept[JsonObjectParseException] {
        parse[CaseClassWithEnum]( """{
          "name" : "Bob",
          "make" : "foo"
         }""")
      }
      e.fieldErrors map {_.msg} should equal(Seq( """make's value 'foo' is not a valid CarMakeEnum with valid values: ford, vw"""))
    }
  }

  feature("Jodatime") {
    scenario("DateTime") {
      DateTime.now < DateTime.now //including so that import com.twitter.finatra.conversions.time._ is not removed (since there was a previous bug where _time included a DateTime type alias)
      parse[CaseClassWithDateTime]( """{
           "date_time" : "2014-05-30T03:57:59.302Z"
         }""") should equal(CaseClassWithDateTime(new DateTime("2014-05-30T03:57:59.302Z", DateTimeZone.UTC)))
    }

    scenario("invalid DateTime") {
      assertJsonParse[CaseClassWithDateTime]( """{
           "date_time" : ""
         }""",
        withErrors = Seq(
          """date_time: field cannot be empty"""))
    }

    scenario("invalid DateTime's") {
      assertJsonParse[CaseClassWithIntAndDateTime]( """{
           "name" : "Bob",
           "age" : "old",
           "age2" : "1",
           "age3" : "",
           "date_time" : "today",
           "date_time2" : "1",
           "date_time3" : -1,
           "date_time4" : ""
         }""",
        withErrors = Seq(
          "age's value 'old' is not a valid int",
          "age3: error parsing ''",
          """date_time: error parsing 'today' into an ISO 8601 datetime""",
          """date_time3: field cannot be negative""",
          """date_time4: field cannot be empty"""))
    }
  }

  feature("escaped fields") {
    scenario("long") {
      parse[CaseClassWithEscapedLong]( """{
          "1-5" : 10
       }""") should equal(CaseClassWithEscapedLong(`1-5` = 10))
    }

    scenario("string") {
      parse[CaseClassWithEscapedString]( """{
          "1-5" : "10"
       }""") should equal(CaseClassWithEscapedString(`1-5` = "10"))
    }

    scenario("non-unicode escaped") {
      parse[CaseClassWithEscapedNormalString](
        """{
            "a" : "foo"
           }""") should equal(CaseClassWithEscapedNormalString("foo"))
    }

    scenario("unicode and non-unicode fields") {
      parse[UnicodeNameCaseClass](
        """{"winning-id":23,"name":"the name of this"}""") should equal(UnicodeNameCaseClass(23, "the name of this"))
    }
  }

  feature("Injection when using FinatraObjectMapper.create") {
    scenario("Inject not found field") {
      intercept[JsonInjectionNotSupportedException] {
        parse[ClassWithFooClassInject]( """{}""")
      }
    }

    scenario("Inject request field") {
      intercept[JsonInjectionNotSupportedException] {
        parse[ClassWithQueryParamDateTimeInject]( """{}""")
      }
    }
  }

  feature("wrapped values") {
    scenario("direct WrappedValue for Int") {
      val origObj = WrappedValueInt(1)
      val obj = parse[WrappedValueInt](generate(origObj))
      origObj should equal(obj)
    }

    scenario("direct WrappedValue for String") {
      val origObj = WrappedValueString("1")
      val obj = parse[WrappedValueString](generate(origObj))
      origObj should equal(obj)
    }

    scenario("direct WrappedValue for Long") {
      val origObj = WrappedValueLong(1)
      val obj = parse[WrappedValueLong](generate(origObj))
      origObj should equal(obj)
    }

    scenario("WrappedValue for Int") {
      val origObj = WrappedValueIntInObj(WrappedValueInt(1))
      val json = origObj.toJson
      val obj = parse[WrappedValueIntInObj](json)
      origObj should equal(obj)
    }

    scenario("WrappedValue for String") {
      val origObj = WrappedValueStringInObj(WrappedValueString("1"))
      val json = origObj.toJson
      val obj = parse[WrappedValueStringInObj](json)
      origObj should equal(obj)
    }

    scenario("WrappedValue for Long") {
      val origObj = WrappedValueLongInObj(WrappedValueLong(11111111))
      val json = origObj.toJson
      val obj = parse[WrappedValueLongInObj](json)
      origObj should equal(obj)
    }

    scenario("Seq[WrappedValue]") {
      generate(
        Seq(WrappedValueLong(11111111))) should be( """[11111111]""")
    }

    scenario("Map[WrappedValueString, String]") {
      val obj = Map(WrappedValueString("11111111") -> "asdf")
      val json = generate(obj)
      json should be( """{"11111111":"asdf"}""")
      parse[Map[WrappedValueString, String]](json) should be(obj)
    }

    scenario("Map[WrappedValueLong, String]") {
      assertJson(
        Map(WrappedValueLong(11111111) -> "asdf"),
        """{"11111111":"asdf"}""")
    }

    scenario("deser Map[Long, String]") {
      pending
      val obj = parse[Map[Long, String]]( """{"11111111":"asdf"}""")
      val expected = Map(11111111L -> "asdf")
      obj should equal(expected)
    }

    scenario("deser Map[String, String]") {
      parse[Map[String, String]]( """{"11111111":"asdf"}""") should
        be(Map("11111111" -> "asdf"))
    }

    scenario("Map[String, WrappedValueLong]") {
      generate(
        Map("asdf" -> WrappedValueLong(11111111))) should be( """{"asdf":11111111}""")
    }

    scenario("Map[String, WrappedValueString]") {
      generate(
        Map("asdf" -> WrappedValueString("11111111"))) should be( """{"asdf":"11111111"}""")
    }

    scenario("object with Map[WrappedValueString, String]") {
      assertJson(
        obj = WrappedValueStringMapObject(Map(WrappedValueString("11111111") -> "asdf")),
        expected =
          """{
                "map" : {
                  "11111111":"asdf"
             }
           }""")
    }
  }

  // ========================================================
  // Jerkson Inspired/Copied Tests Below
  feature("A basic case class") {
    scenario("generates a JSON object with matching field values") {
      generate(CaseClass(1, "Coda")) should be( """{"id":1,"name":"Coda"}""")
    }

    scenario("is parsable from a JSON object with corresponding fields") {
      parse[CaseClass]( """{"id":1,"name":"Coda"}""") should be(CaseClass(1, "Coda"))
    }

    scenario("is parsable from a JSON object with extra fields") {
      parse[CaseClass]( """{"id":1,"name":"Coda","derp":100}""") should be(CaseClass(1, "Coda"))
    }

    scenario("is not parsable from an incomplete JSON object") {
      intercept[Exception] {
        parse[CaseClass]( """{"id":1}""")
      }
    }
  }

  feature("A case class with lazy fields") {
    scenario("generates a JSON object with those fields evaluated") {
      generate(CaseClassWithLazyVal(1)) should be( """{"id":1,"woo":"yeah"}""")
    }

    scenario("is parsable from a JSON object without those fields") {
      parse[CaseClassWithLazyVal]( """{"id":1}""") should be(CaseClassWithLazyVal(1))
    }

    scenario("is not parsable from an incomplete JSON object") {
      intercept[Exception] {
        parse[CaseClassWithLazyVal]( """{}""")
      }
    }
  }

  feature("A case class with ignored members") {
    scenario("generates a JSON object without those fields") {
      generate(CaseClassWithIgnoredField(1)) should be( """{"id":1}""")
      generate(CaseClassWithIgnoredFields(1)) should be( """{"id":1}""")
    }

    scenario("is parsable from a JSON object without those fields") {
      parse[CaseClassWithIgnoredField]( """{"id":1}""") should be(CaseClassWithIgnoredField(1))
      parse[CaseClassWithIgnoredFields]( """{"id":1}""") should be(CaseClassWithIgnoredFields(1))
    }

    scenario("is not parsable from an incomplete JSON object") {
      intercept[Exception] {
        parse[CaseClassWithIgnoredField]( """{}""")
      }
      intercept[Exception] {
        parse[CaseClassWithIgnoredFields]( """{}""")
      }
    }
  }

  feature("A case class with transient members") {
    scenario("generates a JSON object without those fields") {
      generate(CaseClassWithTransientField(1)) should be( """{"id":1}""")
    }

    scenario("is parsable from a JSON object without those fields") {
      parse[CaseClassWithTransientField]( """{"id":1}""") should be(CaseClassWithTransientField(1))
    }

    scenario("is not parsable from an incomplete JSON object") {
      intercept[Exception] {
        parse[CaseClassWithTransientField]( """{}""")
      }
    }
  }

  feature("A case class with lazy vals") {
    scenario("generates a JSON object without those fields") {
      pending // would need to modify case class serializer to implement
      generate(CaseClassWithLazyField(1)) should be( """{"id":1}""")
    }

    scenario("is parsable from a JSON object without those fields") {
      parse[CaseClassWithLazyField]( """{"id":1}""") should be(CaseClassWithLazyField(1))
    }
  }

  feature("A case class with an overloaded field") {
    scenario("generates a JSON object with the nullary version of that field") {
      pending //fails on java 7. ok for now since we don't need this functionality
      generate(CaseClassWithOverloadedField(1)) should be( """{"id":1}""")
    }
  }

  feature("A case class with an Option[String] member") {
    scenario("generates a field if the member is Some") {
      generate(CaseClassWithOption(Some("what"))) should be( """{"value":"what"}""")
    }

    scenario("is parsable from a JSON object with that field") {
      parse[CaseClassWithOption]( """{"value":"what"}""") should be(CaseClassWithOption(Some("what")))
    }

    scenario("doesn't generate a field if the member is None") {
      generate(CaseClassWithOption(None)) should be( """{}""")
    }

    scenario("is parsable from a JSON object without that field") {
      parse[CaseClassWithOption]( """{}""") should be(CaseClassWithOption(None))
    }

    scenario("is parsable from a JSON object with a null value for that field") {
      parse[CaseClassWithOption]( """{"value":null}""") should be(CaseClassWithOption(None))
    }
  }

  feature("A case class with a JsonNode member") {
    scenario("generates a field of the given type") {
      generate(CaseClassWithJsonNode(new IntNode(2))) should be( """{"value":2}""")
    }
  }

  feature("issues") {
    scenario("standalone map") {
      val map = parse[Map[String, String]](
        """
        {
          "one": "two"
        }
        """)
      map should equal(Map("one" -> "two"))
    }

    scenario("case class with map") {
      val obj = parse[CaseClassWithMap](
        """
        {
          "map": {"one": "two"}
        }
        """)
      obj.map should equal(Map("one" -> "two"))
    }

    scenario("case class with multiple constructors") {
      intercept[AssertionError] {
        parse[CaseClassWithTwoConstructors]("{}")
      }
    }

    scenario("case class nested within an object") {
      parse[NestedCaseClassInObject](
        """
        {
          "id": "foo"
        }
        """) should equal(NestedCaseClassInObject(id = "foo"))
    }

    case class NestedCaseClassInClass(id: String)
    scenario("case class nested within a class") {
      intercept[AssertionError] {
        parse[NestedCaseClassInClass](
          """
          {
            "id": "foo"
          }
          """) should equal(NestedCaseClassInClass(id = "foo"))
      }
    }
  }

  feature("Collection of longs") {
    scenario("case class with set of longs") {
      val obj = parse[CaseClassWithSetOfLongs](
        """
        {
          "set": [5000000, 1, 2, 3, 1000]
        }
        """)
      obj.set.toSeq.sorted should equal(Seq(1L, 2L, 3L, 1000L, 5000000L))
    }

    scenario("case class with seq of longs") {
      val obj = parse[CaseClassWithSeqOfLongs](
        """
        {
          "seq": [%s]
        }
        """.format(1004 to 1500 mkString ","))
      obj.seq.sorted should equal(1004 to 1500)
    }

    scenario("nested case class with collection of longs") {
      val idsStr = 1004 to 1500 mkString ","
      val obj = parse[CaseClassWithNestedSeqLong](
        """
        {
          "seq_class" : {"seq": [%s]},
          "set_class" : {"set": [%s]}
        }
        """.format(idsStr, idsStr))
      obj.seqClass.seq.sorted should equal(1004 to 1500)
      obj.setClass.set.toSeq.sorted should equal(1004 to 1500)
    }

    scenario("complex without companion class") {
      val json = """{
                   "entity_ids" : [ 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025, 1026, 1027, 1028, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1038, 1039, 1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095, 1096, 1097, 1098, 1099, 1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124, 1125, 1126, 1127, 1128, 1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143, 1144, 1145, 1146, 1147, 1148, 1149, 1150, 1151, 1152, 1153, 1154, 1155, 1156, 1157, 1158, 1159, 1160, 1161, 1162, 1163, 1164, 1165, 1166, 1167, 1168, 1169, 1170, 1171, 1172, 1173, 1174, 1175, 1176, 1177, 1178, 1179, 1180, 1181, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193, 1194, 1195, 1196, 1197, 1198, 1199, 1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1216, 1217, 1218, 1219, 1220, 1221, 1222, 1223, 1224, 1225, 1226, 1227, 1228, 1229, 1230, 1231, 1232, 1233, 1234, 1235, 1236, 1237, 1238, 1239, 1240, 1241, 1242, 1243, 1244, 1245, 1246, 1247, 1248, 1249, 1250, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 1258, 1259, 1260, 1261, 1262, 1263, 1264, 1265, 1266, 1267, 1268, 1269, 1270, 1271, 1272, 1273, 1274, 1275, 1276, 1277, 1278, 1279, 1280, 1281, 1282, 1283, 1284, 1285, 1286, 1287, 1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1298, 1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1307, 1308, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 1320, 1321, 1322, 1323, 1324, 1325, 1326, 1327, 1328, 1329, 1330, 1331, 1332, 1333, 1334, 1335, 1336, 1337, 1338, 1339, 1340, 1341, 1342, 1343, 1344, 1345, 1346, 1347, 1348, 1349, 1350, 1351, 1352, 1353, 1354, 1355, 1356, 1357, 1358, 1359, 1360, 1361, 1362, 1363, 1364, 1365, 1366, 1367, 1368, 1369, 1370, 1371, 1372, 1373, 1374, 1375, 1376, 1377, 1378, 1379, 1380, 1381, 1382, 1383, 1384, 1385, 1386, 1387, 1388, 1389, 1390, 1391, 1392, 1393, 1394, 1395, 1396, 1397, 1398, 1399, 1400, 1401, 1402, 1403, 1404, 1405, 1406, 1407, 1408, 1409, 1410, 1411, 1412, 1413, 1414, 1415, 1416, 1417, 1418, 1419, 1420, 1421, 1422, 1423, 1424, 1425, 1426, 1427, 1428, 1429, 1430, 1431, 1432, 1433, 1434, 1435, 1436, 1437, 1438, 1439, 1440, 1441, 1442, 1443, 1444, 1445, 1446, 1447, 1448, 1449, 1450, 1451, 1452, 1453, 1454, 1455, 1456, 1457, 1458, 1459, 1460, 1461, 1462, 1463, 1464, 1465, 1466, 1467, 1468, 1469, 1470, 1471, 1472, 1473, 1474, 1475, 1476, 1477, 1478, 1479, 1480, 1481, 1482, 1483, 1484, 1485, 1486, 1487, 1488, 1489, 1490, 1491, 1492, 1493, 1494, 1495, 1496, 1497, 1498, 1499, 1500 ],
                   "previous_cursor" : "$",
                   "next_cursor" : "2892e7ab37d44c6a15b438f78e8d76ed$"
                 }"""
      val entityIdsResponse = parse[TestEntityIdsResponse](json)
      entityIdsResponse.entityIds.sorted.size should be > (0)
    }

    scenario("complex with companion class") {
      pending
      //TODO: Investigate this corner case
      val json = """{
                   "entity_ids" : [ 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025, 1026, 1027, 1028, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1038, 1039, 1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095, 1096, 1097, 1098, 1099, 1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124, 1125, 1126, 1127, 1128, 1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143, 1144, 1145, 1146, 1147, 1148, 1149, 1150, 1151, 1152, 1153, 1154, 1155, 1156, 1157, 1158, 1159, 1160, 1161, 1162, 1163, 1164, 1165, 1166, 1167, 1168, 1169, 1170, 1171, 1172, 1173, 1174, 1175, 1176, 1177, 1178, 1179, 1180, 1181, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193, 1194, 1195, 1196, 1197, 1198, 1199, 1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1216, 1217, 1218, 1219, 1220, 1221, 1222, 1223, 1224, 1225, 1226, 1227, 1228, 1229, 1230, 1231, 1232, 1233, 1234, 1235, 1236, 1237, 1238, 1239, 1240, 1241, 1242, 1243, 1244, 1245, 1246, 1247, 1248, 1249, 1250, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 1258, 1259, 1260, 1261, 1262, 1263, 1264, 1265, 1266, 1267, 1268, 1269, 1270, 1271, 1272, 1273, 1274, 1275, 1276, 1277, 1278, 1279, 1280, 1281, 1282, 1283, 1284, 1285, 1286, 1287, 1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1298, 1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1307, 1308, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 1320, 1321, 1322, 1323, 1324, 1325, 1326, 1327, 1328, 1329, 1330, 1331, 1332, 1333, 1334, 1335, 1336, 1337, 1338, 1339, 1340, 1341, 1342, 1343, 1344, 1345, 1346, 1347, 1348, 1349, 1350, 1351, 1352, 1353, 1354, 1355, 1356, 1357, 1358, 1359, 1360, 1361, 1362, 1363, 1364, 1365, 1366, 1367, 1368, 1369, 1370, 1371, 1372, 1373, 1374, 1375, 1376, 1377, 1378, 1379, 1380, 1381, 1382, 1383, 1384, 1385, 1386, 1387, 1388, 1389, 1390, 1391, 1392, 1393, 1394, 1395, 1396, 1397, 1398, 1399, 1400, 1401, 1402, 1403, 1404, 1405, 1406, 1407, 1408, 1409, 1410, 1411, 1412, 1413, 1414, 1415, 1416, 1417, 1418, 1419, 1420, 1421, 1422, 1423, 1424, 1425, 1426, 1427, 1428, 1429, 1430, 1431, 1432, 1433, 1434, 1435, 1436, 1437, 1438, 1439, 1440, 1441, 1442, 1443, 1444, 1445, 1446, 1447, 1448, 1449, 1450, 1451, 1452, 1453, 1454, 1455, 1456, 1457, 1458, 1459, 1460, 1461, 1462, 1463, 1464, 1465, 1466, 1467, 1468, 1469, 1470, 1471, 1472, 1473, 1474, 1475, 1476, 1477, 1478, 1479, 1480, 1481, 1482, 1483, 1484, 1485, 1486, 1487, 1488, 1489, 1490, 1491, 1492, 1493, 1494, 1495, 1496, 1497, 1498, 1499, 1500 ],
                   "previous_cursor" : "$",
                   "next_cursor" : "2892e7ab37d44c6a15b438f78e8d76ed$"
                 }"""
      val entityIdsResponse = parse[TestEntityIdsResponseWithCompanion](json)
      entityIdsResponse.entityIds.sorted.size should be > (0)
    }
  }

  feature("A case class with members of all ScalaSig types") {
    val json = """
               {
                 "map": {
                   "one": "two"
                 },
                 "set": [1, 2, 3],
                 "string": "woo",
                 "list": [4, 5, 6],
                 "seq": [7, 8, 9],
                 "sequence": [10, 11, 12],
                 "collection": [13, 14, 15],
                 "indexed_seq": [16, 17, 18],
                 "random_access_seq": [19, 20, 21],
                 "vector": [22, 23, 24],
                 "big_decimal": 12.0,
                 "big_int": 13,
                 "int": 1,
                 "long": 2,
                 "char": "x",
                 "bool": false,
                 "short": 14,
                 "byte": 15,
                 "float": 34.5,
                 "double": 44.9,
                 "any": true,
                 "any_ref": "wah"
               }"""
    /*
       "intMap": {
         "1": "1"
       },
       "longMap": {
         "2": 2
       }
     }
     """
     */
    //TODO


    scenario("is parsable from a JSON object with those fields") {
      val got = parse[CaseClassWithAllTypes](json)
      val expected = CaseClassWithAllTypes(
        map = Map("one" -> "two"),
        set = Set(1, 2, 3),
        string = "woo",
        list = List(4, 5, 6),
        seq = Seq(7, 8, 9),
        indexedSeq = IndexedSeq(16, 17, 18),
        vector = Vector(22, 23, 24),
        bigDecimal = BigDecimal("12.0"),
        bigInt = 13, //todo
        int = 1,
        long = 2L,
        char = 'x',
        bool = false,
        short = 14,
        byte = 15,
        float = 34.5f,
        double = 44.9d,
        any = true,
        anyRef = "wah"
        //intMap = Map(1 -> 1), //TODO
        //longMap = Map(2L -> 2L) //TODO
      )

      got should be(expected)
    }
  }

  feature("A case class that throws an exception") {
    scenario("is not parsable from a JSON object") {
      intercept[NullPointerException] {
        parse[CaseClassWithException]("""{}""")
      }
    }
  }

  feature("A case class nested inside of an object") {
    scenario("is parsable from a JSON object") {
      parse[OuterObject.NestedCaseClass]( """{"id": 1}""") should be(OuterObject.NestedCaseClass(1))
    }
  }

  feature("A case class nested inside of an object nested inside of an object") {
    scenario("is parsable from a JSON object") {
      parse[OuterObject.InnerObject.SuperNestedCaseClass]( """{"id": 1}""") should be(OuterObject.InnerObject.SuperNestedCaseClass(1))
    }
  }

  feature("A case class with array members") {

    scenario("is parsable from a JSON object") {
      val c = parse[CaseClassWithArrays]( """{"one":"1","two":["a","b","c"],"three":[1,2,3], "four":[4, 5], "five":["x", "y"]}""")
      c.one should be("1")
      c.two should be(Array("a", "b", "c"))
      c.three should be(Array(1, 2, 3))
      c.four should be(Array(4L, 5L))
      c.five should be(Array('x', 'y'))
    }

    scenario("generates a JSON object") {
      generate(CaseClassWithArrays("1", Array("a", "b", "c"), Array(1, 2, 3), Array(4L, 5L), Array('x', 'y'))) should be(
        """{"one":"1","two":["a","b","c"],"three":[1,2,3],"four":[4,5],"five":"xy"}"""
      )
    }
  }

  feature("A case class with collection of Longs") {
    scenario("array of longs") {
      val c = parse[CaseClassWithArrayLong]( """{"array":[3,1,2]}""")
      c.array.sorted should equal(Array(1, 2, 3))
    }
    scenario("seq of longs") {
      val c = parse[CaseClassWithSeqLong]( """{"seq":[3,1,2]}""")
      c.seq.sorted should equal(Seq(1, 2, 3))
    }
  }

  feature("seq of longs") {
    scenario("seq of longs") {
      val seq = parse[Seq[Long]]( """[3,1,2]""")
      seq.sorted should equal(Seq(1L, 2L, 3L))
    }
  }

  feature("old") {
    scenario("parse seq of longs") {
      val ids = parse[Seq[Long]]("[3,1,2]")
      ids.sorted should equal(Seq(1L, 2L, 3L))
    }

    scenario("handle options and defaults in case class") {
      val bob = parse[Person](
        """
        {
          "id" :1,
          "name" : "Bob",
          "age" : 21
        }
        """)
      bob should equal(Person(1, "Bob", Some(21), None, "unknown"))
    }

    scenario("missing required field") {
      intercept[JsonObjectParseException] {
        parse[Person](
          """
          {
          }
          """)
      }
    }

    scenario("nulls will not render") {
      generate(
        Person(1, null, null, null)) should equal( """{"id":1,"nickname":"unknown"}""")
    }

    scenario("string wrapper deserialization") {
      parse[ObjWithTestId](
        """
      {
        "id": "5"
      }
        """) should equal(ObjWithTestId(TestIdStringWrapper("5")))
    }

    scenario("parse input stream") {
      val is = new ByteArrayInputStream( """{"foo": "bar"}""".getBytes)
      mapper.parse[Blah](is) should equal(Blah("bar"))
    }

    scenario("Logging Trait fields should be ignored") {
      generate(Group3("123")) should be( """{"id":"123"}""")
    }
  }

  def assertJsonParse[T: Manifest](json: String, withErrors: Seq[String]) = {
    if (withErrors.nonEmpty) {
      val e = intercept[JsonObjectParseException] {
        parse[T](json)
      }
      trace(e.fieldErrors.mkString("\n"))
      clearStackTrace(e.fieldErrors)

      val actualMessages = e.fieldErrors map {_.getMessage}
      JsonDiff.jsonDiff(actualMessages, withErrors)
      null
    }
    else {
      parse[T](json)
    }
  }

  def clearStackTrace(exceptions: Seq[JsonFieldParseException]) = {
    exceptions map {_.setStackTrace(Array())}
    exceptions
  }

  def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }

  def generate(any: Any): String = {
    mapper.writeValueAsString(any)
  }

  def assertJson[T: Manifest](obj: T, expected: String) {
    val json = generate(obj)
    JsonDiff.jsonDiff(json, expected)
    println("JSON: " + json.toPrettyJson)
    parse[T](json) should equal(obj)
  }
}

case class WrappedValueInt(value: Int)
  extends JsonWrappedValue[Int]

case class WrappedValueLong(value: Long)
  extends JsonWrappedValue[Long]

case class WrappedValueString(value: String)
  extends JsonWrappedValue[String]

case class WrappedValueIntInObj(
  foo: WrappedValueInt)

case class WrappedValueStringInObj(
  foo: WrappedValueString)

case class WrappedValueLongInObj(
  foo: WrappedValueLong)

case class CaseClassWithVal(
  name: String) {

  val `type`: String = "person"
}

case class CaseClassWithEnum(
  name: String,
  make: CarMakeEnum)

case class CaseClassWithComplexEnums(
  name: String,
  make: CarMakeEnum,
  makeOpt: Option[CarMakeEnum],
  makeSeq: Seq[CarMakeEnum],
  makeSet: Set[CarMakeEnum])

case class CaseClassWithSeqEnum(
  enumSeq: Seq[CarMakeEnum])

case class CaseClassWithOptionEnum(
  enumOpt: Option[CarMakeEnum])

case class CaseClassWithDateTime(
  dateTime: DateTime)

case class CaseClassWithIntAndDateTime(
  name: String,
  age: Int,
  age2: Int,
  age3: Int,
  dateTime: DateTime,
  dateTime2: DateTime,
  dateTime3: DateTime,
  dateTime4: DateTime)

case class ClassWithFooClassInject(
  @JsonInject fooClass: FooClass)

case class ClassWithQueryParamDateTimeInject(
  @QueryParam dateTime: DateTime)

case class CaseClassWithEscapedLong(
  `1-5`: Long)

case class CaseClassWithEscapedString(
  `1-5`: String)

case class CaseClassWithEscapedNormalString(
  `a`: String)

case class UnicodeNameCaseClass(`winning-id`: Int, name: String)

case class TestEntityIdsResponse(
  entityIds: Seq[Long],
  previousCursor: String,
  nextCursor: String)

object TestEntityIdsResponseWithCompanion {
  val msg = "im the companion"
}

case class TestEntityIdsResponseWithCompanion(
  entityIds: Seq[Long],
  previousCursor: String,
  nextCursor: String)

case class WrappedValueStringMapObject(
  map: Map[WrappedValueString, String])

case class FooClass(id: String)

case class Group3(id: String)
  extends Logging
