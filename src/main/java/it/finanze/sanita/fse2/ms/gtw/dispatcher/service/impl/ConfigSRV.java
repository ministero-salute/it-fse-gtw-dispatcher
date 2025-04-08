package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IConfigClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ConfigItemDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum.DISPATCHER;

@Slf4j
@Service
public class ConfigSRV implements IConfigSRV {

    @Autowired
    private IConfigClient client;
    
    @Autowired
    private ProfileUtility profiles;

    @Value("${ms.config.refresh-rate:900000}")
	private Long refreshRate;

	private final Map<String, Pair<Long, String>> props;

	public ConfigSRV() {
		this.props = new HashMap<>();
	}

    
    @PostConstruct
    public void postConstruct() {
        if(!profiles.isTestProfile()) {
            init();
        } else {
            log.info("Skipping gtw-config initialization due to test profile");
        }
    }
    
    
	@Override
	public Boolean isRemoveEds() {
		long lastUpdate = props.get(PROPS_NAME_REMOVE_EDS_ENABLE).getKey();
		if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
			synchronized(Locks.REMOVE_EDS_ENABLE) {
				if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
					refresh(PROPS_NAME_REMOVE_EDS_ENABLE);
				}
			}
		}
		return Boolean.parseBoolean(props.get(PROPS_NAME_REMOVE_EDS_ENABLE).getValue());
	}

    @Override
    public Boolean isAuditEnable() {
        long lastUpdate = props.get(PROPS_NAME_AUDIT_ENABLED).getKey();
        if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
            synchronized(Locks.AUDIT_ENABLED) {
                if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
                    refresh(PROPS_NAME_AUDIT_ENABLED);
                }
            }
        }
        return Boolean.parseBoolean(
            props.get(PROPS_NAME_AUDIT_ENABLED).getValue()
        );
    }

    @Override
    public Boolean isControlLogPersistenceEnable() {
        long lastUpdate = props.get(PROPS_NAME_CONTROL_LOG_ENABLED).getKey();
        if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
            synchronized(Locks.CONTROL_LOG_ENABLED) {
                if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
                    refresh(PROPS_NAME_CONTROL_LOG_ENABLED);
                }
            }
        }
        return Boolean.parseBoolean(
            props.get(PROPS_NAME_CONTROL_LOG_ENABLED).getValue()
        );
    }
  
    @Override
	public Boolean isSubjectNotAllowed() {
		long lastUpdate = props.get(PROPS_NAME_SUBJECT).getKey();
		if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
			synchronized (Locks.SUBJECT_CLEANING) {
				if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
					refresh(PROPS_NAME_SUBJECT);
				}
			}
		}
		return Boolean.parseBoolean(
			props.get(PROPS_NAME_SUBJECT).getValue()
		);
	}
    
    @Override
	public Boolean isCfOnIssuerNotAllowed() {
		long lastUpdate = props.get(PROPS_NAME_ISSUER_CF).getKey();
		if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
			synchronized(Locks.ISSUER_CF_CLEANING) {
				if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
					refresh(PROPS_NAME_ISSUER_CF);
				}
			}
		}
		return Boolean.parseBoolean(
			props.get(PROPS_NAME_ISSUER_CF).getValue()
		);
	}
    
    @Override
	public Boolean isAuditIniEnable() {
		long lastUpdate = props.get(PROPS_NAME_AUDIT_INI_ENABLED).getKey();
		if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
			synchronized(Locks.AUDIT_INI_ENABLED) {
				if (new Date().getTime() - lastUpdate >= getRefreshRate()) {
					refresh(PROPS_NAME_AUDIT_INI_ENABLED);
				}
			}
		}
		return Boolean.parseBoolean(props.get(PROPS_NAME_AUDIT_INI_ENABLED).getValue());
	}

    private void refresh(String name) {
        String previous = props.getOrDefault(name, Pair.of(0L, null)).getValue();
        String prop = client.getProps(name, previous, DISPATCHER);
        props.put(name, Pair.of(new Date().getTime(), prop));
    }

    private void integrity() {
        String err = "Missing props {} from dispatcher";
        String[] out = new String[]{
            PROPS_NAME_AUDIT_ENABLED,
            PROPS_NAME_CONTROL_LOG_ENABLED,
            PROPS_NAME_SUBJECT,
            PROPS_NAME_ISSUER_CF,
            PROPS_NAME_REMOVE_EDS_ENABLE
        };
        for (String prop : out) {
            if(!props.containsKey(prop)) throw new IllegalStateException(err.replace("{}", prop));
        }
    }

    private void init() {
        for(ConfigItemTypeEnum en : ConfigItemTypeEnum.priority()) {
            log.info("[GTW-CFG] Retrieving {} properties ...", en.name());
            ConfigItemDTO items = client.getConfigurationItems(en);
            List<ConfigItemDTO.ConfigDataItemDTO> opts = items.getConfigurationItems();
            for(ConfigItemDTO.ConfigDataItemDTO opt : opts) {
                opt.getItems().forEach((key, value) -> {
                    log.info("[GTW-CFG] Property {} is set as {}", key, value);
                    props.put(key, Pair.of(new Date().getTime(), value));
                });
            }
            if(opts.isEmpty()) log.info("[GTW-CFG] No props were found");
        }
        integrity();
    }
    
    @Override
    public long getRefreshRate() {
        return this.refreshRate;
    }

    private static final class Locks {
        public static final Object REMOVE_EDS_ENABLE = new Object();
        public static final Object CONTROL_LOG_ENABLED = new Object();
        public static final Object AUDIT_ENABLED = new Object();
        public static final Object ISSUER_CF_CLEANING = new Object();
        public static final Object SUBJECT_CLEANING = new Object();
        public static final Object AUDIT_INI_ENABLED = new Object();
    }

}
