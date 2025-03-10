package pp202402.assign0

object Main:
  val studentID: String = "2020-12852"
  val studentName: String = "최진호"

  val laptopOS: String = "MacOS"
  val laptopCPU: String = "Apple M2"

  def isValidOS(os: String): Boolean =
    if os == "Windows" || os == "MacOS" || os == "Linux" || os == "None" then
      true
    else false

  def splitDashFromID(id: String): (String, String) = {
    val result = id.split("-");
    (result(0), result(1))
  }
