package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;

/** Reuses infrastructure-free regression cases without starting Spring Boot. */
@Tag("ISOLATION")
class ExistingBehaviorIsolationTest {
    @Nested class Mapping extends FhirMappingPublicationTest { }
    @Nested class MappingClient extends FhirMappingClientTest { }
    @Nested class ValidationOutcome extends ValidationOutcomeTest { }
    @Nested class StrategyResolution extends AffinityDomainStrategyResolverTest { }
    @Nested class MetadataIndexing extends PresentMetadataIndexerTest { }
    @Nested class AffinityValidation extends AffinityDomainValidationSRVTest { }
    @Nested class Issuer extends IssuerSRVTest { }
    @Nested class Exceptions extends ExceptionTest { }
}
