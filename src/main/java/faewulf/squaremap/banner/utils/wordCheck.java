package faewulf.squaremap.banner.utils;

import java.util.regex.Pattern;

public class wordCheck {
    static public boolean containsWholeWord(String input, String word) {
        // Unicode aware on purpose: \b only knows ASCII letters, so words with diacritics
        String patternString = "(?<![\\p{L}\\p{N}_])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}_])";
        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return pattern.matcher(input).find();
    }
}