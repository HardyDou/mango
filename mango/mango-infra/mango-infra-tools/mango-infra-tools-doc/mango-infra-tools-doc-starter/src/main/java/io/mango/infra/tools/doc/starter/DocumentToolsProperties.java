package io.mango.infra.tools.doc.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mango.tools.doc")
public class DocumentToolsProperties {

    /**
     * Whether document tools auto-configuration is enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to register the lightweight HTML to text converter.
     */
    private boolean htmlToTextEnabled = true;

    /**
     * Whether to register the default PDF operation service.
     */
    private boolean pdfOperationsEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHtmlToTextEnabled() {
        return htmlToTextEnabled;
    }

    public void setHtmlToTextEnabled(boolean htmlToTextEnabled) {
        this.htmlToTextEnabled = htmlToTextEnabled;
    }

    public boolean isPdfOperationsEnabled() {
        return pdfOperationsEnabled;
    }

    public void setPdfOperationsEnabled(boolean pdfOperationsEnabled) {
        this.pdfOperationsEnabled = pdfOperationsEnabled;
    }
}
