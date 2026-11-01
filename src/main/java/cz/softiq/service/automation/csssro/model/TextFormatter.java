package cz.softiq.service.automation.csssro.model;

import cz.softiq.common.base.model.IBaseModel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFormatter implements IBaseModel {
  /**
   * Method takes any text and returns only digits
   *
   * @param text
   * @return only numbers
   */
  public String extractNumbers(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }

    Pattern pattern = Pattern.compile("\\d+(?:\\s\\d+)*");
    Matcher matcher = pattern.matcher(text);

    if (matcher.find()) {
      return matcher.group().replaceAll("\\s+", "");
    }

    return "";
  }

  /**
   * This method repalces line breaks with html code
   *
   * @param text with \n
   * @return text with <br>
   */
  public String replaceLineBreaks(String text) {
    // Use regular expression to replace all line breaks with an empty string
    return text.replaceAll("\\n", "<br>");
  }

  public String extractLetters(String text) {
    // Use a regular expression to match everything except digits
    Pattern pattern = Pattern.compile("\\D+");
    Matcher matcher = pattern.matcher(text);
    // Concatenate all matched non-digits
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      result.append(matcher.group());
    }
    return result.toString();
  }
}
