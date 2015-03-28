// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.routines;

import org.openstreetmap.josm.tools.I18n;

/**
 * Abstract validator superclass to extend Apache Validator routines.
 * @since 7489
 */
public abstract class AbstractValidator {

    private String errorMessage;
    private String fix;

    /**
     * Tests validity of a given value.
     * @param value Value to test
     * @return {@code true} if value is valid, {@code false} otherwise
     */
    public abstract boolean isValid(String value);

    /**
     * Replies the error message.
     * @return the errorMessage
     */
    public final String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the localized error message.
     * @param errorMessage the errorMessage
     * @param objects the additional argument(s) to pass to {@link I18n#tr(String, Object...)}
     */
    protected final void setErrorMessage(String errorMessage, Object... objects) {
        this.errorMessage = I18n.tr(errorMessage, objects);
    }

    /**
     * Replies the fixed value, if any.
     * @return the fixed value or {@code null}
     */
    public final String getFix() {
        return fix;
    }

    /**
     * Sets the fixed value.
     * @param fix the fixed value, if any
     */
    protected final void setFix(String fix) {
        this.fix = fix;
    }
}
