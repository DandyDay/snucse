// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
import pp202402.assign0.*

class TestSuite extends munit.FunSuite {
  test("is valid OS string") {
    val windows = "Windows"

    assert(Main.isValidOS(windows))
    assert(Main.isValidOS("None"))
    assert(Main.isValidOS("Android") == false)
  }

  test("split dash from ID") {
    val id = "2024-10000"
    val (year, num) = Main.splitDashFromID(id)

    assertEquals(year, "2024")
    assertEquals(num, "10000")
    assert(true)
  }
}

