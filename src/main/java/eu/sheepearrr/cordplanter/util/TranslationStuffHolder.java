package eu.sheepearrr.cordplanter.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

public class TranslationStuffHolder {
    public final Map<String, MessageFormat> translations;
    public final Locale locale;
    public TranslationStuffHolder(Map<String, MessageFormat> translations, Locale locale) {
        this.translations = translations;
        this.locale = locale;
    }
}
