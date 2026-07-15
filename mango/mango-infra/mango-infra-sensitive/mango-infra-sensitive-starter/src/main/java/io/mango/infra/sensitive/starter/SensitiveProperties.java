package io.mango.infra.sensitive.starter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mango sensitive masking configuration.
 */
@ConfigurationProperties(SensitiveProperties.PREFIX)
public class SensitiveProperties {

    public static final String PREFIX = "mango.sensitive";

    private static final int DEFAULT_NUM_CHECK_LENGTH = 8;

    private final Masking masking = new Masking();

    private final Word word = new Word();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Masking getMasking() {
        return masking;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration binding intentionally exposes this nested property bean")
    public Word getWord() {
        return word;
    }

    /**
     * Field masking settings.
     */
    public static class Masking {

        /**
         * Authority that allows the current caller to view raw values.
         */
        private String rawAuthority = "no_mask";

        public String getRawAuthority() {
            return rawAuthority;
        }

        public void setRawAuthority(String rawAuthority) {
            this.rawAuthority = rawAuthority;
        }
    }

    /**
     * Sensitive word engine settings.
     */
    public static class Word {

        private boolean enabled = true;

        private boolean ignoreCase = true;

        private boolean ignoreWidth = true;

        private boolean ignoreNumStyle = true;

        private boolean ignoreChineseStyle = true;

        private boolean ignoreEnglishStyle = true;

        private boolean ignoreRepeat = true;

        private boolean enableNumCheck;

        private boolean enableEmailCheck;

        private boolean enableUrlCheck = true;

        private int numCheckLen = DEFAULT_NUM_CHECK_LENGTH;

        private String errorMsg = "您的输入包含敏感词，请重新输入";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        public void setIgnoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
        }

        public boolean isIgnoreWidth() {
            return ignoreWidth;
        }

        public void setIgnoreWidth(boolean ignoreWidth) {
            this.ignoreWidth = ignoreWidth;
        }

        public boolean isIgnoreNumStyle() {
            return ignoreNumStyle;
        }

        public void setIgnoreNumStyle(boolean ignoreNumStyle) {
            this.ignoreNumStyle = ignoreNumStyle;
        }

        public boolean isIgnoreChineseStyle() {
            return ignoreChineseStyle;
        }

        public void setIgnoreChineseStyle(boolean ignoreChineseStyle) {
            this.ignoreChineseStyle = ignoreChineseStyle;
        }

        public boolean isIgnoreEnglishStyle() {
            return ignoreEnglishStyle;
        }

        public void setIgnoreEnglishStyle(boolean ignoreEnglishStyle) {
            this.ignoreEnglishStyle = ignoreEnglishStyle;
        }

        public boolean isIgnoreRepeat() {
            return ignoreRepeat;
        }

        public void setIgnoreRepeat(boolean ignoreRepeat) {
            this.ignoreRepeat = ignoreRepeat;
        }

        public boolean isEnableNumCheck() {
            return enableNumCheck;
        }

        public void setEnableNumCheck(boolean enableNumCheck) {
            this.enableNumCheck = enableNumCheck;
        }

        public boolean isEnableEmailCheck() {
            return enableEmailCheck;
        }

        public void setEnableEmailCheck(boolean enableEmailCheck) {
            this.enableEmailCheck = enableEmailCheck;
        }

        public boolean isEnableUrlCheck() {
            return enableUrlCheck;
        }

        public void setEnableUrlCheck(boolean enableUrlCheck) {
            this.enableUrlCheck = enableUrlCheck;
        }

        public int getNumCheckLen() {
            return numCheckLen;
        }

        public void setNumCheckLen(int numCheckLen) {
            this.numCheckLen = numCheckLen;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

    }
}
