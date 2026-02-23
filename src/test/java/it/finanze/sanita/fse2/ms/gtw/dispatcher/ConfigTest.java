package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.PROPS_NAME_AUDIT_ENABLED;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.PROPS_NAME_CONTROL_LOG_ENABLED;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.PROPS_NAME_ISSUER_CF;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.PROPS_NAME_REMOVE_EDS_ENABLE;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.PROPS_NAME_SUBJECT;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Profile.TEST;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.adapter.CustomResponseBodyAdviceAdapter;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.WebCFG;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(TEST)
@TestInstance(PER_CLASS)
class ConfigTest extends AbstractConfig {

    // Exclude from running by mocking
	@MockitoBean
    private WebCFG web;
    @MockitoBean
    private CustomResponseBodyAdviceAdapter adapter;

    private static final List<Pair<String, String>> DEFAULT_PROPS = Arrays.asList(
        Pair.of(PROPS_NAME_REMOVE_EDS_ENABLE, "false"),
        Pair.of(PROPS_NAME_AUDIT_ENABLED, "false"),
        Pair.of(PROPS_NAME_CONTROL_LOG_ENABLED, "false"),
        Pair.of(PROPS_NAME_SUBJECT, "false"),
        Pair.of(PROPS_NAME_ISSUER_CF, "false")
    );

    @Test
    void testCacheProps() {
        testCacheProps(DEFAULT_PROPS.get(0), () -> assertFalse(config.isRemoveEds()));
        testCacheProps(DEFAULT_PROPS.get(1), () -> assertFalse(config.isAuditEnable()));
        testCacheProps(DEFAULT_PROPS.get(2), () -> assertFalse(config.isControlLogPersistenceEnable()));
        testCacheProps(DEFAULT_PROPS.get(3), () -> assertFalse(config.isSubjectNotAllowed()));
        testCacheProps(DEFAULT_PROPS.get(4), () -> assertFalse(config.isCfOnIssuerNotAllowed()));
    }

    @Test
    void testRefreshProps() {
        testRefreshProps(DEFAULT_PROPS.get(0), "true", () -> assertTrue(config.isRemoveEds()));
        testRefreshProps(DEFAULT_PROPS.get(1), "true", () -> assertTrue(config.isAuditEnable()));
        testRefreshProps(DEFAULT_PROPS.get(2), "true", () -> assertTrue(config.isControlLogPersistenceEnable()));
        testRefreshProps(DEFAULT_PROPS.get(3), "true", () -> assertTrue(config.isSubjectNotAllowed()));
        testRefreshProps(DEFAULT_PROPS.get(4), "true", () -> assertTrue(config.isCfOnIssuerNotAllowed()));
    }

    @Test
    void testIntegrityProps() {
        testIntegrityCheck();
    }

    @Override
    public List<Pair<String, String>> defaults() {
        return DEFAULT_PROPS;
    }
}
