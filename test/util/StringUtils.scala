package util

import scala.Predef.String

object StringUtils {
  /**
   * Gets first link from content. To be used within tests to parse emails
   * and then follow links in them.
   * @param content
   * @param linkPart
   * @return
   */
  def getFirstLinkByContent(content: String, linkPart: String): Option[String] = {
    val startIndex = content.indexOf(linkPart)
    if (startIndex == -1) return None
    // go down and find nearest space or quote
    val from = Math.max(
      if (content.substring(0, startIndex).lastIndexOf('"') != -1) content.substring(0, startIndex).lastIndexOf('"') else 0,
      if (content.substring(0, startIndex).lastIndexOf(' ') != -1) content.substring(0, startIndex).lastIndexOf(' ') else 0)
    // go up and find nearest space or quote
    val to = Math.min(
      if (content.substring(startIndex).indexOf('"') != -1) startIndex + content.substring(startIndex).indexOf('"') else content.length,
      if (content.substring(startIndex).indexOf(' ') != -1) startIndex + content.substring(startIndex).indexOf(' ') else content.length)
    Some(content.substring(from + 1, to).trim)
  }

  def getRequestPathFromString(link: String): String = {
    if (link.indexOf('/') == -1) return link
    if (link.contains("""//""")) {
      val withoutHttpSlashSlash = link.substring(link.indexOf("""//""") + 2)
      withoutHttpSlashSlash.substring(withoutHttpSlashSlash.indexOf('/'))
    } else {
      link.substring(link.indexOf("/"))
    }
  }
}
