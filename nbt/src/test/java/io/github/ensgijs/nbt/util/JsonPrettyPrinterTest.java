package io.github.ensgijs.nbt.util;

import junit.framework.TestCase;
import static io.github.ensgijs.nbt.util.JsonPrettyPrinter.prettyPrintJson;

public class JsonPrettyPrinterTest extends TestCase {

    public void testFormatObject_strict() {
        String json = "{\"hello\":\"world\",\"stuff\":[42.5e+2,\"things\"]}";
        String expected = """
                {
                  "hello": "world",
                  "stuff": [
                    42.5e+2,
                    "things"
                  ]
                }""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }

    public void testFormatObject_strict_allow_oneliners() {
        String json = "{\"hello\":\"world\",\"stuff\":[42.5e+2,\"things\"]}";
        String expected = """
                {
                  "hello": "world",
                  "stuff": [42.5e+2, "things"]
                }""";
        assertEquals(expected, prettyPrintJson(json));
    }

    public void testFormatObject_lose() {
        String json = "{hello:\"world\",stuff:[-42.5e-2,\"thang'z\", 'mo\"oo']}";
        String expected = """
                {
                  hello: "world",
                  stuff: [
                    -42.5e-2,
                    "thang'z",
                    'mo"oo'
                  ]
                }""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }

    public void testFormatArray() {
        String json = "[0,\"one\",\"o\":{},\"a\":[]]";
        String expected = """
                [
                  0,
                  "one",
                  "o": {},
                  "a": []
                ]""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }

    public void testFormatLongSNBTArray() {
        String json = "[L;0, 42, 99]";
        String expected = """
                [L;
                  0,
                  42,
                  99
                ]""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }

    public void testFormatLongSNBTArray_allow_oneliners() {
        String json = "[L;0, 42, 99]";
        String expected = """
                [L; 0, 42, 99]""";
        assertEquals(expected, prettyPrintJson(json));
    }

    public void testFormatEmptyLongSNBTArray() {
        String json = "[L;]";
        String expected = "[L;]";
        assertEquals(expected, prettyPrintJson(json, 0));
    }
    public void testFormatEmptyLongSNBTArray_allow_oneliners() {
        String json = "[L;]";
        String expected = "[L;]";
        assertEquals(expected, prettyPrintJson(json));
    }


    public void testFormatIntSNBTArray() {
        String json = "[I;0, 42, 99]";
        String expected = """
                [I;
                  0,
                  42,
                  99
                ]""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }
    public void testFormatEmptyIntSNBTArray() {
        String json = "[I;]";
        String expected = "[I;]";
        assertEquals(expected, prettyPrintJson(json, 0));
    }

    public void testFormatByteSNBTArray() {
        String json = "[B;0, 42, 99]";
        String expected = """
                [B;
                  0,
                  42,
                  99
                ]""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }
    public void testFormatEmptyByteSNBTArray() {
        String json = "[B;]";
        String expected = "[B;]";
        assertEquals(expected, prettyPrintJson(json, 0));
    }

    public void testFormatJustAString() {
        String json = "\"hello world\"";
        assertEquals(json, prettyPrintJson(json, 0));
        json = "'hello world'";
        assertEquals(json, prettyPrintJson(json, 0));
    }

    public void testFormatStringContainingNewline() {
        String json = "\"hello\nworld\"";
        assertEquals("\"hello\\nworld\"", prettyPrintJson(json, 0));
    }

    public void testFormatJustANumber() {
        String json = "42";
        assertEquals(json, prettyPrintJson(json, 0));
    }


    public void testBasicNestedCompoundsAndLists() {
        String json = """
                "root": {
                  "longArray": [L; 1125899906842624, 2],
                  "intArray": [I; 1, 33554432, 3],
                  "nestedCompound": {
                    "name": "value12",
                    "deeper": [{x:42,a:[{"not":"home","done":10}]}],
                    "blame": "me"
                  },
                  "byteArray": [B; 4, -3, 2],
                  "nestedList": ["world", "hello"]
                }""";
        String expected = """
                "root": {
                  "longArray": [L;
                    1125899906842624,
                    2
                  ],
                  "intArray": [I;
                    1,
                    33554432,
                    3
                  ],
                  "nestedCompound": {
                    "name": "value12",
                    "deeper": [
                      {
                        x: 42,
                        a: [
                          {
                            "not": "home",
                            "done": 10
                          }
                        ]
                      }
                    ],
                    "blame": "me"
                  },
                  "byteArray": [B;
                    4,
                    -3,
                    2
                  ],
                  "nestedList": [
                    "world",
                    "hello"
                  ]
                }""";
        assertEquals(expected, prettyPrintJson(json, 0));
    }
    public void testBasicNestedCompoundsAndLists_allow_oneliners() {
        String json = """
                "root": {
                  "longArray": [L;
                    1125899906842624,
                    2
                  ],
                  "intArray": [I; 1, 33554432, 3],
                  "nestedCompound": {
                    "name": "value12",
                    "deeper": [{x:42,a:[{"not":"home","done":10}]}],
                    "blame": "me"
                  },
                  "byteArray": [B;4,-3,2],"nestedList":[     "world"   ,   "hello"   ]
                }""";
        String expected = """
                "root": {
                  "longArray": [L; 1125899906842624, 2],
                  "intArray": [I; 1, 33554432, 3],
                  "nestedCompound": {
                    "name": "value12",
                    "deeper": [
                      {
                        x: 42,
                        a: [
                          {"not": "home", "done": 10}
                        ]
                      }
                    ],
                    "blame": "me"
                  },
                  "byteArray": [B; 4, -3, 2],
                  "nestedList": ["world", "hello"]
                }""";
        assertEquals(expected, prettyPrintJson(json));
    }

    public void testOneLinerWrappingBoundaryCases() {
        String json = """
                {
                  "o": {},
                  "l": [],
                  "a": [L; 1, 2, 3],
                  "b": [L; 4, 5]
                  ,"c": [L; 6, 7]
                }""";

        // happy case
        assertEquals("""
                {
                  "o": {},
                  "l": [],
                  "a": [L; 1, 2, 3],
                  "b": [L; 4, 5],
                  "c": [L; 6, 7]
                }""", prettyPrintJson(json, 99));

        // no wrap
        assertEquals("""
                {
                  "o": {},
                  "l": [],
                  "a": [L;
                    1,
                    2,
                    3
                  ],
                  "b": [L;
                    4,
                    5
                  ],
                  "c": [L;
                    6,
                    7
                  ]
                }""", prettyPrintJson(json, 0));

        // wrap of "b" forced due to comma while "c" is OK to one-line
        assertEquals("""
                {
                  "o": {},
                  "l": [],
                  "a": [L;
                    1,
                    2,
                    3
                  ],
                  "b": [L;
                    4,
                    5
                  ],
                  "c": [L; 6, 7]
                }""", prettyPrintJson(json, 16));
    }

    public void testOneLinerWrappingIsntConfusedByStringContents() {
        String json = """
                {
                  "a": {"He}lo"},
                  "b": {"He{lo"},
                  "c": {"He]lo"},
                  "d": {"He[lo"},
                  "a2": {'He}lo'},
                  "b2": {'He{lo'},
                  "c2": {'He]lo'},
                  "d2": {'He[lo'}
                }""";

        // happy case
        assertEquals("""
                {
                  "a": {"He}lo"},
                  "b": {"He{lo"},
                  "c": {"He]lo"},
                  "d": {"He[lo"},
                  "a2": {'He}lo'},
                  "b2": {'He{lo'},
                  "c2": {'He]lo'},
                  "d2": {'He[lo'}
                }""", prettyPrintJson(json, 99));

        // no wrap
        assertEquals("""
                {
                  "a": {
                    "He}lo"
                  },
                  "b": {
                    "He{lo"
                  },
                  "c": {
                    "He]lo"
                  },
                  "d": {
                    "He[lo"
                  },
                  "a2": {
                    'He}lo'
                  },
                  "b2": {
                    'He{lo'
                  },
                  "c2": {
                    'He]lo'
                  },
                  "d2": {
                    'He[lo'
                  }
                }""", prettyPrintJson(json, 0));

    }

    public void testHangingCommas() {
        String json = "{\"a\":1,\"b\":2,}";
        String expected = """
                {
                  "a": 1,
                  "b": 2,
                }""";
        assertEquals(expected, prettyPrintJson(json, 0));

        json = "{\"a\":1,\"b\":2,}";
        expected = """
                {"a": 1, "b": 2,}""";
        assertEquals(expected, prettyPrintJson(json));
    }
}
