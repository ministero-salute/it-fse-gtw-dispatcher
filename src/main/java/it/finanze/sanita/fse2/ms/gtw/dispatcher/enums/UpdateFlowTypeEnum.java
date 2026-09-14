package it.finanze.sanita.fse2.ms.gtw.dispatcher.enums;

public enum UpdateFlowTypeEnum {
	UPDATE_ITI57(true, true),
	UPDATE_ITI42(true, true),
	UPDATE_OSCURAMENTO(false, false);

	private final boolean validateAffinityDomain;
	private final boolean useAsyncRetry;

	UpdateFlowTypeEnum(boolean validateAffinityDomain, boolean useAsyncRetry) {
		this.validateAffinityDomain = validateAffinityDomain;
		this.useAsyncRetry = useAsyncRetry;
	}

	public boolean shouldValidateAffinityDomain() {
		return validateAffinityDomain;
	}

	public boolean shouldUseAsyncRetry() {
		return useAsyncRetry;
	}
}