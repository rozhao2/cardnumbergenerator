package util;

/**
 *
 * @author rong
 * 
 * load properties file with section
 * 
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SectionProperties {

    protected Map<String, Properties> sections = new HashMap<String, Properties>();

    private transient String          currentSecion;

    private transient Properties      current;

    private Pattern                   pattern  = Pattern.compile("^\\$(.*)-(.*)$");

    public SectionProperties(String filename) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            parse(reader);
        } catch (IOException ex) {
            Logger.getLogger(SectionProperties.class.getName()).log(Level.SEVERE,
                    "cannot find configuration file: [" + filename + "]", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(SectionProperties.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    protected void parse(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            parseLine(line);
        }
    }

    protected void parseLine(String line) {
        line = line.trim();
        if (line.matches("^#")) {
            return;
        }

        if (line.matches("\\[.*\\]")) {
            currentSecion = line.replaceFirst("\\[(.*)\\]", "$1");
            current = new Properties();
            sections.put(currentSecion, current);
        } else if (line.matches(".+=.+")) {
            if (current != null) {
                int i = line.indexOf('=');
                String name = line.substring(0, i).trim();
                String value = line.substring(i + 1).trim();
                current.setProperty(name, value);
            }
        }
    }

    public String getValue(String section, String name) {
        Properties p = (Properties) sections.get(section);
        if (p == null) {
            return null;
        }
        String value = p.getProperty(name);

        if (value != null) {
            value = value.trim();
            Matcher matcher = pattern.matcher(value);

            if (matcher.find()) {
                String newSection = matcher.group(1);
                String newName = matcher.group(2);
                return getValue(newSection, newName);
            }
        }

        return value;
    }

    public Set<String> getSectionNames() {
        return this.sections.keySet();
    }
}
