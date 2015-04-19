package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class SectionPropertiesFactory {

    private static final Logger LOG = Logger.getLogger(SectionProperties.class);
    private static final Map<String, SectionProperties> PROPER_MAP = Collections.synchronizedMap(new HashMap<String, SectionProperties>());

    private static class Holder {

        private static final SectionProperties instance;

        static {
            instance = new SectionProperties("conf/conf.properties");
        }
    }

    public static SectionProperties getProperties() {
        return Holder.instance;
    }

    public static synchronized SectionProperties getProperties(String fileUrl) {
        if (fileUrl == null) {
            LOG.info("invalid properties file url");
            return null;
        }

        if (PROPER_MAP.containsKey(fileUrl)) {
            return PROPER_MAP.get(fileUrl);
        } else {
            SectionProperties prop = new SectionProperties(fileUrl);
            PROPER_MAP.put(fileUrl, prop);
            return prop;
        }
    }

    public static void main(String[] args) {
        SectionProperties config = SectionPropertiesFactory.getProperties();
        // test refer
        System.out.println(config.getValue("CONF", "kxd-portal"));
        // test not refer
        System.out.println(config.getValue("CONF", "kxdm"));
    }
}
